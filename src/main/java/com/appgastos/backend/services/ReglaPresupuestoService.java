package com.appgastos.backend.services;

import com.appgastos.backend.dto.DistribucionRegla;
import com.appgastos.backend.dto.PartidaDistribucion;
import com.appgastos.backend.dto.PorcentajeGrupo;
import com.appgastos.backend.dto.PresupuestoInput;
import com.appgastos.backend.dto.PresupuestoSugerido;
import com.appgastos.backend.dto.ReglaPresupuesto;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.GrupoGasto;
import com.appgastos.backend.models.PlantillaRegla;
import com.appgastos.backend.models.PlantillaRegla.Partida;
import com.appgastos.backend.models.Presupuesto;
import com.appgastos.backend.models.ReglaHogar;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.ReglaHogarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guía de distribución del ingreso del hogar (50/30/20, 70/20/10, 6 jarras).
 *
 * Es solo una guía: calcula cuánto recomienda la regla y cuánto se usó, y
 * propone presupuestos, pero nunca los guarda sin que el usuario los confirme.
 */
@Service
@RequiredArgsConstructor
public class ReglaPresupuestoService {

    /**
     * Salario mínimo legal vigente desde el 1 de julio de 2026 (Decreto N.° 6225).
     * Solo se usa para sugerir una plantilla; hay que actualizarlo en cada reajuste.
     */
    static final double SALARIO_MINIMO = 3_044_000;

    /** En guaraníes no tiene sentido proponer presupuestos con centenas. */
    private static final double REDONDEO = 1_000;

    private final ReglaHogarRepository reglaRepository;
    private final CategoriaRepository categoriaRepository;
    private final GastoService gastoService;
    private final IngresoService ingresoService;
    private final AhorroService ahorroService;
    private final PresupuestoService presupuestoService;

    public Optional<ReglaPresupuesto> getRegla() {
        return reglaRepository.findFirstByOrderByIdAsc().map(this::aDto);
    }

    @Transactional
    public ReglaPresupuesto guardarRegla(PlantillaRegla plantilla, List<PorcentajeGrupo> porcentajes) {
        if (plantilla == null) {
            throw new ValidacionException("Elegí una plantilla");
        }
        ReglaHogar regla = reglaRepository.findFirstByOrderByIdAsc().orElseGet(ReglaHogar::new);
        regla.setPlantilla(plantilla);
        // Con una plantilla fija los porcentajes personalizados se dejan como estaban.
        if (plantilla == PlantillaRegla.PERSONALIZADA) {
            Map<GrupoGasto, Double> validados = validarPersonalizados(porcentajes);
            regla.getPorcentajes().clear();
            regla.getPorcentajes().putAll(validados);
        }
        return aDto(reglaRepository.save(regla));
    }

    @Transactional
    public boolean desactivarRegla() {
        reglaRepository.deleteAll();
        return true;
    }

    /**
     * Por debajo de dos salarios mínimos de ingreso del hogar, lo básico suele
     * pasar del 50%. Con un sueldo mínimo neto de IPS (Gs. 2.770.040) y dos
     * personas, la línea de pobreza urbana del INE ya se lleva el 67%. En ese
     * caso conviene 70/20/10. Sin ingresos cargados no hay base para sugerir.
     */
    static PlantillaRegla sugerirPlantilla(double ingresoMensual) {
        if (ingresoMensual <= 0) {
            return null;
        }
        return ingresoMensual < 2 * SALARIO_MINIMO ? PlantillaRegla.REGLA_70_20_10 : PlantillaRegla.REGLA_50_30_20;
    }

    /**
     * Compara la regla con lo que pasó en el mes.
     *
     * Lo real incluye los gastos pendientes de pago: a principio de mes el
     * alquiler todavía no se pagó, y dejarlo afuera mostraría las necesidades
     * casi en cero. Los ahorros registrados cuentan en el grupo de ahorro.
     */
    public DistribucionRegla calcularDistribucion(int mes, int anio) {
        double ingreso = ingresoService.getTotalIngresos(mes, anio);
        PlantillaRegla sugerida = sugerirPlantilla(ingreso);
        Optional<ReglaHogar> regla = reglaRepository.findFirstByOrderByIdAsc();
        PlantillaRegla plantilla = regla.map(ReglaHogar::getPlantilla)
                .orElse(sugerida != null ? sugerida : PlantillaRegla.REGLA_50_30_20);
        List<Partida> partidas = plantilla.partidas(regla.map(ReglaHogar::getPorcentajes).orElse(Map.of()));

        Map<GrupoGasto, Double> realPorGrupo = new EnumMap<>(GrupoGasto.class);
        double sinClasificar = 0;
        for (Gasto gasto : gastoService.findAll(mes, anio)) {
            double monto = gasto.getAmount() != null ? gasto.getAmount() : 0;
            GrupoGasto grupo = gasto.getCategoria() != null ? gasto.getCategoria().getGrupo() : null;
            if (grupo == null) {
                sinClasificar += monto;
            } else {
                realPorGrupo.merge(grupo, monto, Double::sum);
            }
        }
        realPorGrupo.merge(GrupoGasto.AHORRO, ahorroService.getTotalAhorros(mes, anio), Double::sum);

        List<PartidaDistribucion> resultado = partidas.stream()
                .map(p -> {
                    double real = p.grupos().stream().mapToDouble(g -> realPorGrupo.getOrDefault(g, 0.0)).sum();
                    return new PartidaDistribucion(p.clave(), p.nombre(), p.porcentaje(), p.grupos(),
                            ingreso * p.porcentaje() / 100, real, ingreso > 0 ? real / ingreso * 100 : 0,
                            p.esAhorro());
                })
                .toList();

        int categoriasSinClasificar = (int) categoriaRepository.findByTipo("GASTO").stream()
                .filter(c -> c.getGrupo() == null)
                .count();

        return new DistribucionRegla(regla.isPresent(), plantilla, sugerida, ingreso, resultado,
                sinClasificar, categoriasSinClasificar);
    }

    /**
     * Propone un presupuesto por categoría a partir de la regla activa. No guarda nada.
     *
     * Cada categoría parte de su gasto de referencia: el mayor entre el mes
     * anterior (completo) y el mes en curso (que puede traer gastos nuevos).
     * - Si la partida entra en lo que recomienda la regla, se respeta tal cual:
     *   inflar el presupuesto del alquiler para "usar" el margen no guía a nadie.
     * - Si se pasa, se recorta en proporción, que es justo lo que la regla pide.
     * - Si ninguna categoría de la partida tiene gastos, se reparte en partes iguales.
     *
     * Las partidas de ahorro no generan presupuesto, porque son una meta y no un
     * techo. Tampoco las categorías sin clasificar.
     */
    public List<PresupuestoSugerido> sugerirPresupuestos(int mes, int anio) {
        ReglaHogar regla = reglaRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ValidacionException(
                        "Activá una regla de distribución antes de pedir presupuestos sugeridos"));
        double ingreso = ingresoService.getTotalIngresos(mes, anio);
        if (ingreso <= 0) {
            return List.of();
        }

        List<Categoria> categorias = categoriaRepository.findByTipo("GASTO").stream()
                .filter(c -> c.getGrupo() != null)
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        YearMonth anterior = YearMonth.of(anio, mes).minusMonths(1);
        Map<Long, Double> gastadoMesAnterior = gastadoPorCategoria(anterior.getMonthValue(), anterior.getYear());
        Map<Long, Double> gastadoMesActual = gastadoPorCategoria(mes, anio);
        Map<Long, Double> presupuestosActuales = new HashMap<>();
        for (Presupuesto p : presupuestoService.getPresupuestos(mes, anio)) {
            presupuestosActuales.put(p.getCategoria().getId(), p.getMonto());
        }

        List<PresupuestoSugerido> sugeridos = new ArrayList<>();
        for (Partida partida : regla.getPlantilla().partidas(regla.getPorcentajes())) {
            if (partida.esAhorro() || partida.porcentaje() <= 0) {
                continue;
            }
            List<Categoria> suyas = categorias.stream()
                    .filter(c -> partida.grupos().contains(c.getGrupo()))
                    .toList();
            if (suyas.isEmpty()) {
                continue;
            }

            double montoPartida = ingreso * partida.porcentaje() / 100;
            Map<Long, Double> referencias = new HashMap<>();
            for (Categoria c : suyas) {
                referencias.put(c.getId(), Math.max(
                        gastadoMesAnterior.getOrDefault(c.getId(), 0.0),
                        gastadoMesActual.getOrDefault(c.getId(), 0.0)));
            }
            double sumaReferencias = referencias.values().stream().mapToDouble(Double::doubleValue).sum();

            for (Categoria categoria : suyas) {
                double referencia = referencias.get(categoria.getId());
                double propuesto;
                if (sumaReferencias <= 0) {
                    propuesto = montoPartida / suyas.size();
                } else if (sumaReferencias > montoPartida) {
                    propuesto = referencia * montoPartida / sumaReferencias;
                } else {
                    propuesto = referencia;
                }
                double monto = redondearHaciaAbajo(propuesto);
                // Un presupuesto en 0 significa "sin límite" en la app, así que no se propone.
                if (monto > 0) {
                    sugeridos.add(new PresupuestoSugerido(categoria, partida.nombre(), monto, referencia,
                            presupuestosActuales.get(categoria.getId())));
                }
            }
        }
        return sugeridos;
    }

    /**
     * Guarda los presupuestos que el usuario confirmó (con los montos que haya
     * editado). Se valida todo antes de escribir, así se aplican todos o ninguno.
     */
    @Transactional
    public List<Presupuesto> aplicarPresupuestos(int mes, int anio, List<PresupuestoInput> presupuestos) {
        if (presupuestos == null || presupuestos.isEmpty()) {
            return List.of();
        }
        for (PresupuestoInput p : presupuestos) {
            if (p.categoriaId() == null || p.monto() == null || p.monto() < 0) {
                throw new ValidacionException("Hay un presupuesto con un monto inválido");
            }
        }
        return presupuestos.stream()
                .map(p -> presupuestoService.setPresupuesto(p.categoriaId(), p.monto(), mes, anio))
                .toList();
    }

    private Map<GrupoGasto, Double> validarPersonalizados(List<PorcentajeGrupo> porcentajes) {
        if (porcentajes == null || porcentajes.isEmpty()) {
            throw new ValidacionException("Una regla personalizada necesita el porcentaje de cada grupo");
        }
        Map<GrupoGasto, Double> mapa = new EnumMap<>(GrupoGasto.class);
        for (PorcentajeGrupo p : porcentajes) {
            if (p.grupo() == null) {
                throw new ValidacionException("Falta el grupo de uno de los porcentajes");
            }
            if (p.porcentaje() < 0 || p.porcentaje() > 100) {
                throw new ValidacionException("Cada porcentaje tiene que estar entre 0 y 100");
            }
            if (mapa.put(p.grupo(), p.porcentaje()) != null) {
                throw new ValidacionException("El grupo " + p.grupo().getNombre() + " está repetido");
            }
        }
        double suma = mapa.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(suma - 100) > 0.01) {
            throw new ValidacionException(
                    "Los porcentajes tienen que sumar 100% (ahora suman " + formatearPorcentaje(suma) + "%)");
        }
        return mapa;
    }

    /** 90.0 se muestra "90" y 33.333 se muestra "33.33". */
    private static String formatearPorcentaje(double valor) {
        double redondeado = Math.round(valor * 100) / 100.0;
        return redondeado == Math.rint(redondeado) ? String.valueOf((long) redondeado) : String.valueOf(redondeado);
    }

    /**
     * Hacia abajo, para que la suma de una partida nunca pase lo que recomienda la
     * regla. El épsilon evita que 2.099.999,9999 (error de punto flotante) caiga a
     * 2.099.000.
     */
    private static double redondearHaciaAbajo(double monto) {
        return Math.floor(monto / REDONDEO + 1e-9) * REDONDEO;
    }

    private Map<Long, Double> gastadoPorCategoria(int mes, int anio) {
        Map<Long, Double> total = new HashMap<>();
        for (Gasto gasto : gastoService.findAll(mes, anio)) {
            if (gasto.getCategoria() != null && gasto.getAmount() != null) {
                total.merge(gasto.getCategoria().getId(), gasto.getAmount(), Double::sum);
            }
        }
        return total;
    }

    private ReglaPresupuesto aDto(ReglaHogar regla) {
        List<PorcentajeGrupo> porcentajes = regla.getPorcentajes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PorcentajeGrupo(e.getKey(), e.getValue()))
                .toList();
        return new ReglaPresupuesto(regla.getPlantilla(), porcentajes);
    }
}
