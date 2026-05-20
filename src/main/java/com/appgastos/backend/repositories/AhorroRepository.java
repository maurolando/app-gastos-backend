package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Ahorro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AhorroRepository extends JpaRepository<Ahorro, Long> {
}
