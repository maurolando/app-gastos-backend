package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findByHogarOrderByFechaRegistroDesc(Hogar hogar);
    Optional<Compra> findByIdAndHogar(Long id, Hogar hogar);
}
