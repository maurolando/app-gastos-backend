package com.appgastos.backend.services;

import com.appgastos.backend.dto.ResumenCuotas;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.CategoriaRepository;
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

/**
 * Las cuotas de una compra no estan unidas por ningun identificador: cada mes
 * es un gasto suelto que el cierre copia con cuotaActual+1. Se reconocen por
 * coincidencia de descripcion, categoria, persona y total de cuotas, que es la
 * misma regla que ya usa CierreService para no duplicar una cuota.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResumenCuotasTest {

    @Autowired private GastoService service;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    private Persona mauro;
    private Persona daiana;
    private Categoria heladera;

    @BeforeEach
    void prepararDatos() {
        mauro = nuevaPersona("Mauro");
        daiana = nuevaPersona("Daiana");
        heladera = nuevaCategoria("Heladera");
    }

    private Persona nuevaPersona(String nombre) {
        Persona p = new Persona();
        p.setNombre(nombre);
        return personaRepository.save(p);
    }

    private Categoria nuevaCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setTipo("GASTO");
        return categoriaRepository.save(c);
    }

    /** Una cuota de la heladera: 12 cuotas de 450.000 a nombre de Mauro. */
    private Gasto cuotaHeladera(int numero, boolean pagada) {
        return service.createGasto(450_000.0, heladera.getId(),
                LocalDate.of(2026, 7, 5).plusMonths(numero - 1L), "Heladera Samsung",
                mauro.getId(), "EFECTIVO", false, pagada, null, false, numero, 12);
    }

    @Test
    @DisplayName("el total a pagar es el monto de la cuota por la cantidad de cuotas")
    void totalAPagar() {
        Gasto tercera = cuotaHeladera(3, false);

        ResumenCuotas resumen = service.getResumenCuotas(tercera.getId());

        assertThat(resumen.cuotaActual()).isEqualTo(3);
        assertThat(resumen.cuotasTotales()).isEqualTo(12);
        assertThat(resumen.montoCuota()).isEqualTo(450_000.0);
        assertThat(resumen.totalAPagar()).isEqualTo(5_400_000.0);
    }

    @Test
    @DisplayName("lo pagado suma solo las cuotas marcadas como pagadas, no las que ya pasaron")
    void loPagadoCuentaSoloLasPagadas() {
        cuotaHeladera(1, true);
        cuotaHeladera(2, false);   // una cuota vieja que quedo impaga
        Gasto tercera = cuotaHeladera(3, true);

        ResumenCuotas resumen = service.getResumenCuotas(tercera.getId());

        assertThat(resumen.totalPagado()).isEqualTo(900_000.0);
        assertThat(resumen.saldoPendiente()).isEqualTo(4_500_000.0);
        assertThat(resumen.cuotas()).hasSize(3);
    }

    @Test
    @DisplayName("no mezcla las cuotas de otra persona aunque todo lo demas coincida")
    void noMezclaOtraPersona() {
        cuotaHeladera(1, true);
        Gasto deDaiana = service.createGasto(450_000.0, heladera.getId(),
                LocalDate.of(2026, 8, 5), "Heladera Samsung",
                daiana.getId(), "EFECTIVO", false, true, null, false, 1, 12);

        ResumenCuotas resumen = service.getResumenCuotas(deDaiana.getId());

        assertThat(resumen.cuotas()).hasSize(1);
        assertThat(resumen.totalPagado()).isEqualTo(450_000.0);
    }

    @Test
    @DisplayName("no mezcla otra compra de la misma categoria con distinta descripcion")
    void noMezclaOtraCompra() {
        cuotaHeladera(1, true);
        cuotaHeladera(2, true);
        Gasto microondas = service.createGasto(200_000.0, heladera.getId(),
                LocalDate.of(2026, 8, 5), "Microondas", mauro.getId(),
                "EFECTIVO", false, true, null, false, 1, 12);

        ResumenCuotas resumen = service.getResumenCuotas(microondas.getId());

        assertThat(resumen.cuotas()).hasSize(1);
        assertThat(resumen.totalPagado()).isEqualTo(200_000.0);
    }

    @Test
    @DisplayName("las cuotas vienen ordenadas por numero, no por como se cargaron")
    void cuotasOrdenadas() {
        cuotaHeladera(3, false);
        cuotaHeladera(1, true);
        Gasto segunda = cuotaHeladera(2, true);

        ResumenCuotas resumen = service.getResumenCuotas(segunda.getId());

        assertThat(resumen.cuotas()).extracting(Gasto::getCuotaActual)
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("pedir el resumen de un gasto sin cuotas avisa en vez de inventar numeros")
    void gastoSinCuotas() {
        Gasto suelto = service.createGasto(900_000.0, heladera.getId(),
                LocalDate.of(2026, 10, 8), "Compra mensual", mauro.getId(),
                "EFECTIVO", false, false, null, false, null, null);

        assertThatThrownBy(() -> service.getResumenCuotas(suelto.getId()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("cuotas");
    }
}
