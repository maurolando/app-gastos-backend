package com.appgastos.backend.controllers;

import com.appgastos.backend.SesionDePrueba;
import com.appgastos.backend.dto.SaldoPersona;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.HogarRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import com.appgastos.backend.services.AhorroService;
import com.appgastos.backend.services.GastoService;
import com.appgastos.backend.services.IngresoService;
import com.appgastos.backend.services.PagoCompartidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** #59: el saldo del mes de cada persona del hogar. */
@SpringBootTest
@ActiveProfiles("test")
class SaldosPorPersonaTest {

    @Autowired private GastoGraphqlController controller;
    @Autowired private GastoService gastoService;
    @Autowired private IngresoService ingresoService;
    @Autowired private AhorroService ahorroService;
    @Autowired private PagoCompartidoService pagoCompartidoService;
    @Autowired private HogarRepository hogarRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    @Test
    @DisplayName("ingresos - gastos pagados - ahorros; en un compartido cuenta solo el aporte de cada uno")
    void saldoPorPersona() {
        Hogar hogar = SesionDePrueba.iniciar(hogarRepository, personaRepository);
        Persona ana = persona(hogar, "Ana saldo");
        Persona beto = persona(hogar, "Beto saldo");
        Categoria cat = new Categoria();
        cat.setHogar(hogar);
        cat.setNombre("Varios");
        cat.setTipo("GASTO");
        cat = categoriaRepository.save(cat);
        LocalDate dia = LocalDate.of(2026, 10, 10);

        ingresoService.createIngreso(1_000_000.0, dia, cat.getId(), ana.getId(), false);
        ingresoService.createIngreso(500_000.0, dia, cat.getId(), beto.getId(), false);
        gastoService.createGasto(200_000.0, cat.getId(), dia, "Super", ana.getId(), "EFECTIVO", false, true, null, false, null, null);
        gastoService.createGasto(999_000.0, cat.getId(), dia, "Sin pagar", ana.getId(), "EFECTIVO", false, false, null, false, null, null);
        ahorroService.createAhorro(100_000.0, dia, beto.getId(), null);
        Long luz = gastoService.createGasto(300_000.0, cat.getId(), dia, "Luz", ana.getId(), "EFECTIVO", false, false, null, true, null, null).getId();
        pagoCompartidoService.agregarPago(luz, ana.getId(), 100_000.0, "EFECTIVO", dia.toString());
        pagoCompartidoService.agregarPago(luz, beto.getId(), 200_000.0, "EFECTIVO", dia.toString());

        assertThat(controller.getSaldosPorPersona(10, 2026))
                .filteredOn(s -> !s.persona().getNombre().startsWith("SESION"))
                .extracting(s -> s.persona().getNombre(), SaldoPersona::ingresos, SaldoPersona::gastos,
                        SaldoPersona::ahorros, SaldoPersona::saldo)
                .containsExactlyInAnyOrder(
                        tuple("ANA SALDO", 1_000_000.0, 300_000.0, 0.0, 700_000.0),
                        tuple("BETO SALDO", 500_000.0, 200_000.0, 100_000.0, 200_000.0));
    }

    private Persona persona(Hogar hogar, String nombre) {
        Persona p = new Persona();
        p.setHogar(hogar);
        p.setNombre(nombre);
        return personaRepository.save(p);
    }
}
