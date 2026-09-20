package com.appgastos.backend.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Comprueba al arrancar que esten las variables de entorno sin las que la app no
 * puede funcionar.
 *
 * Existe porque el fallo por defecto no nombra la variable: con
 * {@code spring.datasource.url=${DB_URL}} sin definir, Spring deja el
 * placeholder sin resolver y el arranque muere con
 * {@code 'url' must start with "jdbc"}, que manda a revisar la cadena de
 * conexion en vez de la configuracion del servidor.
 */
public final class VariablesRequeridas {

    /**
     * Una contrasena vacia es legitima (H2 en local), asi que se exige que la
     * variable este definida, no que tenga contenido.
     */
    private static final List<String> OBLIGATORIAS =
            List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET");

    private VariablesRequeridas() {
    }

    public static void verificar(UnaryOperator<String> entorno) {
        List<String> faltantes = new ArrayList<>();
        for (String variable : OBLIGATORIAS) {
            if (entorno.apply(variable) == null) {
                faltantes.add(variable);
            }
        }

        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Faltan variables de entorno obligatorias: " + String.join(", ", faltantes)
                            + ". En Render se cargan en Environment; en local, ver la skill "
                            + "arrancar-app-gastos.");
        }
    }
}
