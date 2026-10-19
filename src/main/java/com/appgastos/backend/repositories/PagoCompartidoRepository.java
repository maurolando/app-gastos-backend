package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.PagoCompartido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PagoCompartidoRepository extends JpaRepository<PagoCompartido, Long> {
    List<PagoCompartido> findByGastoId(Long gastoId);
    void deleteByGastoHogar(Hogar hogar);
}
