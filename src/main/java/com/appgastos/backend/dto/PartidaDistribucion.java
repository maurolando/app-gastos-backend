package com.appgastos.backend.dto;

import com.appgastos.backend.models.GrupoGasto;

import java.util.List;

/**
 * Una partida de la regla aplicada a un mes: cuánto recomienda la regla y
 * cuánto se usó en realidad, en guaraníes y como porcentaje del ingreso.
 */
public record PartidaDistribucion(
        String clave,
        String nombre,
        double porcentaje,
        List<GrupoGasto> grupos,
        double montoRecomendado,
        double montoReal,
        double porcentajeReal,
        boolean esAhorro) {
}
