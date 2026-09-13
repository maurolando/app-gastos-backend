package com.appgastos.backend.services;

import com.appgastos.backend.models.Categoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.appgastos.backend.models.GrupoGasto.AHORRO;
import static com.appgastos.backend.models.GrupoGasto.NECESIDADES;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoriaServiceTest {

    @Autowired private CategoriaService service;

    @Test
    @DisplayName("una categoria de ingreso nunca queda clasificada en un grupo de gasto")
    void ingresoNoTieneGrupo() {
        Categoria sueldo = service.createCategoria("Sueldo", "work", "INGRESO", AHORRO);

        assertThat(sueldo.getGrupo()).isNull();
    }

    @Test
    @DisplayName("editar reemplaza el grupo, asi una categoria puede volver a sin clasificar")
    void editarReemplazaElGrupo() {
        Categoria alquiler = service.createCategoria("Alquiler", "home", "GASTO", NECESIDADES);
        assertThat(alquiler.getGrupo()).isEqualTo(NECESIDADES);

        Categoria editada = service.updateCategoria(alquiler.getId(), null, null, null, null);

        assertThat(editada.getGrupo()).isNull();
        assertThat(editada.getNombre()).isEqualTo("Alquiler");
    }
}
