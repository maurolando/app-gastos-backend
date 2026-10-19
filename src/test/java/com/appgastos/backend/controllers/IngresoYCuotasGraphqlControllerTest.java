package com.appgastos.backend.controllers;

import com.appgastos.backend.SesionDePrueba;
import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.repositories.HogarRepository;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Ingreso;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.GastoRepository;
import com.appgastos.backend.repositories.IngresoRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

/**
 * Los tests de servicio no pasan por GraphQL. Estos cubren lo que solo se rompe
 * en el cableado: que el schema y el controller coincidan, que el ID llegue como
 * Long y que un error de validación le llegue al cliente con su mensaje.
 */
@SpringBootTest
@ActiveProfiles("test")
class IngresoYCuotasGraphqlControllerTest {

    private static final String DESCRIPCION = "Heladera de prueba GraphQL";

    @Autowired private ExecutionGraphQlService graphQlService;
    @Autowired private IngresoRepository ingresoRepository;
    @Autowired private GastoRepository gastoRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private HogarRepository hogarRepository;

    private Hogar hogar;

    private GraphQlTester tester;
    private Persona persona;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        hogar = SesionDePrueba.iniciar(hogarRepository, personaRepository);
        tester = ExecutionGraphQlServiceTester.create(graphQlService);
        limpiar();

        Persona p = new Persona();

        p.setHogar(hogar);
        p.setNombre("Persona de prueba GraphQL");
        persona = personaRepository.save(p);

        Categoria c = new Categoria();

        c.setHogar(hogar);
        c.setNombre("Categoria de prueba GraphQL");
        c.setTipo("GASTO");
        categoria = categoriaRepository.save(c);
    }

    // Sin transacción de test, lo que se guarda por GraphQL queda en la base compartida.
    @AfterEach
    void limpiar() {
        gastoRepository.findAll().stream()
                .filter(g -> DESCRIPCION.equals(g.getDescription()))
                .forEach(gastoRepository::delete);
        ingresoRepository.findAll().stream()
                .filter(i -> i.getCategoria() != null
                        && "Categoria de prueba GraphQL".equals(i.getCategoria().getNombre()))
                .forEach(ingresoRepository::delete);
    }

    private Ingreso nuevoIngreso() {
        Ingreso i = new Ingreso();
        i.setHogar(hogar);
        i.setMonto(5_000_000.0);
        i.setFecha(LocalDate.of(2026, 10, 3));
        i.setCategoria(categoria);
        i.setPersona(persona);
        i.setRecurrent(false);
        return ingresoRepository.save(i);
    }

    private Gasto nuevaCuota(int numero, boolean pagada) {
        Gasto g = new Gasto();
        g.setHogar(hogar);
        g.setAmount(450_000.0);
        g.setDescription(DESCRIPCION);
        g.setDate(LocalDate.of(2026, 8, 5).plusMonths(numero - 1L));
        g.setCategoria(categoria);
        g.setPersona(persona);
        g.setPagado(pagada);
        g.setCuotaActual(numero);
        g.setCuotasTotales(12);
        return gastoRepository.save(g);
    }

    @Test
    @DisplayName("updateIngreso edita por GraphQL y devuelve el ingreso actualizado")
    void updateIngresoPorGraphql() {
        Ingreso ingreso = nuevoIngreso();

        tester.document("""
                mutation($id: ID!) {
                  updateIngreso(id: $id, monto: 6000000) { id monto }
                }
                """)
                .variable("id", ingreso.getId())
                .execute()
                .path("updateIngreso.monto").entity(Double.class).isEqualTo(6_000_000.0);
    }

    @Test
    @DisplayName("deleteIngreso borra por GraphQL")
    void deleteIngresoPorGraphql() {
        Ingreso ingreso = nuevoIngreso();

        tester.document("""
                mutation($id: ID!) { deleteIngreso(id: $id) }
                """)
                .variable("id", ingreso.getId())
                .execute()
                .path("deleteIngreso").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    @DisplayName("getResumenCuotas devuelve los totales de la compra")
    void resumenCuotasPorGraphql() {
        nuevaCuota(1, true);
        nuevaCuota(2, true);
        Gasto tercera = nuevaCuota(3, false);

        tester.document("""
                query($id: ID!) {
                  getResumenCuotas(gastoId: $id) {
                    cuotaActual cuotasTotales montoCuota
                    totalAPagar totalPagado saldoPendiente
                    cuotas { cuotaActual pagado }
                  }
                }
                """)
                .variable("id", tercera.getId())
                .execute()
                .path("getResumenCuotas.totalAPagar").entity(Double.class).isEqualTo(5_400_000.0)
                .path("getResumenCuotas.totalPagado").entity(Double.class).isEqualTo(900_000.0)
                .path("getResumenCuotas.saldoPendiente").entity(Double.class).isEqualTo(4_500_000.0)
                .path("getResumenCuotas.cuotas").entityList(Object.class).hasSize(3);
    }

    @Test
    @DisplayName("pedir el resumen de un gasto sin cuotas llega al cliente con su mensaje")
    void resumenDeGastoSinCuotas() {
        Gasto suelto = new Gasto();
        suelto.setHogar(hogar);
        suelto.setAmount(900_000.0);
        suelto.setDescription(DESCRIPCION);
        suelto.setDate(LocalDate.of(2026, 10, 8));
        suelto.setCategoria(categoria);
        suelto.setPersona(persona);
        suelto = gastoRepository.save(suelto);

        tester.document("""
                query($id: ID!) { getResumenCuotas(gastoId: $id) { totalAPagar } }
                """)
                .variable("id", suelto.getId())
                .execute()
                .errors()
                .satisfy(errores -> {
                    assert !errores.isEmpty() : "se esperaba un error de validacion";
                    assert errores.get(0).getMessage().contains("cuotas")
                            : "el mensaje deberia nombrar las cuotas: " + errores.get(0).getMessage();
                });
    }
}
