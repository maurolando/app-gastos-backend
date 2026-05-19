package com.appgastos.backend.controllers;

import com.appgastos.backend.models.Ingreso;
import com.appgastos.backend.services.IngresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class IngresoGraphqlController {

    private final IngresoService ingresoService;

    @QueryMapping
    public List<Ingreso> getAllIngresos(@Argument Integer mes, @Argument Integer anio) {
        try {
            return ingresoService.getAllIngresos(mes, anio);
        } catch (Exception e) {
            System.err.println("Error en getAllIngresos: " + e.getMessage());
            return List.of();
        }
    }

    @MutationMapping
    public Ingreso createIngreso(
            @Argument Double monto,
            @Argument String fecha,
            @Argument Long categoriaId,
            @Argument Long personaId,
            @Argument Boolean recurrent) {
        try {
            LocalDate parsedFecha = fecha != null ? LocalDate.parse(fecha) : LocalDate.now();
            return ingresoService.createIngreso(monto, parsedFecha, categoriaId, personaId, recurrent);
        } catch (Exception e) {
            System.err.println("Error en createIngreso: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
