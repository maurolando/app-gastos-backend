package com.appgastos.backend.services;

import com.appgastos.backend.SesionDePrueba;
import com.appgastos.backend.controllers.GastoGraphqlController;
import com.appgastos.backend.models.AjusteBalance;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.HogarRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AjusteBalanceServiceTest {

    @Autowired private AjusteBalanceService service;
    @Autowired private IngresoService ingresoService;
    @Autowired private CierreService cierreService;
    @Autowired private GastoGraphqlController gastoController;
    @Autowired private HogarRepository hogarRepository;
    @Autowired private PersonaRepository personaRepository;

    @BeforeEach
    void iniciarSesion() {
        SesionDePrueba.iniciar(hogarRepository, personaRepository);
    }

    @Test
    @DisplayName("el ajuste suma o resta solo al balance de su mes")
    void ajustaElBalanceDelMes() {
        ingresoService.createIngreso(1_000_000.0, LocalDate.of(2026, 10, 5), null, null, false);
        service.createAjuste(50_000.0, LocalDate.of(2026, 10, 10), "Efectivo encontrado");
        service.createAjuste(-20_000.0, LocalDate.of(2026, 10, 12), null);
        service.createAjuste(99_000.0, LocalDate.of(2026, 11, 2), null);

        assertThat(gastoController.getGlobalBalance(10, 2026)).isEqualTo(1_030_000.0);
        assertThat(gastoController.getGlobalBalance(11, 2026)).isEqualTo(99_000.0);
        assertThat(service.getAjustes(10, 2026)).hasSize(2);
    }

    @Test
    @DisplayName("un ajuste en cero no se acepta")
    void rechazaCero() {
        assertThatThrownBy(() -> service.createAjuste(0.0, null, null))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> service.createAjuste(null, null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    @DisplayName("no se puede borrar el ajuste de otro hogar")
    void aisladoPorHogar() {
        AjusteBalance ajeno = service.createAjuste(10_000.0, LocalDate.of(2026, 10, 1), null);

        SesionDePrueba.iniciar(hogarRepository, personaRepository);
        service.deleteAjuste(ajeno.getId());
        assertThat(service.getAjustes(null, null)).isEmpty();

        Persona duenio = personaRepository.findAll().stream()
                .filter(p -> p.getHogar().getId().equals(ajeno.getHogar().getId()))
                .findFirst().orElseThrow();
        SesionDePrueba.loguear(duenio);
        assertThat(service.getAjustes(null, null)).hasSize(1);
    }

    @Test
    @DisplayName("reiniciar datos también borra los ajustes")
    void reiniciarBorraAjustes() {
        service.createAjuste(10_000.0, LocalDate.of(2026, 10, 1), null);
        cierreService.reiniciarDatos();
        assertThat(service.getAjustes(null, null)).isEmpty();
    }
}
