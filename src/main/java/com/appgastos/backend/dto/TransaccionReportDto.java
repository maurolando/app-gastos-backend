package com.appgastos.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionReportDto {
    private String fecha;
    private String tipo;
    private String descripcion;
    private String persona;
    private Double monto;
    private String formaPago;
    private String estado;
}
