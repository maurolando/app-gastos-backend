package com.appgastos.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La conexion a la base y la firma de los tokens tienen que venir si o si del
 * entorno. Un valor por defecto en application.properties no solo filtra a que
 * proyecto apunta la app: hace que, cuando falta la variable en el servidor,
 * el arranque no falle diciendo "falta DB_USERNAME" sino intentando conectarse
 * a ese proyecto, y el error que sale apunta a cualquier otro lado.
 *
 * Paso de verdad: el 2026-09-13 tres deploys de Render murieron con
 * "FATAL: (ENOTFOUND) tenant/user postgres.ebxjhioukmzpcpgnehap not found",
 * que es el usuario que estaba escrito como default aca.
 */
class CredencialesSinDefaultTest {

    /** Propiedades cuyo valor es un secreto o depende del entorno. */
    private static final List<String> SIN_DEFAULT = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "jwt.secret");

    /** ${VAR} sin default; ${VAR:algo} con default. Capturamos el ":algo". */
    private static final Pattern CON_DEFAULT = Pattern.compile("\\$\\{[A-Z_0-9]+:(.*)}\\s*$");

    private static String leerPropiedad(String clave) throws IOException {
        Path properties = Path.of("src/main/resources/application.properties");
        for (String linea : Files.readAllLines(properties, StandardCharsets.UTF_8)) {
            String limpia = linea.strip();
            if (limpia.startsWith(clave + "=")) {
                return limpia.substring(clave.length() + 1);
            }
        }
        throw new AssertionError("No esta definida la propiedad " + clave);
    }

    @Test
    @DisplayName("ninguna credencial de la base ni la clave de firma tiene valor por defecto")
    void credencialesSinValorPorDefecto() throws IOException {
        for (String clave : SIN_DEFAULT) {
            String valor = leerPropiedad(clave);
            Matcher conDefault = CON_DEFAULT.matcher(valor);

            assertThat(conDefault.find())
                    .withFailMessage(
                            "%s tiene un valor por defecto: %s. Si falta la variable de entorno, "
                                    + "la app arranca contra ese valor en vez de fallar diciendo cual falta.",
                            clave, valor)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("la configuracion no menciona ningun proyecto concreto de Supabase")
    void sinRastrosDelProyectoDeSupabase() throws IOException {
        String properties = Files.readString(
                Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);

        assertThat(properties)
                .withFailMessage("application.properties no debe nombrar el host ni el proyecto de Supabase")
                .doesNotContain("supabase.com")
                .doesNotContain("postgres.");
    }
}
