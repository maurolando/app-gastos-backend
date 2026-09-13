package com.appgastos.backend.controllers;

import com.appgastos.backend.dto.DistribucionRegla;
import com.appgastos.backend.dto.PorcentajeGrupo;
import com.appgastos.backend.dto.PresupuestoInput;
import com.appgastos.backend.dto.PresupuestoSugerido;
import com.appgastos.backend.dto.ReglaPresupuesto;
import com.appgastos.backend.models.PlantillaRegla;
import com.appgastos.backend.models.Presupuesto;
import com.appgastos.backend.services.ReglaPresupuestoService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReglaPresupuestoGraphqlController {

    private final ReglaPresupuestoService service;

    @QueryMapping
    public ReglaPresupuesto getReglaPresupuesto() {
        return service.getRegla().orElse(null);
    }

    @QueryMapping
    public DistribucionRegla getDistribucionRegla(@Argument Integer mes, @Argument Integer anio) {
        return service.calcularDistribucion(mes, anio);
    }

    @QueryMapping
    public List<PresupuestoSugerido> getPresupuestosSugeridos(@Argument Integer mes, @Argument Integer anio) {
        return service.sugerirPresupuestos(mes, anio);
    }

    @MutationMapping
    public ReglaPresupuesto guardarReglaPresupuesto(@Argument PlantillaRegla plantilla,
            @Argument List<PorcentajeGrupo> porcentajes) {
        return service.guardarRegla(plantilla, porcentajes);
    }

    @MutationMapping
    public boolean desactivarReglaPresupuesto() {
        return service.desactivarRegla();
    }

    @MutationMapping
    public List<Presupuesto> aplicarPresupuestos(@Argument Integer mes, @Argument Integer anio,
            @Argument List<PresupuestoInput> presupuestos) {
        return service.aplicarPresupuestos(mes, anio, presupuestos);
    }
}
