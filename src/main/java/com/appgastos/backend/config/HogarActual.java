package com.appgastos.backend.config;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Persona;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * El hogar de quien hace la request. Todo servicio filtra por esto: es lo que
 * impide que una cuenta lea o toque datos de otra pasando un id ajeno.
 */
public final class HogarActual {

    private HogarActual() {}

    public static Hogar get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Persona persona && persona.getHogar() != null) {
            return persona.getHogar();
        }
        // Sin hogar no hay filtro posible: mejor cortar que devolver datos de nadie (o de todos).
        throw new AccessDeniedException("No autenticado");
    }
}
