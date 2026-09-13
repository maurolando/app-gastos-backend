package com.appgastos.backend.services;

import com.appgastos.backend.dto.DistribucionRegla;
import com.appgastos.backend.dto.PartidaDistribucion;
import com.appgastos.backend.dto.PorcentajeGrupo;
import com.appgastos.backend.dto.PresupuestoInput;
import com.appgastos.backend.dto.PresupuestoSugerido;
import com.appgastos.backend.dto.ReglaPresupuesto;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.GrupoGasto;
import com.appgastos.backend.models.Presupuesto;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.PresupuestoRepository;
import com.appgastos.backend.repositories.ReglaHogarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.appgastos.backend.models.GrupoGasto.*;
import static com.appgastos.backend.models.PlantillaRegla.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * La guía de distribución del ingreso: qué plantilla se sugiere, cómo se compara
 * con lo real del mes y cómo se proponen y aplican presupuestos.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReglaPresupuestoServiceTest {

    private static final int MES = 5;
    private static final int ANIO = 2026;
    /** Un salario mínimo (Gs. 3.044.000) menos el 9% de IPS. */
    private static final double SUELDO_MINIMO_NETO = 2_770_040;

    @Autowired private ReglaPresupuestoService service;
    @Autowired private GastoService gastoService;
    @Autowired private IngresoService ingresoService;
    @Autowired private AhorroService ahorroService;
    @Autowired private PresupuestoService presupuestoService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private ReglaHogarRepository reglaRepository;

    @BeforeEach
    void setUp() {
        gastoService.deleteAll();
        ingresoService.deleteAll();
        ahorroService.deleteAll();
        presupuestoRepository.deleteAll();
        reglaRepository.deleteAll();
        categoriaRepository.deleteAll();
    }

    @Test
    @DisplayName("sugiere 70/20/10 con ingresos bajos y 50/30/20 desde dos salarios minimos")
    void sugierePlantillaSegunIngreso() {
        assertThat(ReglaPresupuestoService.sugerirPlantilla(SUELDO_MINIMO_NETO)).isEqualTo(REGLA_70_20_10);
        assertThat(ReglaPresupuestoService.sugerirPlantilla(2 * ReglaPresupuestoService.SALARIO_MINIMO))
                .isEqualTo(REGLA_50_30_20);
        assertThat(ReglaPresupuestoService.sugerirPlantilla(0)).isNull();
    }

    @Test
    @DisplayName("sin regla activa muestra la plantilla sugerida sin imponerla")
    void sinReglaMuestraLaSugerida() {
        ingreso(SUELDO_MINIMO_NETO);

        DistribucionRegla d = service.calcularDistribucion(MES, ANIO);

        assertThat(d.activa()).isFalse();
        assertThat(d.plantilla()).isEqualTo(REGLA_70_20_10);
        assertThat(d.plantillaSugerida()).isEqualTo(REGLA_70_20_10);
        assertThat(partida(d, "NECESIDADES").montoRecomendado()).isCloseTo(1_939_028, within(0.01));
        assertThat(partida(d, "AHORRO").montoRecomendado()).isCloseTo(554_008, within(0.01));
        assertThat(partida(d, "DESEOS").montoRecomendado()).isCloseTo(277_004, within(0.01));
    }

    @Test
    @DisplayName("suma lo real por partida: pendientes incluidos, educacion como necesidad y ahorros registrados")
    void calculaLoRealPorPartida() {
        service.guardarRegla(REGLA_50_30_20, null);
        ingreso(5_000_000);
        Categoria alquiler = categoria("Alquiler", NECESIDADES);
        gasto(alquiler, 1_500_000, MES, false);
        gasto(alquiler, 1_500_000, MES - 1, true); // otro mes: no cuenta
        gasto(categoria("Curso de ingles", EDUCACION), 300_000, MES, true);
        gasto(categoria("Salidas", DESEOS), 400_000, MES, true);
        gasto(categoria("Varios", null), 100_000, MES, true);
        ahorroService.createAhorro(500_000.0, LocalDate.of(ANIO, MES, 20), null, "Fondo de emergencia");

        DistribucionRegla d = service.calcularDistribucion(MES, ANIO);

        assertThat(d.activa()).isTrue();
        PartidaDistribucion necesidades = partida(d, "NECESIDADES");
        assertThat(necesidades.montoRecomendado()).isCloseTo(2_500_000, within(0.01));
        assertThat(necesidades.montoReal()).isCloseTo(1_800_000, within(0.01));
        assertThat(necesidades.porcentajeReal()).isCloseTo(36, within(0.01));
        assertThat(partida(d, "DESEOS").montoReal()).isCloseTo(400_000, within(0.01));
        assertThat(partida(d, "AHORRO").montoReal()).isCloseTo(500_000, within(0.01));
        assertThat(partida(d, "AHORRO").esAhorro()).isTrue();
        assertThat(d.montoSinClasificar()).isCloseTo(100_000, within(0.01));
        assertThat(d.categoriasSinClasificar()).isEqualTo(1);
    }

    @Test
    @DisplayName("las 6 jarras separan cada grupo y suman 100%")
    void seisJarrasSeparanCadaGrupo() {
        service.guardarRegla(SEIS_JARRAS, null);
        ingreso(4_000_000);
        gasto(categoria("Curso", EDUCACION), 300_000, MES, true);

        DistribucionRegla d = service.calcularDistribucion(MES, ANIO);

        assertThat(d.partidas()).hasSize(6);
        assertThat(d.partidas().stream().mapToDouble(PartidaDistribucion::porcentaje).sum()).isEqualTo(100);
        assertThat(partida(d, "EDUCACION").montoReal()).isCloseTo(300_000, within(0.01));
        assertThat(partida(d, "NECESIDADES").montoReal()).isZero();
    }

    @Test
    @DisplayName("una regla personalizada que no suma 100% se rechaza")
    void personalizadaQueNoSumaCienSeRechaza() {
        assertThatThrownBy(() -> service.guardarRegla(PERSONALIZADA, porcentajes(60, 30, 0, 20, 0, 0)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("sumar 100%");

        assertThat(service.getRegla()).isEmpty();
    }

    @Test
    @DisplayName("una regla personalizada sin porcentajes o con un grupo repetido se rechaza")
    void personalizadaIncompletaSeRechaza() {
        assertThatThrownBy(() -> service.guardarRegla(PERSONALIZADA, null))
                .isInstanceOf(ValidacionException.class);

        List<PorcentajeGrupo> repetido = List.of(new PorcentajeGrupo(NECESIDADES, 50), new PorcentajeGrupo(NECESIDADES, 50));
        assertThatThrownBy(() -> service.guardarRegla(PERSONALIZADA, repetido))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    @DisplayName("la personalizada usa sus porcentajes y los conserva al cambiar de plantilla")
    void personalizadaConservaSusPorcentajes() {
        ingreso(4_000_000);
        service.guardarRegla(PERSONALIZADA, porcentajes(60, 15, 5, 10, 10, 0));

        DistribucionRegla d = service.calcularDistribucion(MES, ANIO);
        assertThat(d.partidas()).hasSize(6);
        assertThat(partida(d, "NECESIDADES").montoRecomendado()).isCloseTo(2_400_000, within(0.01));
        assertThat(partida(d, "DONACIONES").porcentaje()).isZero();

        service.guardarRegla(REGLA_50_30_20, null);

        ReglaPresupuesto regla = service.getRegla().orElseThrow();
        assertThat(regla.plantilla()).isEqualTo(REGLA_50_30_20);
        assertThat(regla.porcentajes()).extracting(PorcentajeGrupo::porcentaje)
                .containsExactly(60.0, 15.0, 5.0, 10.0, 10.0, 0.0);
        assertThat(reglaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("desactivar la guia vuelve a mostrar solo la sugerencia")
    void desactivarVuelveALaSugerida() {
        ingreso(8_000_000);
        service.guardarRegla(SEIS_JARRAS, null);

        service.desactivarRegla();

        DistribucionRegla d = service.calcularDistribucion(MES, ANIO);
        assertThat(d.activa()).isFalse();
        assertThat(d.plantilla()).isEqualTo(REGLA_50_30_20);
    }

    @Test
    @DisplayName("si la partida alcanza, sugiere lo gastado sin inflar los presupuestos")
    void siLaPartidaAlcanzaNoInfla() {
        service.guardarRegla(REGLA_70_20_10, null);
        ingreso(4_000_000);
        Categoria alquiler = categoria("Alquiler", NECESIDADES);
        Categoria supermercado = categoria("Supermercado", NECESIDADES);
        Categoria salidas = categoria("Salidas", DESEOS);
        categoria("Inversiones", INVERSION);
        categoria("Varios", null);
        gasto(alquiler, 1_500_000, MES - 1, true);
        gasto(supermercado, 500_000, MES - 1, true);
        gasto(supermercado, 600_000, MES, true);
        gasto(salidas, 150_000, MES, true);
        presupuestoService.setPresupuesto(alquiler.getId(), 1_000_000.0, MES, ANIO);

        List<PresupuestoSugerido> sugeridos = service.sugerirPresupuestos(MES, ANIO);

        // Necesidades recomienda 2.800.000 y se gastan 2.100.000 (el mayor de cada mes):
        // se sugiere lo gastado y el resto queda como margen. Las inversiones son
        // ahorro (meta, no techo) y "Varios" no está clasificada.
        assertThat(sugeridos)
                .extracting(s -> s.categoria().getNombre(), PresupuestoSugerido::montoSugerido,
                        PresupuestoSugerido::montoReferencia, PresupuestoSugerido::montoActual)
                .containsExactlyInAnyOrder(
                        tuple("Alquiler", 1_500_000.0, 1_500_000.0, 1_000_000.0),
                        tuple("Supermercado", 600_000.0, 600_000.0, null),
                        tuple("Salidas", 150_000.0, 150_000.0, null));
    }

    @Test
    @DisplayName("si la partida no alcanza, recorta cada categoria en proporcion para cumplir la regla")
    void siLaPartidaNoAlcanzaRecorta() {
        service.guardarRegla(REGLA_70_20_10, null);
        ingreso(SUELDO_MINIMO_NETO);
        Categoria salidas = categoria("Salidas", DESEOS);
        Categoria streaming = categoria("Streaming", DESEOS);
        gasto(salidas, 400_000, MES - 1, true);
        gasto(streaming, 100_000, MES - 1, true);

        List<PresupuestoSugerido> sugeridos = service.sugerirPresupuestos(MES, ANIO);

        // Deseos recomienda 277.004 y se gastaron 500.000: cada una baja al 55,4%.
        assertThat(sugeridos)
                .extracting(s -> s.categoria().getNombre(), PresupuestoSugerido::montoSugerido)
                .containsExactly(tuple("Salidas", 221_000.0), tuple("Streaming", 55_000.0));
        assertThat(sugeridos.stream().mapToDouble(PresupuestoSugerido::montoSugerido).sum())
                .isLessThanOrEqualTo(277_004);
    }

    @Test
    @DisplayName("sin historial reparte parejo y redondea hacia abajo a miles")
    void sinHistorialRepartePArejo() {
        service.guardarRegla(REGLA_50_30_20, null);
        ingreso(3_333_333);
        categoria("Salidas", DESEOS);
        categoria("Streaming", DESEOS);

        List<PresupuestoSugerido> sugeridos = service.sugerirPresupuestos(MES, ANIO);

        // 30% de 3.333.333 = 999.999,9; la mitad es 499.999,95.
        assertThat(sugeridos)
                .filteredOn(s -> s.partida().equals("Deseos"))
                .extracting(PresupuestoSugerido::montoSugerido)
                .containsExactly(499_000.0, 499_000.0);
    }

    @Test
    @DisplayName("pedir presupuestos sugeridos sin una regla activa falla con un mensaje claro")
    void sugerirSinReglaFalla() {
        ingreso(4_000_000);

        assertThatThrownBy(() -> service.sugerirPresupuestos(MES, ANIO))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("regla");
    }

    @Test
    @DisplayName("sin ingresos del mes no sugiere presupuestos")
    void sinIngresosNoSugiere() {
        service.guardarRegla(REGLA_50_30_20, null);
        categoria("Alquiler", NECESIDADES);

        assertThat(service.sugerirPresupuestos(MES, ANIO)).isEmpty();
    }

    @Test
    @DisplayName("aplicar los presupuestos confirmados crea los nuevos y actualiza los existentes")
    void aplicarCreaYActualiza() {
        Categoria alquiler = categoria("Alquiler", NECESIDADES);
        Categoria salidas = categoria("Salidas", DESEOS);
        presupuestoService.setPresupuesto(alquiler.getId(), 1_000_000.0, MES, ANIO);

        service.aplicarPresupuestos(MES, ANIO, List.of(
                new PresupuestoInput(alquiler.getId(), 2_100_000.0),
                new PresupuestoInput(salidas.getId(), 400_000.0)));

        assertThat(presupuestoService.getPresupuestos(MES, ANIO))
                .extracting(p -> p.getCategoria().getNombre(), Presupuesto::getMonto)
                .containsExactlyInAnyOrder(tuple("Alquiler", 2_100_000.0), tuple("Salidas", 400_000.0));
    }

    @Test
    @DisplayName("si un monto confirmado es invalido no se aplica ninguno")
    void aplicarConMontoInvalidoNoGuardaNada() {
        Categoria alquiler = categoria("Alquiler", NECESIDADES);
        Categoria salidas = categoria("Salidas", DESEOS);

        assertThatThrownBy(() -> service.aplicarPresupuestos(MES, ANIO, List.of(
                new PresupuestoInput(alquiler.getId(), 2_100_000.0),
                new PresupuestoInput(salidas.getId(), -1.0))))
                .isInstanceOf(ValidacionException.class);

        assertThat(presupuestoService.getPresupuestos(MES, ANIO)).isEmpty();
    }

    private Categoria categoria(String nombre, GrupoGasto grupo) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setTipo("GASTO");
        categoria.setGrupo(grupo);
        return categoriaRepository.save(categoria);
    }

    private void gasto(Categoria categoria, double monto, int mes, boolean pagado) {
        gastoService.createGasto(monto, categoria.getId(), LocalDate.of(ANIO, mes, 10), null,
                null, "Efectivo", false, pagado, null, false, null, null);
    }

    private void ingreso(double monto) {
        ingresoService.createIngreso(monto, LocalDate.of(ANIO, MES, 1), null, null, true);
    }

    private PartidaDistribucion partida(DistribucionRegla distribucion, String clave) {
        return distribucion.partidas().stream()
                .filter(p -> p.clave().equals(clave))
                .findFirst()
                .orElseThrow();
    }

    private List<PorcentajeGrupo> porcentajes(double necesidades, double deseos, double educacion,
            double ahorro, double inversion, double donaciones) {
        return List.of(
                new PorcentajeGrupo(NECESIDADES, necesidades),
                new PorcentajeGrupo(DESEOS, deseos),
                new PorcentajeGrupo(EDUCACION, educacion),
                new PorcentajeGrupo(AHORRO, ahorro),
                new PorcentajeGrupo(INVERSION, inversion),
                new PorcentajeGrupo(DONACIONES, donaciones));
    }
}
