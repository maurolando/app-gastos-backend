package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {
    Optional<Invitacion> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
