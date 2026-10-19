package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HogarRepository extends JpaRepository<Hogar, Long> {
}
