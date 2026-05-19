package com.appgastos.backend.controllers;

import com.appgastos.backend.models.Persona;
import com.appgastos.backend.services.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PersonaGraphqlController {
    private final PersonaService service;

    @QueryMapping
    public List<Persona> getAllPersonas() {
        return service.findAll();
    }

    @MutationMapping
    public Persona createPersona(@Argument String nombre) {
        return service.createPersona(nombre);
    }

    @MutationMapping
    public Persona updatePersona(@Argument Long id, @Argument String nombre, @Argument Boolean activo) {
        return service.updatePersona(id, nombre, activo);
    }

    @MutationMapping
    public Persona login(@Argument String nombre, @Argument String clave) {
        return service.login(nombre, clave);
    }

    @MutationMapping
    public Persona setClave(@Argument Long id, @Argument String clave) {
        return service.setClave(id, clave);
    }

    /** Campo virtual: indica si la persona tiene contraseña configurada */
    @SchemaMapping(typeName = "Persona", field = "tieneClave")
    public Boolean tieneClave(Persona persona) {
        return persona.getClave() != null && !persona.getClave().isBlank();
    }
}
