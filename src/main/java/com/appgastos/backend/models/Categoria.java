package com.appgastos.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categorias")
@Data
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    private String icono; // Nombre del Material Icon (ej: 'restaurant', 'shopping_cart')
    private String tipo;  // 'GASTO' o 'INGRESO'
}
