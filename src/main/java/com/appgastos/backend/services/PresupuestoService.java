package com.appgastos.backend.services;

import com.appgastos.backend.config.HogarActual;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Presupuesto;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.PresupuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PresupuestoService {

    @Autowired
    private PresupuestoRepository presupuestoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Presupuesto> getPresupuestos(Integer mes, Integer anio) {
        return presupuestoRepository.findByHogarAndMesAndAnio(HogarActual.get(), mes, anio);
    }

    public Presupuesto setPresupuesto(Long categoriaId, Double monto, Integer mes, Integer anio) {
        Categoria categoria = categoriaRepository.findByIdAndHogar(categoriaId, HogarActual.get())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoriaId));

        Optional<Presupuesto> existing = presupuestoRepository.findByHogarAndCategoriaIdAndMesAndAnio(HogarActual.get(), categoriaId, mes, anio);
        
        Presupuesto presupuesto;
        if (existing.isPresent()) {
            presupuesto = existing.get();
            presupuesto.setMonto(monto);
        } else {
            presupuesto = new Presupuesto();
            presupuesto.setCategoria(categoria);
            presupuesto.setMonto(monto);
            presupuesto.setMes(mes);
            presupuesto.setAnio(anio);
            presupuesto.setHogar(HogarActual.get());
        }
        
        return presupuestoRepository.save(presupuesto);
    }
}
