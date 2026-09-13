package com.appgastos.backend.models;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.appgastos.backend.models.GrupoGasto.*;

/**
 * Reglas de distribución del ingreso que el hogar puede elegir como guía.
 *
 * - 70/20/10: pensada para ingresos bajos. Con un salario mínimo neto y dos
 *   personas, la línea de pobreza del INE (2025) ya se lleva ~67% del sueldo,
 *   así que exigir que las necesidades entren en un 50% es irreal.
 * - 50/30/20: la regla popularizada por Warren y Tyagi en "All Your Worth"
 *   (2005). Razonable desde unos dos salarios mínimos.
 * - 6 jarras (T. Harv Eker): separa educación, inversión y donaciones.
 * - Personalizada: el hogar define el porcentaje de cada grupo.
 *
 * En las reglas de tres partidas, educación cuenta como necesidad, las
 * donaciones como gasto discrecional y la inversión como ahorro.
 */
public enum PlantillaRegla {
    REGLA_70_20_10(List.of(
            new Partida("NECESIDADES", "Necesidades", 70, List.of(NECESIDADES, EDUCACION)),
            new Partida("AHORRO", "Ahorro y deudas", 20, List.of(AHORRO, INVERSION)),
            new Partida("DESEOS", "Deseos", 10, List.of(DESEOS, DONACIONES)))),

    REGLA_50_30_20(List.of(
            new Partida("NECESIDADES", "Necesidades", 50, List.of(NECESIDADES, EDUCACION)),
            new Partida("DESEOS", "Deseos", 30, List.of(DESEOS, DONACIONES)),
            new Partida("AHORRO", "Ahorro y deudas", 20, List.of(AHORRO, INVERSION)))),

    SEIS_JARRAS(List.of(
            new Partida("NECESIDADES", "Necesidades", 55, List.of(NECESIDADES)),
            new Partida("EDUCACION", "Educación", 10, List.of(EDUCACION)),
            new Partida("DESEOS", "Ocio", 10, List.of(DESEOS)),
            new Partida("AHORRO", "Ahorro a largo plazo", 10, List.of(AHORRO)),
            new Partida("INVERSION", "Libertad financiera", 10, List.of(INVERSION)),
            new Partida("DONACIONES", "Dar", 5, List.of(DONACIONES)))),

    PERSONALIZADA(List.of());

    /** Una porción del ingreso: qué porcentaje le toca y qué grupos de gasto la consumen. */
    public record Partida(String clave, String nombre, double porcentaje, List<GrupoGasto> grupos) {

        /**
         * Una partida de ahorro es una meta a alcanzar, no un techo de gasto:
         * pasarse es bueno y no tiene sentido ponerle presupuesto.
         */
        public boolean esAhorro() {
            return grupos.contains(AHORRO) || grupos.contains(INVERSION);
        }
    }

    private final List<Partida> partidasFijas;

    PlantillaRegla(List<Partida> partidasFijas) {
        this.partidasFijas = partidasFijas;
    }

    /**
     * Partidas de la plantilla. Solo la personalizada usa los porcentajes
     * guardados por el hogar: una partida por grupo, en el orden del enum.
     */
    public List<Partida> partidas(Map<GrupoGasto, Double> porcentajesPersonalizados) {
        if (this != PERSONALIZADA) {
            return partidasFijas;
        }
        return Arrays.stream(GrupoGasto.values())
                .map(g -> new Partida(g.name(), g.getNombre(),
                        porcentajesPersonalizados.getOrDefault(g, 0.0), List.of(g)))
                .toList();
    }
}
