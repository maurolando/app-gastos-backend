package com.appgastos.backend.services;

import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.repositories.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {
    private final CategoriaRepository repository;

    public List<Categoria> findAll(String tipo) {
        if (tipo != null) {
            return repository.findByTipo(tipo);
        }
        return repository.findAll();
    }

    public Categoria createCategoria(String nombre, String icono, String tipo) {
        Categoria cat = new Categoria();
        cat.setNombre(nombre);
        cat.setIcono(icono);
        cat.setTipo(tipo);
        return repository.save(cat);
    }

    public void deleteCategoria(Long id) {
        repository.deleteById(id);
    }

    public Categoria updateCategoria(Long id, String nombre, String icono, String tipo) {
        return repository.findById(id).map(cat -> {
            if (nombre != null) cat.setNombre(nombre);
            if (icono != null) cat.setIcono(icono);
            if (tipo != null) cat.setTipo(tipo);
            return repository.save(cat);
        }).orElse(null);
    }
}
