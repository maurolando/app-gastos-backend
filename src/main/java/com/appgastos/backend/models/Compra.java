package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private Double precio;
    private String lugar;
    private Boolean comprado;
    private LocalDate fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hogar_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Hogar hogar;
}
