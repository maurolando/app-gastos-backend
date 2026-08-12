package com.appgastos.backend.dto;

/**
 * Resumen de lo que hizo un cierre de mes.
 *
 * Devolver un contador en vez de un booleano permite avisarle al usuario cuando
 * el cierre no copio nada porque ya se habia ejecutado, en lugar de mostrarle un
 * "listo" identico al de la primera vez.
 */
public record CierreResult(
        int gastosCopiados,
        int ingresosCopiados,
        int gastosOmitidos,
        int ingresosOmitidos) {
}
