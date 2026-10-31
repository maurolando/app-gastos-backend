package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Código para sumarse a un hogar al crear la cuenta (issue #35 del front). Es de un
 * solo uso y vence, porque cualquiera que lo tenga entra a ver y cargar los datos del hogar.
 */
@Data
@Entity
@Table(name = "invitaciones")
public class Invitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hogar_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Hogar hogar;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usada_en")
    private LocalDateTime usadaEn;

    // Si dos personas usan el mismo código a la vez, la segunda falla al guardar.
    @Version
    private Long version;
}
