package com.appgastos.backend.repositories;

import com.appgastos.backend.models.ReglaHogar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReglaHogarRepository extends JpaRepository<ReglaHogar, Long> {
    Optional<ReglaHogar> findFirstByOrderByIdAsc();
}
