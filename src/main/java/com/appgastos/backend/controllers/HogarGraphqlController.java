package com.appgastos.backend.controllers;

import com.appgastos.backend.dto.ConfigHogar;
import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.services.CierreService;
import com.appgastos.backend.services.PeriodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class HogarGraphqlController {

    private final PeriodoService periodoService;
    private final CierreService cierreService;

    @QueryMapping
    public ConfigHogar getConfigHogar() {
        return config(periodoService.hogar());
    }

    @MutationMapping
    public ConfigHogar setConfigHogar(@Argument int diaInicioMes, @Argument boolean cierreAutomatico,
            @Argument String hoy) {
        return config(periodoService.configurar(diaInicioMes, cierreAutomatico, LocalDate.parse(hoy)));
    }

    @MutationMapping
    public int cerrarMesesPendientes(@Argument String hoy) {
        return cierreService.cerrarPendientes(LocalDate.parse(hoy));
    }

    private ConfigHogar config(Hogar hogar) {
        return new ConfigHogar(PeriodoService.diaInicio(hogar), Boolean.TRUE.equals(hogar.getCierreAutomatico()));
    }
}
