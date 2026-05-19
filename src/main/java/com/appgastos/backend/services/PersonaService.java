package com.appgastos.backend.services;

import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {
    private final PersonaRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Persona> findAll() {
        return repository.findAll();
    }

    public Persona save(Persona persona) {
        return repository.save(persona);
    }

    public Persona findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Persona updatePersona(Long id, String nombre, Boolean activo) {
        return repository.findById(id).map(p -> {
            if (nombre != null) p.setNombre(nombre);
            if (activo != null) p.setActivo(activo);
            return repository.save(p);
        }).orElse(null);
    }

    /** Autentica una persona. Retorna la persona si es correcto, null si falla. */
    public Persona login(String nombre, String clave) {
        return repository.findAll().stream()
            .filter(p -> p.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .filter(p -> {
                if (p.getClave() == null) return false;
                return passwordEncoder.matches(clave, p.getClave());
            })
            .orElse(null);
    }

    /** Establece o cambia la contraseña de una persona (guarda hash). */
    public Persona setClave(Long id, String nuevaClave) {
        return repository.findById(id).map(p -> {
            p.setClave(passwordEncoder.encode(nuevaClave));
            return repository.save(p);
        }).orElse(null);
    }

    public Persona createPersona(String nombre) {
        Persona p = new Persona();
        p.setNombre(nombre);
        p.setClave(passwordEncoder.encode("123")); // contraseña por defecto
        return repository.save(p);
    }
}
