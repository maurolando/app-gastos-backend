package com.appgastos.backend;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.HogarRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Deja logueada a una persona de un hogar nuevo, como hace JwtAuthFilter con un
 * token valido. Los servicios filtran todo por ese hogar, asi que lo que un test
 * guarde directo en un repositorio tiene que llevar el hogar que devuelve esto.
 */
public final class SesionDePrueba {

    private SesionDePrueba() {}

    public static Hogar iniciar(HogarRepository hogares, PersonaRepository personas) {
        Hogar hogar = hogares.save(new Hogar());
        Persona persona = new Persona();
        persona.setNombre("Sesion de prueba " + hogar.getId());
        persona.setHogar(hogar);
        personas.save(persona);
        loguear(persona);
        return hogar;
    }

    /** Deja logueada a una persona que ya existe, con su propio hogar. */
    public static void loguear(Persona persona) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                persona, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
