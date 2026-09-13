package com.appgastos.backend.models;

/**
 * Para qué se usa el dinero de una categoría de gasto dentro de la guía de
 * distribución del ingreso.
 *
 * Son los seis destinos del método de las 6 jarras, que es el más detallado.
 * Las reglas de tres partidas (50/30/20 y 70/20/10) los agrupan, así que
 * clasificar una categoría una sola vez sirve para cualquier plantilla.
 */
public enum GrupoGasto {
    NECESIDADES("Necesidades"),
    DESEOS("Deseos"),
    EDUCACION("Educación"),
    AHORRO("Ahorro"),
    INVERSION("Inversión"),
    DONACIONES("Donaciones");

    private final String nombre;

    GrupoGasto(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
