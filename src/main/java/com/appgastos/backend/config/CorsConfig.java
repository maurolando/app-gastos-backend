package com.appgastos.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    /**
     * Dominios habilitados para llamar a esta API, separados por coma.
     *
     * Estaba fijo en localhost, que sirve mientras todo corre en la misma
     * maquina pero rechaza al frontend desplegado. Se configura por entorno
     * (CORS_ORIGINS en Render) en vez de abrirlo a "*": la API expone gastos e
     * ingresos personales detras de un JWT, y un comodin deja que cualquier
     * pagina que la victima tenga abierta le hable a la API con su token.
     */
    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
