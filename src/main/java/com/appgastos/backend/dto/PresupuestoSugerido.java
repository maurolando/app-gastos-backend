package com.appgastos.backend.dto;

import com.appgastos.backend.models.Categoria;

/**
 * Un presupuesto propuesto a partir de la regla. No se guarda hasta que el
 * usuario lo confirma.
 *
 * @param montoReferencia lo gastado en la categoría del que parte la sugerencia
 * @param montoActual     el presupuesto que ya tiene ese mes, o null si no tiene
 */
public record PresupuestoSugerido(
        Categoria categoria,
        String partida,
        double montoSugerido,
        double montoReferencia,
        Double montoActual) {
}
