package com.appgastos.backend.config;

import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PersonaRepository personaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // Asigna la contraseña "123" a todas las personas que aún no tienen clave
        for (Persona persona : personaRepository.findAll()) {
            if (persona.getClave() == null || persona.getClave().isBlank()) {
                persona.setClave(passwordEncoder.encode("123"));
                personaRepository.save(persona);
                System.out.println("✅ Contraseña por defecto asignada a: " + persona.getNombre());
            }
        }
    }
}
