package com.appgastos.backend.services;

import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.dto.ResumenCuotas;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.GastoRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import com.appgastos.backend.repositories.PagoCompartidoRepository;
import com.appgastos.backend.models.Persona;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoService {
    private final GastoRepository repository;
    private final PersonaRepository personaRepository;
    private final CategoriaRepository categoriaRepository;
    private final PagoCompartidoRepository pagoCompartidoRepository;

    public List<Gasto> findAll(Integer mes, Integer anio) {
        List<Gasto> all = repository.findAll();
        if (mes == null && anio == null)
            return all;

        return all.stream()
                .filter(g -> {
                    if (g.getDate() == null)
                        return false;
                    boolean match = true;
                    if (mes != null)
                        match &= g.getDate().getMonthValue() == mes;
                    if (anio != null)
                        match &= g.getDate().getYear() == anio;
                    return match;
                })
                .toList();
    }

    public Gasto createGasto(Double amount, Long categoriaId, LocalDate date, String description, Long personaId,
            String formaPago, Boolean recurrent, Boolean pagado, LocalDate fechaVencimiento, Boolean esCompartido,
            Integer cuotaActual, Integer cuotasTotales) {
        Persona persona = personaId != null ? personaRepository.findById(personaId).orElse(null) : null;
        Categoria categoria = categoriaId != null ? categoriaRepository.findById(categoriaId).orElse(null) : null;

        Gasto gasto = new Gasto();
        gasto.setAmount(amount);
        gasto.setCategoria(categoria);
        gasto.setDate(date != null ? date : LocalDate.now());
        gasto.setDescription(description);
        gasto.setPersona(persona);
        gasto.setFormaPago(formaPago);
        gasto.setRecurrent(recurrent != null && recurrent);

        boolean estaPagado = pagado != null && pagado;
        gasto.setPagado(estaPagado);
        // Un gasto que se registra ya pagado se salda en su propia fecha:
        // sin esto quedaría contando en el balance pero sin fecha de pago.
        gasto.setFechaPago(estaPagado ? gasto.getDate() : null);

        gasto.setFechaVencimiento(fechaVencimiento);
        gasto.setEsCompartido(esCompartido != null && esCompartido);
        gasto.setCuotaActual(cuotaActual);
        gasto.setCuotasTotales(cuotasTotales);

        return repository.save(gasto);
    }

    /**
     * Actualiza un gasto existente.
     *
     * Los campos obligatorios del formulario (monto, fecha, categoria, persona, forma de
     * pago y los flags) conservan su valor anterior si llegan en null. Los opcionales
     * (descripcion, vencimiento y cuotas) se reemplazan tal cual vienen, para que el
     * usuario pueda vaciarlos desde la interfaz.
     */
    @Transactional
    public Gasto updateGasto(Long id, Double amount, Long categoriaId, LocalDate date, String description,
            Long personaId, String formaPago, Boolean recurrent, Boolean pagado, LocalDate fechaVencimiento,
            Boolean esCompartido, Integer cuotaActual, Integer cuotasTotales) {
        Gasto gasto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado: " + id));

        if (amount != null) gasto.setAmount(amount);
        if (date != null) gasto.setDate(date);
        if (categoriaId != null) categoriaRepository.findById(categoriaId).ifPresent(gasto::setCategoria);
        if (personaId != null) personaRepository.findById(personaId).ifPresent(gasto::setPersona);
        if (formaPago != null) gasto.setFormaPago(formaPago);
        if (recurrent != null) gasto.setRecurrent(recurrent);
        if (esCompartido != null) gasto.setEsCompartido(esCompartido);

        gasto.setDescription(description);
        gasto.setFechaVencimiento(fechaVencimiento);
        gasto.setCuotaActual(cuotaActual);
        gasto.setCuotasTotales(cuotasTotales);

        if (pagado != null) {
            boolean estabaPagado = Boolean.TRUE.equals(gasto.getPagado());
            gasto.setPagado(pagado);
            // La fecha de pago sigue al flag: si se marca pagado sin tenerla, se salda en
            // la fecha del gasto; si se vuelve a pendiente, deja de tener sentido.
            if (pagado && !estabaPagado) {
                gasto.setFechaPago(gasto.getDate());
            } else if (!pagado) {
                gasto.setFechaPago(null);
            }
        }

        return repository.save(gasto);
    }

    @Transactional
    public void deleteGasto(Long id) {
        Gasto gasto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado: " + id));

        // Los aportes tienen FK al gasto: se borran primero para no romper la restriccion.
        pagoCompartidoRepository.deleteAll(pagoCompartidoRepository.findByGastoId(id));
        repository.delete(gasto);
    }

    /**
     * Estado de la compra en cuotas a la que pertenece este gasto.
     *
     * Las cuotas de una compra no estan unidas por ningun identificador: cada mes
     * es una fila suelta que el cierre copia con cuotaActual+1. Se reconocen por
     * coincidencia de descripcion, categoria, persona y total de cuotas, la misma
     * regla que usa CierreService para no duplicar una cuota, salvo el monto: dos
     * cuotas de una misma compra pueden diferir (una entrega mas grande, un ajuste)
     * y seguir siendo la misma compra.
     *
     * El limite de esto es que dos compras identicas en los cuatro campos se
     * cuentan como una sola. Resolverlo de verdad pide darle identidad propia a la
     * compra, que es un cambio de esquema.
     */
    public ResumenCuotas getResumenCuotas(Long gastoId) {
        Gasto gasto = repository.findById(gastoId)
                .orElseThrow(() -> new ValidacionException("El gasto no existe o ya fue borrado."));

        if (gasto.getCuotasTotales() == null || gasto.getCuotaActual() == null) {
            throw new ValidacionException("Este gasto no esta dividido en cuotas.");
        }

        List<Gasto> cuotas = repository.findAll().stream()
                .filter(g -> esDeLaMismaCompra(g, gasto))
                .sorted(Comparator.comparing(Gasto::getCuotaActual,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        double montoCuota = gasto.getAmount() != null ? gasto.getAmount() : 0.0;
        double totalAPagar = montoCuota * gasto.getCuotasTotales();
        double totalPagado = cuotas.stream()
                .filter(g -> Boolean.TRUE.equals(g.getPagado()))
                .mapToDouble(g -> g.getAmount() != null ? g.getAmount() : 0.0)
                .sum();

        return new ResumenCuotas(
                gasto.getCuotaActual(),
                gasto.getCuotasTotales(),
                montoCuota,
                totalAPagar,
                totalPagado,
                totalAPagar - totalPagado,
                cuotas);
    }

    private boolean esDeLaMismaCompra(Gasto candidato, Gasto referencia) {
        return Objects.equals(candidato.getCuotasTotales(), referencia.getCuotasTotales())
                && Objects.equals(candidato.getDescription(), referencia.getDescription())
                && Objects.equals(idDeCategoria(candidato), idDeCategoria(referencia))
                && Objects.equals(idDePersona(candidato), idDePersona(referencia));
    }

    private Long idDeCategoria(Gasto g) {
        return g.getCategoria() != null ? g.getCategoria().getId() : null;
    }

    private Long idDePersona(Gasto g) {
        return g.getPersona() != null ? g.getPersona().getId() : null;
    }

    public LocalDate getLastGastoDate() {
        return repository.findAll().stream()
                .map(Gasto::getDate)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public Gasto markAsPaid(Long id, Double monto, Long personaId, String formaPago, LocalDate fechaPago) {
        Gasto gasto = repository.findById(id).orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
        Persona persona = personaId != null ? personaRepository.findById(personaId).orElse(null) : null;

        // NO modificamos el monto original, solo registramos quién pagó y cuándo
        gasto.setPersona(persona);
        gasto.setFormaPago(formaPago);
        gasto.setFechaPago(fechaPago != null ? fechaPago : LocalDate.now());
        gasto.setPagado(true);

        return repository.save(gasto);
    }

    public void deleteAll() {
        pagoCompartidoRepository.deleteAll(); // Borrar dependencias primero para no romper FK
        repository.deleteAll();
    }

    public Double getTotalGastos(Integer mes, Integer anio) {
        return findAll(mes, anio).stream()
                .filter(g -> g.getPagado() != null && g.getPagado())     // Solo pagados
                .filter(g -> {
                    // Gastos compartidos: solo cuentan cuando pagado=true (lo setea PagoCompartidoService)
                    // Gastos normales: siempre cuentan si pagado=true
                    boolean esCompartido = Boolean.TRUE.equals(g.getEsCompartido());
                    if (!esCompartido) return true; // gasto normal pagado → siempre suma
                    // compartido: solo suma si pagado fue marcado como true (suma de aportes >= amount)
                    return true;
                })
                .mapToDouble(g -> g.getAmount() != null ? g.getAmount() : 0.0)
                .sum();
    }
}
