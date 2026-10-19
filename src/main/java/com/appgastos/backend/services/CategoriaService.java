package com.appgastos.backend.services;

import com.appgastos.backend.config.HogarActual;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.GrupoGasto;
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
            return repository.findByHogarAndTipo(HogarActual.get(), tipo);
        }
        return repository.findByHogar(HogarActual.get());
    }

    public Categoria createCategoria(String nombre, String icono, String tipo, GrupoGasto grupo) {
        Categoria cat = new Categoria();
        cat.setNombre(nombre);
        cat.setIcono(icono);
        cat.setTipo(tipo);
        cat.setGrupo(grupoSegunTipo(tipo, grupo));
        cat.setHogar(HogarActual.get());
        return repository.save(cat);
    }

    public void deleteCategoria(Long id) {
        repository.findByIdAndHogar(id, HogarActual.get()).ifPresent(repository::delete);
    }

    public Categoria updateCategoria(Long id, String nombre, String icono, String tipo, GrupoGasto grupo) {
        return repository.findByIdAndHogar(id, HogarActual.get()).map(cat -> {
            if (nombre != null) cat.setNombre(nombre);
            if (icono != null) cat.setIcono(icono);
            if (tipo != null) cat.setTipo(tipo);
            // El grupo se reemplaza tal cual viene, aunque sea null, para que el usuario
            // pueda devolver una categoría a "sin clasificar" desde el formulario.
            cat.setGrupo(grupoSegunTipo(cat.getTipo(), grupo));
            return repository.save(cat);
        }).orElse(null);
    }

    /** Solo los gastos entran en la guía de distribución: un ingreso no tiene grupo. */
    private GrupoGasto grupoSegunTipo(String tipo, GrupoGasto grupo) {
        return "GASTO".equals(tipo) ? grupo : null;
    }
}
