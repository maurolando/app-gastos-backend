package com.appgastos.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final String ORIGEN_LOCAL = "http://localhost:4200";

    /**
     * Dominios habilitados para llamar a esta API, separados por coma.
     *
     * Estaba fijo en localhost, que sirve mientras todo corre en la misma
     * maquina pero rechaza al frontend desplegado. Se configura por entorno
     * (CORS_ORIGINS en Render) en vez de abrirlo a "*": la API expone gastos e
     * ingresos personales detras de un JWT, y un comodin deja que cualquier
     * pagina que la victima tenga abierta le hable a la API con su token.
     */
    @Value("${cors.allowed-origins:}")
    private String origenesCrudos;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        List<String> origenes = parsearOrigenes(origenesCrudos);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(origenes.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    /**
     * Normaliza la lista de origenes.
     *
     * Un panel de hosting hace muy facil equivocarse de tres formas que dejan
     * la API rechazando todo con un 403 que en el navegador parece un problema
     * de red: dejar la variable vacia, pegar el dominio con espacios alrededor,
     * o copiarlo de la barra del navegador con la barra final. El header Origin
     * nunca trae barra final, asi que un solo caracter de mas y no matchea nada.
     */
    static List<String> parsearOrigenes(String crudos) {
        List<String> origenes = Arrays.stream(crudos == null ? new String[0] : crudos.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .distinct()
                .toList();

        if (origenes.isEmpty()) {
            // Sin esto la lista queda sin elementos y no entra nadie, ni siquiera
            // el front corriendo en local, que es el caso mas dificil de diagnosticar.
            System.err.println("CORS_ORIGINS vacio o sin definir: se permite solo " + ORIGEN_LOCAL);
            return List.of(ORIGEN_LOCAL);
        }

        System.out.println("CORS habilitado para: " + String.join(", ", origenes));
        return origenes;
    }
}
