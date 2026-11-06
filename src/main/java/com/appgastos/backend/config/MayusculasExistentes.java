package com.appgastos.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Las entidades ya guardan en MAYÚSCULAS lo nuevo; esto pasa los datos de antes.
 * Solo toca filas con minúsculas, así que desde el segundo arranque no hace nada.
 */
@Component
@RequiredArgsConstructor
public class MayusculasExistentes {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void convertir() {
        jdbc.update("UPDATE personas SET nombre = UPPER(nombre) WHERE nombre <> UPPER(nombre)");
        jdbc.update("UPDATE categorias SET nombre = UPPER(nombre) WHERE nombre <> UPPER(nombre)");
        jdbc.update("UPDATE gastos SET description = UPPER(description) WHERE description <> UPPER(description)");
    }
}
