package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByHogar(Hogar hogar);
    Optional<Gasto> findByIdAndHogar(Long id, Hogar hogar);
}
