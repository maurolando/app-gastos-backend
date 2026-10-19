package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Ingreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    List<Ingreso> findByHogar(Hogar hogar);
    Optional<Ingreso> findByIdAndHogar(Long id, Hogar hogar);
    void deleteByHogar(Hogar hogar);
}
