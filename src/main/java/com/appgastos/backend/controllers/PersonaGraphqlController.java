package com.appgastos.backend.controllers;

import com.appgastos.backend.config.JwtService;
import com.appgastos.backend.dto.LoginResult;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.services.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PersonaGraphqlController {
    private final PersonaService service;
    private final JwtService jwtService;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<Persona> getAllPersonas() {
        return service.findAll();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Persona createPersona(@Argument String nombre) {
        return service.createPersona(nombre);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Persona updatePersona(@Argument Long id, @Argument String nombre, @Argument Boolean activo) {
        return service.updatePersona(id, nombre, activo);
    }

    @MutationMapping
    public LoginResult login(@Argument String nombre, @Argument String clave) {
        Persona persona = service.login(nombre, clave);
        if (persona == null) {
            return null;
        }
        return new LoginResult(jwtService.generateToken(persona), persona);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Persona setClave(@Argument Long id, @Argument String clave) {
        return service.setClave(id, clave);
    }

    /** Campo virtual: indica si la persona tiene contraseña configurada */
    @SchemaMapping(typeName = "Persona", field = "tieneClave")
    public Boolean tieneClave(Persona persona) {
        return persona.getClave() != null && !persona.getClave().isBlank();
    }
}
