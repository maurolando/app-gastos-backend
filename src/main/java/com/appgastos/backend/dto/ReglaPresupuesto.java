package com.appgastos.backend.dto;

import com.appgastos.backend.models.PlantillaRegla;

import java.util.List;

/** La regla guardada del hogar, con los porcentajes personalizados que tenga. */
public record ReglaPresupuesto(PlantillaRegla plantilla, List<PorcentajeGrupo> porcentajes) {
}
