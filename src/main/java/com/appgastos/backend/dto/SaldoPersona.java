package com.appgastos.backend.dto;

import com.appgastos.backend.models.Persona;

/**
 * Lo que le queda a una persona en el mes: ingresos - gastos pagados - ahorros.
 * En un gasto compartido cuenta solo lo que aportó esa persona.
 */
public record SaldoPersona(Persona persona, double ingresos, double gastos, double ahorros, double saldo) {
}
