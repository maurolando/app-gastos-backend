package com.appgastos.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sin esta verificacion, olvidarse una variable en el servidor no falla diciendo
 * cual falta: Spring deja el placeholder sin resolver y Hikari termina tirando
 * 'url' must start with "jdbc", que no nombra la variable.
 */
class VariablesRequeridasTest {

    private static final Map<String, String> COMPLETO = Map.of(
            "DB_URL", "jdbc:postgresql://host:5432/postgres",
            "DB_USERNAME", "usuario",
            "DB_PASSWORD", "secreto",
            "JWT_SECRET", "una-clave-de-firma-de-al-menos-32-caracteres");

    @Test
    @DisplayName("el error nombra la variable que falta")
    void nombraLaQueFalta() {
        Map<String, String> sinUrl = new java.util.HashMap<>(COMPLETO);
        sinUrl.remove("DB_URL");

        assertThatThrownBy(() -> VariablesRequeridas.verificar(sinUrl::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL");
    }

    @Test
    @DisplayName("el error nombra todas las que faltan, no solo la primera")
    void nombraTodasLasQueFaltan() {
        Map<String, String> vacio = Map.of();

        assertThatThrownBy(() -> VariablesRequeridas.verificar(vacio::get))
                .hasMessageContaining("DB_URL")
                .hasMessageContaining("DB_USERNAME")
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("una contrasena vacia es un valor valido, no una variable faltante")
    void contrasenaVaciaEsValida() {
        Map<String, String> conPasswordVacia = new java.util.HashMap<>(COMPLETO);
        conPasswordVacia.put("DB_PASSWORD", "");

        assertThatCode(() -> VariablesRequeridas.verificar(conPasswordVacia::get))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("con todas definidas no interrumpe el arranque")
    void conTodasNoFalla() {
        assertThatCode(() -> VariablesRequeridas.verificar(COMPLETO::get))
                .doesNotThrowAnyException();
    }
}
