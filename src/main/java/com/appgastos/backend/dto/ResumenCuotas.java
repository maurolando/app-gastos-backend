package com.appgastos.backend.dto;

import com.appgastos.backend.models.Gasto;

import java.util.List;

/**
 * Estado de una compra en cuotas, vista desde cualquiera de sus cuotas.
 *
 * @param montoCuota     lo que se paga por mes
 * @param totalAPagar    montoCuota por cuotasTotales
 * @param totalPagado    suma de las cuotas marcadas como pagadas, no las vencidas
 * @param saldoPendiente lo que falta: totalAPagar menos totalPagado
 * @param cuotas         las cuotas encontradas, ordenadas por numero
 */
public record ResumenCuotas(
        int cuotaActual,
        int cuotasTotales,
        double montoCuota,
        double totalAPagar,
        double totalPagado,
        double saldoPendiente,
        List<Gasto> cuotas) {
}
