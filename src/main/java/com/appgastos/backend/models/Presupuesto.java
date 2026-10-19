package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "presupuestos")
@Data
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer anio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hogar_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Hogar hogar;
}
