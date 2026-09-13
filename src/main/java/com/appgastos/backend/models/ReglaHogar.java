package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.EnumMap;
import java.util.Map;

/**
 * La regla de distribución que eligió el hogar. Hay como mucho una fila: todo
 * en la app es compartido y no hay datos por persona. Si no existe, la guía
 * está desactivada y solo se muestra como sugerencia.
 */
@Entity
@Table(name = "regla_hogar")
@Data
public class ReglaHogar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlantillaRegla plantilla;

    /**
     * Porcentajes de la plantilla personalizada. Se conservan aunque el hogar
     * pase a una plantilla fija, para no perderlos si vuelve a la personalizada.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "regla_hogar_porcentajes", joinColumns = @JoinColumn(name = "regla_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "grupo")
    @Column(name = "porcentaje")
    private Map<GrupoGasto, Double> porcentajes = new EnumMap<>(GrupoGasto.class);
}
