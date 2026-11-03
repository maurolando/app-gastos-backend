package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una cuenta de la app. Cada registro crea un hogar y todo lo demas (personas,
 * gastos, categorias, presupuestos...) cuelga de uno: un hogar no ve los datos de otro.
 */
@Data
@Entity
@Table(name = "hogares")
public class Hogar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    /** Dia (1-28) en que arranca el mes del hogar, p. ej. el de cobro. Null es 1: el mes calendario. */
    @Column(name = "dia_inicio_mes")
    private Integer diaInicioMes;

    /** Si es true, el mes se cierra solo al abrir la app pasado el dia de inicio. */
    @Column(name = "cierre_automatico")
    private Boolean cierreAutomatico;

    /** Ultimo periodo ya cerrado (dia 1 de su mes). Solo lo usa el cierre automatico. */
    @Column(name = "cerrado_hasta")
    private LocalDate cerradoHasta;
}
