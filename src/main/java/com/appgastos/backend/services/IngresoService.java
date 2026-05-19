package com.appgastos.backend.services;

import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Ingreso;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.IngresoRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngresoService {

    private final IngresoRepository ingresoRepository;
    private final PersonaRepository personaRepository;
    private final CategoriaRepository categoriaRepository;

    public List<Ingreso> getAllIngresos(Integer mes, Integer anio) {
        List<Ingreso> all = ingresoRepository.findAll();
        if (mes == null && anio == null) return all;
        
        return all.stream()
                .filter(i -> {
                    if (i.getFecha() == null) return false;
                    boolean match = true;
                    if (mes != null) match &= i.getFecha().getMonthValue() == mes;
                    if (anio != null) match &= i.getFecha().getYear() == anio;
                    return match;
                })
                .toList();
    }

    public Ingreso createIngreso(Double monto, LocalDate fecha, Long categoriaId, Long personaId, Boolean recurrent) {
        Persona persona = personaId != null ? personaRepository.findById(personaId).orElse(null) : null;
        Categoria categoria = categoriaId != null ? categoriaRepository.findById(categoriaId).orElse(null) : null;

        Ingreso ingreso = new Ingreso();
        ingreso.setMonto(monto);
        ingreso.setFecha(fecha != null ? fecha : LocalDate.now());
        ingreso.setCategoria(categoria);
        ingreso.setPersona(persona);
        ingreso.setRecurrent(recurrent != null && recurrent);

        return ingresoRepository.save(ingreso);
    }

    public LocalDate getLastIngresoDate() {
        return ingresoRepository.findAll().stream()
                .map(Ingreso::getFecha)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public Double getTotalIngresos(Integer mes, Integer anio) {
        return getAllIngresos(mes, anio).stream()
                .mapToDouble(i -> i.getMonto() != null ? i.getMonto() : 0.0)
                .sum();
    }
    public void deleteAll() {
        ingresoRepository.deleteAll();
    }
}
