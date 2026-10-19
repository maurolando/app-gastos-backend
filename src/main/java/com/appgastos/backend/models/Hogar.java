package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

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
}
