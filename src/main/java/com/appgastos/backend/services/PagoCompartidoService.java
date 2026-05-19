package com.appgastos.backend.services;

import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.PagoCompartido;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.GastoRepository;
import com.appgastos.backend.repositories.PagoCompartidoRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoCompartidoService {

    private final PagoCompartidoRepository pagoCompartidoRepository;
    private final GastoRepository gastoRepository;
    private final PersonaRepository personaRepository;

    public List<PagoCompartido> getPagosPorGasto(Long gastoId) {
        return pagoCompartidoRepository.findByGastoId(gastoId);
    }

    @Transactional
    public PagoCompartido agregarPago(Long gastoId, Long personaId, Double monto, String formaPago, String fecha) {
        Gasto gasto = gastoRepository.findById(gastoId)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado: " + gastoId));
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada: " + personaId));

        PagoCompartido pago = new PagoCompartido();
        pago.setGasto(gasto);
        pago.setPersona(persona);
        pago.setMonto(monto);
        pago.setFormaPago(formaPago);
        pago.setFecha(fecha != null ? LocalDate.parse(fecha) : LocalDate.now());

        PagoCompartido saved = pagoCompartidoRepository.save(pago);

        // Verificar si la suma de pagos cubre el monto total
        double totalPagado = pagoCompartidoRepository.findByGastoId(gastoId)
                .stream().mapToDouble(PagoCompartido::getMonto).sum();

        if (totalPagado >= gasto.getAmount()) {
            gasto.setPagado(true);
            gasto.setFechaPago(LocalDate.now());
            gastoRepository.save(gasto);
        }

        return saved;
    }
}
