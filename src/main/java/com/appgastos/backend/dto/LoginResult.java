package com.appgastos.backend.dto;

import com.appgastos.backend.models.Persona;

public record LoginResult(String token, Persona persona) {
}
