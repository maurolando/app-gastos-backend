package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Corrección manual del balance de un mes (#50 del front). Positivo suma, negativo
 * resta. Solo cuenta en el balance: no es ingreso ni gasto, así que no aparece en
 * reportes ni en la guía de distribución.
 */
@Data
@Entity
@Table(name = "ajustes_balance")
public class AjusteBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private LocalDate fecha;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hogar_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Hogar hogar;
}
