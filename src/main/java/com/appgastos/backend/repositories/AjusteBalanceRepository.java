package com.appgastos.backend.repositories;

import com.appgastos.backend.models.AjusteBalance;
import com.appgastos.backend.models.Hogar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AjusteBalanceRepository extends JpaRepository<AjusteBalance, Long> {

    List<AjusteBalance> findByHogar(Hogar hogar);
    Optional<AjusteBalance> findByIdAndHogar(Long id, Hogar hogar);
    void deleteByHogar(Hogar hogar);
}
