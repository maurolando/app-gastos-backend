package com.appgastos.backend.dto;

import com.appgastos.backend.models.PlantillaRegla;

import java.util.List;

/**
 * La guía de distribución calculada para un mes.
 *
 * Si el hogar no activó ninguna regla, {@code activa} es false y la plantilla
 * es la sugerida, para mostrarla como invitación sin imponerla.
 */
public record DistribucionRegla(
        boolean activa,
        PlantillaRegla plantilla,
        PlantillaRegla plantillaSugerida,
        double ingresoMensual,
        List<PartidaDistribucion> partidas,
        double montoSinClasificar,
        int categoriasSinClasificar) {
}
