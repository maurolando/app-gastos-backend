package com.appgastos.backend.controllers;

import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.ReglaHogarRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * Los tests del servicio no pasan por GraphQL. Estos verifican lo que solo se
 * rompe en el cableado: enums e inputs del schema que llegan como records, y
 * que un error de validación le llegue al cliente con su mensaje.
 */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser
class ReglaPresupuestoGraphqlControllerTest {

    private static final String CATEGORIA_DE_PRUEBA = "Categoria de prueba GraphQL";

    @Autowired private ExecutionGraphQlService graphQlService;
    @Autowired private ReglaHogarRepository reglaRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    private GraphQlTester tester;

    @BeforeEach
    void setUp() {
        tester = ExecutionGraphQlServiceTester.create(graphQlService);
        reglaRepository.deleteAll();
    }

    // Sin transacción de test lo que se guarda por GraphQL queda en la base compartida.
    @AfterEach
    void limpiar() {
        reglaRepository.deleteAll();
        categoriaRepository.findAll().stream()
                .filter(c -> CATEGORIA_DE_PRUEBA.equals(c.getNombre()))
                .forEach(categoriaRepository::delete);
    }

    @Test
    @DisplayName("guarda una regla personalizada y la usa para la distribucion del mes")
    void guardaPersonalizada() {
        tester.document("""
                mutation {
                  guardarReglaPresupuesto(plantilla: PERSONALIZADA, porcentajes: [
                    {grupo: NECESIDADES, porcentaje: 60},
                    {grupo: DESEOS, porcentaje: 20},
                    {grupo: AHORRO, porcentaje: 20}
                  ]) { plantilla porcentajes { grupo porcentaje } }
                }""")
                .execute()
                .path("guardarReglaPresupuesto.plantilla").entity(String.class).isEqualTo("PERSONALIZADA")
                .path("guardarReglaPresupuesto.porcentajes[*].grupo").entityList(String.class)
                .containsExactly("NECESIDADES", "DESEOS", "AHORRO");

        tester.document("{ getDistribucionRegla(mes: 5, anio: 2026) { activa plantilla partidas { clave esAhorro } } }")
                .execute()
                .path("getDistribucionRegla.activa").entity(Boolean.class).isEqualTo(true)
                .path("getDistribucionRegla.partidas").entityList(Object.class).hasSize(6);
    }

    @Test
    @DisplayName("un error de validacion llega al cliente con su mensaje")
    void errorDeValidacionConMensaje() {
        tester.document("""
                mutation {
                  guardarReglaPresupuesto(plantilla: PERSONALIZADA, porcentajes: [{grupo: NECESIDADES, porcentaje: 90}]) {
                    plantilla
                  }
                }""")
                .execute()
                .errors()
                .expect(e -> e.getMessage() != null && e.getMessage().contains("sumar 100%"))
                .verify();
    }

    @Test
    @DisplayName("crea una categoria de gasto con su grupo")
    void creaCategoriaConGrupo() {
        tester.document("mutation { createCategoria(nombre: \"" + CATEGORIA_DE_PRUEBA
                        + "\", icono: \"home\", tipo: \"GASTO\", grupo: NECESIDADES) { grupo } }")
                .execute()
                .path("createCategoria.grupo").entity(String.class).isEqualTo("NECESIDADES");
    }
}
