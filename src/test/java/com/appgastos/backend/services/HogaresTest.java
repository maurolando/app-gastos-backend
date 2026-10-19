package com.appgastos.backend.services;

import com.appgastos.backend.SesionDePrueba;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.GastoRepository;
import com.appgastos.backend.repositories.HogarRepository;
import com.appgastos.backend.repositories.PersonaRepository;
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
 * Cada cuenta es un hogar y no puede ver ni tocar lo de otro, ni siquiera
 * mandando a mano un id ajeno. Es lo que separa los gastos de cada usuario.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HogaresTest {

    @Autowired private GastoService gastoService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private PersonaService personaService;
    @Autowired private GastoRepository gastoRepository;
    @Autowired private HogarRepository hogarRepository;
    @Autowired private PersonaRepository personaRepository;

    @Test
    @DisplayName("un hogar no ve ni modifica los gastos, categorias ni personas de otro")
    void aislamiento() {
        SesionDePrueba.iniciar(hogarRepository, personaRepository);
        Categoria comida = categoriaService.createCategoria("Comida", null, "GASTO", null);
        Persona mauro = personaService.createPersona("Mauro hogar A");
        Gasto ajeno = gastoService.createGasto(1000.0, comida.getId(), LocalDate.now(), "Super", mauro.getId(),
                "Efectivo", false, true, null, false, null, null);

        SesionDePrueba.iniciar(hogarRepository, personaRepository);

        assertThat(gastoService.findAll(null, null)).isEmpty();
        assertThat(categoriaService.findAll(null)).isEmpty();
        assertThat(personaService.findAll()).extracting(Persona::getNombre).doesNotContain("Mauro hogar A");
        assertThatThrownBy(() -> gastoService.deleteGasto(ajeno.getId()));
        assertThat(categoriaService.updateCategoria(comida.getId(), "Robada", null, null, null)).isNull();
        assertThat(personaService.setClave(mauro.getId(), "clave-robada")).isNull();
        // Una categoria ajena no se puede colgar de un gasto propio.
        Gasto propio = gastoService.createGasto(1.0, comida.getId(), LocalDate.now(), null, null,
                "Efectivo", false, true, null, false, null, null);
        assertThat(propio.getCategoria()).isNull();

        assertThat(gastoRepository.findById(ajeno.getId())).isPresent();
    }

    @Test
    @DisplayName("registrarse crea un hogar propio y deja entrar con esa clave")
    void registrar() {
        Persona nueva = personaService.registrar("  Lucia  ", "secreta1");

        assertThat(nueva.getNombre()).isEqualTo("Lucia");
        assertThat(nueva.getHogar()).isNotNull();
        assertThat(personaService.login("lucia", "secreta1")).isNotNull();
        assertThat(personaService.login("lucia", "otra-clave")).isNull();
    }

    @Test
    @DisplayName("no se puede registrar un nombre repetido ni una clave corta")
    void registrarValida() {
        personaService.registrar("Repetido", "secreta1");

        assertThatThrownBy(() -> personaService.registrar("REPETIDO", "secreta2"))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> personaService.registrar("Otro", "123"))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    @DisplayName("una persona agregada al hogar no entra hasta que le pongan clave")
    void personaNuevaSinClave() {
        SesionDePrueba.iniciar(hogarRepository, personaRepository);
        personaService.createPersona("Invitado");

        assertThat(personaService.login("Invitado", "123")).isNull();
    }
}
