package com.appgastos.backend.repositories;

import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByHogar(Hogar hogar);
    List<Categoria> findByHogarAndTipo(Hogar hogar, String tipo);
    Optional<Categoria> findByIdAndHogar(Long id, Hogar hogar);
}
