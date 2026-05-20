package com.appgastos.backend.services;

import com.appgastos.backend.models.Ahorro;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.AhorroRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AhorroService {

    private final AhorroRepository ahorroRepository;
    private final PersonaRepository personaRepository;

    public List<Ahorro> getAllAhorros(Integer mes, Integer anio) {
        List<Ahorro> all = ahorroRepository.findAll();
        if (mes == null && anio == null) return all;
        
        return all.stream()
                .filter(a -> {
                    if (a.getFecha() == null) return false;
                    boolean match = true;
                    if (mes != null) match &= a.getFecha().getMonthValue() == mes;
                    if (anio != null) match &= a.getFecha().getYear() == anio;
                    return match;
                })
                .toList();
    }

    public Ahorro createAhorro(Double monto, LocalDate fecha, Long personaId, String descripcion) {
        Persona persona = personaId != null ? personaRepository.findById(personaId).orElse(null) : null;

        Ahorro ahorro = new Ahorro();
        ahorro.setMonto(monto);
        ahorro.setFecha(fecha != null ? fecha : LocalDate.now());
        ahorro.setPersona(persona);
        ahorro.setDescripcion(descripcion);

        return ahorroRepository.save(ahorro);
    }

    public void deleteAhorro(Long id) {
        ahorroRepository.deleteById(id);
    }

    public Double getTotalAhorros(Integer mes, Integer anio) {
        return getAllAhorros(mes, anio).stream()
                .mapToDouble(a -> a.getMonto() != null ? a.getMonto() : 0.0)
                .sum();
    }

    public void deleteAll() {
        ahorroRepository.deleteAll();
    }
}
