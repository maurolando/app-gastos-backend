package com.appgastos.backend.controllers;

import com.appgastos.backend.models.Ahorro;
import com.appgastos.backend.services.AhorroService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AhorroGraphqlController {

    private final AhorroService ahorroService;

    @QueryMapping
    public List<Ahorro> getAllAhorros(@Argument Integer mes, @Argument Integer anio) {
        try {
            return ahorroService.getAllAhorros(mes, anio);
        } catch (Exception e) {
            System.err.println("Error en getAllAhorros: " + e.getMessage());
            return List.of();
        }
    }

    @MutationMapping
    public Ahorro createAhorro(
            @Argument Double monto,
            @Argument String fecha,
            @Argument Long personaId,
            @Argument String descripcion) {
        try {
            LocalDate parsedFecha = fecha != null ? LocalDate.parse(fecha) : LocalDate.now();
            return ahorroService.createAhorro(monto, parsedFecha, personaId, descripcion);
        } catch (Exception e) {
            System.err.println("Error en createAhorro: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @MutationMapping
    public boolean deleteAhorro(@Argument Long id) {
        try {
            ahorroService.deleteAhorro(id);
            return true;
        } catch (Exception e) {
            System.err.println("Error en deleteAhorro: " + e.getMessage());
            return false;
        }
    }
}
