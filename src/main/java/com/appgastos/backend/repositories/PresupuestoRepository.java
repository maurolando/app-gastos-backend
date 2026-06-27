package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {
    List<Presupuesto> findByMesAndAnio(Integer mes, Integer anio);
    Optional<Presupuesto> findByCategoriaIdAndMesAndAnio(Long categoriaId, Integer mes, Integer anio);
}
