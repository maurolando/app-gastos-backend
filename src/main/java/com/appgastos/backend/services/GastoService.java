package com.appgastos.backend.services;

import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.GastoRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import com.appgastos.backend.repositories.PagoCompartidoRepository;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.models.PagoCompartido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
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
        gasto.setPagado(pagado != null ? pagado : false);
        gasto.setFechaVencimiento(fechaVencimiento);
        gasto.setEsCompartido(esCompartido != null && esCompartido);
        gasto.setCuotaActual(cuotaActual);
        gasto.setCuotasTotales(cuotasTotales);

        return repository.save(gasto);
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

    public Gasto updateGasto(Long id, Double amount, Long categoriaId, LocalDate date, String description, Long personaId,
            String formaPago, Boolean recurrent, Boolean pagado, LocalDate fechaVencimiento, Boolean esCompartido,
            Integer cuotaActual, Integer cuotasTotales) {
        Gasto gasto = repository.findById(id).orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
        Persona persona = personaId != null ? personaRepository.findById(personaId).orElse(null) : null;
        Categoria categoria = categoriaId != null ? categoriaRepository.findById(categoriaId).orElse(null) : null;

        gasto.setAmount(amount);
        gasto.setCategoria(categoria);
        gasto.setDate(date != null ? date : LocalDate.now());
        gasto.setDescription(description);
        gasto.setPersona(persona);
        gasto.setFormaPago(formaPago);
        gasto.setRecurrent(recurrent != null && recurrent);
        gasto.setFechaVencimiento(fechaVencimiento);
        gasto.setEsCompartido(esCompartido != null && esCompartido);
        gasto.setCuotaActual(cuotaActual);
        gasto.setCuotasTotales(cuotasTotales);

        if (Boolean.TRUE.equals(gasto.getEsCompartido())) {
            double totalPagado = pagoCompartidoRepository.findByGastoId(id)
                    .stream().mapToDouble(PagoCompartido::getMonto).sum();
            if (totalPagado >= amount) {
                gasto.setPagado(true);
                if (gasto.getFechaPago() == null) {
                    gasto.setFechaPago(LocalDate.now());
                }
            } else {
                gasto.setPagado(false);
                gasto.setFechaPago(null);
            }
        } else {
            gasto.setPagado(pagado != null ? pagado : false);
        }

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
