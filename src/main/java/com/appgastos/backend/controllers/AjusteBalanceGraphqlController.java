package com.appgastos.backend.controllers;

import com.appgastos.backend.models.AjusteBalance;
import com.appgastos.backend.services.AjusteBalanceService;
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
public class AjusteBalanceGraphqlController {

    private final AjusteBalanceService service;

    @QueryMapping
    public List<AjusteBalance> getAjustesBalance(@Argument Integer mes, @Argument Integer anio) {
        return service.getAjustes(mes, anio);
    }

    @MutationMapping
    public AjusteBalance createAjusteBalance(@Argument Double monto, @Argument String fecha, @Argument String descripcion) {
        return service.createAjuste(monto, fecha != null ? LocalDate.parse(fecha) : null, descripcion);
    }

    @MutationMapping
    public boolean deleteAjusteBalance(@Argument Long id) {
        service.deleteAjuste(id);
        return true;
    }
}
