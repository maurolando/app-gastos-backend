package com.appgastos.backend.services;

import com.appgastos.backend.SesionDePrueba;
import com.appgastos.backend.models.Hogar;
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
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El mes del hogar arranca en su dia de inicio (p. ej. el de cobro) y se puede cerrar solo. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PeriodoServiceTest {

    @Autowired private PeriodoService periodoService;
    @Autowired private CierreService cierreService;
    @Autowired private GastoService gastoService;
    @Autowired private IngresoService ingresoService;
    @Autowired private HogarRepository hogarRepository;
    @Autowired private PersonaRepository personaRepository;

    private Hogar hogar;

    @BeforeEach
    void setUp() {
        hogar = SesionDePrueba.iniciar(hogarRepository, personaRepository);
    }

    @Test
    @DisplayName("antes del dia de inicio la fecha cuenta en el mes anterior")
    void periodoDe() {
        assertThat(PeriodoService.periodoDe(LocalDate.of(2026, 11, 3), 4)).isEqualTo(YearMonth.of(2026, 10));
        assertThat(PeriodoService.periodoDe(LocalDate.of(2026, 11, 4), 4)).isEqualTo(YearMonth.of(2026, 11));
        assertThat(PeriodoService.periodoDe(LocalDate.of(2026, 1, 2), 4)).isEqualTo(YearMonth.of(2025, 12));
        assertThat(PeriodoService.periodoDe(LocalDate.of(2026, 11, 1), 1)).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("con dia 4, octubre va del 4/10 al 3/11")
    void filtraPorPeriodo() {
        periodoService.configurar(4, false, LocalDate.of(2026, 11, 3));
        gasto(LocalDate.of(2026, 10, 3));  // septiembre
        gasto(LocalDate.of(2026, 10, 4));  // octubre
        gasto(LocalDate.of(2026, 11, 3));  // octubre
        gasto(LocalDate.of(2026, 11, 4));  // noviembre

        assertThat(gastoService.findAll(10, 2026)).hasSize(2);
        assertThat(gastoService.findAll(9, 2026)).hasSize(1);
    }

    @Test
    @DisplayName("el cierre automatico arranca desde hoy y despues cierra cada periodo una sola vez")
    void cierreAutomatico() {
        periodoService.configurar(4, true, LocalDate.of(2026, 11, 3)); // periodo actual: octubre
        ingresoService.createIngreso(4_000_000.0, LocalDate.of(2026, 10, 4), null, null, true);

        assertThat(cierreService.cerrarPendientes(LocalDate.of(2026, 11, 3))).isZero();
        assertThat(cierreService.cerrarPendientes(LocalDate.of(2026, 11, 4))).isEqualTo(1);
        assertThat(cierreService.cerrarPendientes(LocalDate.of(2026, 11, 20))).isZero();
        assertThat(ingresoService.getAllIngresos(11, 2026)).hasSize(1);

        // Sin abrir la app dos meses: cierra noviembre y diciembre en orden.
        assertThat(cierreService.cerrarPendientes(LocalDate.of(2027, 1, 10))).isEqualTo(2);
        assertThat(ingresoService.getAllIngresos(1, 2027)).hasSize(1);
    }

    @Test
    @DisplayName("apagado no cierra nada")
    void apagadoNoCierra() {
        periodoService.configurar(4, false, LocalDate.of(2026, 11, 3));
        assertThat(cierreService.cerrarPendientes(LocalDate.of(2027, 3, 10))).isZero();
    }

    @Test
    @DisplayName("el dia de inicio va de 1 a 28")
    void validaDia() {
        assertThatThrownBy(() -> periodoService.configurar(29, false, LocalDate.of(2026, 11, 3)))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> periodoService.configurar(0, false, LocalDate.of(2026, 11, 3)))
                .isInstanceOf(ValidacionException.class);
    }

    private void gasto(LocalDate fecha) {
        gastoService.createGasto(10_000.0, null, fecha, "x", null, "Efectivo", false, true, null, false, null, null);
    }
}
