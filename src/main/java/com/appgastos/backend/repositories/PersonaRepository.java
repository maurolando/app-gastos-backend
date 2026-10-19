package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    List<Persona> findByHogar(Hogar hogar);
    Optional<Persona> findByIdAndHogar(Long id, Hogar hogar);
    Optional<Persona> findFirstByNombreIgnoreCaseAndHogarIsNotNull(String nombre);
    boolean existsByNombreIgnoreCaseAndHogarIsNotNull(String nombre);
}
