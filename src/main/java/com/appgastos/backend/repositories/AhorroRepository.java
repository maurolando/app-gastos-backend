package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Ahorro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AhorroRepository extends JpaRepository<Ahorro, Long> {

    List<Ahorro> findByHogar(Hogar hogar);
    Optional<Ahorro> findByIdAndHogar(Long id, Hogar hogar);
    void deleteByHogar(Hogar hogar);
}
