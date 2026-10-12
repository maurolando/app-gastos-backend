package com.appgastos.backend.services;

import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Ingreso;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.IngresoRepository;
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
class IngresoServiceTest {

    @Autowired private IngresoService service;
    @Autowired private IngresoRepository ingresoRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    private Persona mauro;
    private Persona daiana;
    private Categoria sueldo;
    private Categoria alquilerCochera;

    @BeforeEach
    void prepararDatos() {
        mauro = nuevaPersona("Mauro");
        daiana = nuevaPersona("Daiana");
        sueldo = nuevaCategoria("Sueldo");
        alquilerCochera = nuevaCategoria("Alquiler cochera");
    }

    private Persona nuevaPersona(String nombre) {
        Persona p = new Persona();
        p.setNombre(nombre);
        return personaRepository.save(p);
    }

    private Categoria nuevaCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setTipo("INGRESO");
        return categoriaRepository.save(c);
    }

    @Test
    @DisplayName("editar un ingreso cambia solo los campos que se mandan")
    void edicionParcial() {
        Ingreso original = service.createIngreso(
                5_000_000.0, LocalDate.of(2026, 10, 3), sueldo.getId(), mauro.getId(), false);

        Ingreso editado = service.updateIngreso(
                original.getId(), 6_000_000.0, null, null, null, null);

        assertThat(editado.getMonto()).isEqualTo(6_000_000.0);
        assertThat(editado.getFecha()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(editado.getCategoria().getId()).isEqualTo(sueldo.getId());
        assertThat(editado.getPersona().getId()).isEqualTo(mauro.getId());
        assertThat(editado.getRecurrent()).isFalse();
    }

    @Test
    @DisplayName("editar un ingreso permite cambiar la categoria y la persona")
    void edicionDeCategoriaYPersona() {
        Ingreso original = service.createIngreso(
                5_000_000.0, LocalDate.of(2026, 10, 3), sueldo.getId(), mauro.getId(), false);

        Ingreso editado = service.updateIngreso(original.getId(), null, LocalDate.of(2026, 10, 20),
                alquilerCochera.getId(), daiana.getId(), true);

        assertThat(editado.getMonto()).isEqualTo(5_000_000.0);
        assertThat(editado.getFecha()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(editado.getCategoria().getId()).isEqualTo(alquilerCochera.getId());
        assertThat(editado.getPersona().getId()).isEqualTo(daiana.getId());
        assertThat(editado.getRecurrent()).isTrue();
    }

    @Test
    @DisplayName("editar un ingreso que no existe avisa con un mensaje mostrable")
    void edicionDeInexistente() {
        assertThatThrownBy(() -> service.updateIngreso(999_999L, 1.0, null, null, null, null))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no");
    }

    @Test
    @DisplayName("borrar un ingreso lo saca de la base")
    void borrado() {
        Ingreso ingreso = service.createIngreso(
                5_000_000.0, LocalDate.of(2026, 10, 3), sueldo.getId(), mauro.getId(), false);

        boolean borrado = service.deleteIngreso(ingreso.getId());

        assertThat(borrado).isTrue();
        assertThat(ingresoRepository.findById(ingreso.getId())).isEmpty();
    }

    @Test
    @DisplayName("borrar un ingreso que no existe avisa con un mensaje mostrable")
    void borradoDeInexistente() {
        assertThatThrownBy(() -> service.deleteIngreso(999_999L))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no");
    }
}
