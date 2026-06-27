package com.appgastos.backend.controllers;

import com.appgastos.backend.models.Presupuesto;
import com.appgastos.backend.services.PresupuestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class PresupuestoGraphqlController {

    @Autowired
    private PresupuestoService presupuestoService;

    @QueryMapping
    public List<Presupuesto> getPresupuestos(@Argument Integer mes, @Argument Integer anio) {
        return presupuestoService.getPresupuestos(mes, anio);
    }

    @MutationMapping
    public Presupuesto setPresupuesto(@Argument Long categoriaId, @Argument Double monto, @Argument Integer mes, @Argument Integer anio) {
        return presupuestoService.setPresupuesto(categoriaId, monto, mes, anio);
    }
}
