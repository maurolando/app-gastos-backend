package com.appgastos.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un CORS mal configurado se manifiesta como un 403 que en el navegador parece
 * un problema de red, y se diagnostica lento. Estos casos son los que ya
 * pasaron o estuvieron a un caracter de pasar al desplegar.
 */
class CorsConfigTest {

    private static final String LOCAL = "http://localhost:4200";
    private static final String VERCEL = "https://app-gastos-frontend-rosy.vercel.app";

    @Test
    @DisplayName("una variable vacia cae al origen local en vez de no permitir ninguno")
    void vaciaCaeAlLocal() {
        // Render crea la variable vacia si el blueprint la declara con sync:false
        // y no se completa. Sin este caso la lista queda sin elementos y la API
        // rechaza a todo el mundo, incluido el front corriendo en local.
        assertThat(CorsConfig.parsearOrigenes("")).containsExactly(LOCAL);
        assertThat(CorsConfig.parsearOrigenes("   ")).containsExactly(LOCAL);
        assertThat(CorsConfig.parsearOrigenes(null)).containsExactly(LOCAL);
    }

    @Test
    @DisplayName("descarta la barra final, que el header Origin nunca trae")
    void descartaBarraFinal() {
        assertThat(CorsConfig.parsearOrigenes(VERCEL + "/")).containsExactly(VERCEL);
    }

    @Test
    @DisplayName("tolera espacios alrededor de cada valor")
    void toleraEspacios() {
        assertThat(CorsConfig.parsearOrigenes("  " + VERCEL + " ,  " + LOCAL + "  "))
                .containsExactly(VERCEL, LOCAL);
    }

    @Test
    @DisplayName("ignora los elementos vacios de una lista mal armada")
    void ignoraElementosVacios() {
        assertThat(CorsConfig.parsearOrigenes(VERCEL + ",,")).containsExactly(VERCEL);
        assertThat(CorsConfig.parsearOrigenes("," + LOCAL)).containsExactly(LOCAL);
    }

    @Test
    @DisplayName("no repite un origen cargado dos veces")
    void noRepite() {
        assertThat(CorsConfig.parsearOrigenes(VERCEL + "," + VERCEL + "/")).containsExactly(VERCEL);
    }

    @Test
    @DisplayName("respeta varios origenes bien cargados")
    void variosOrigenes() {
        assertThat(CorsConfig.parsearOrigenes(VERCEL + "," + LOCAL))
                .containsExactly(VERCEL, LOCAL);
    }
}
