package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "gastos")
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    private LocalDate date;
    private String description;
    private String formaPago;
    private Boolean recurrent;
    private Boolean pagado;
    private LocalDate fechaVencimiento;
    private LocalDate fechaPago;
    private Boolean esCompartido;
    private Integer cuotaActual;
    private Integer cuotasTotales;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = true)
    private Persona persona;

    @OneToMany(mappedBy = "gasto", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PagoCompartido> pagosCompartidos;
}
