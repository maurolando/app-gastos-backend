package com.appgastos.backend.services;

import com.appgastos.backend.models.Compra;
import com.appgastos.backend.repositories.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;

    public List<Compra> getTodasLasCompras() {
        return compraRepository.findAllByOrderByFechaRegistroDesc();
    }

    public Compra agregarCompra(String nombre, Double precio, String lugar) {
        Compra compra = new Compra();
        compra.setNombre(nombre);
        compra.setPrecio(precio);
        compra.setLugar(lugar);
        compra.setComprado(false);
        compra.setFechaRegistro(LocalDate.now());
        return compraRepository.save(compra);
    }

    public Compra alternarEstadoCompra(Long id) {
        Compra compra = compraRepository.findById(id).orElseThrow(() -> new RuntimeException("Compra no encontrada"));
        compra.setComprado(!compra.getComprado());
        return compraRepository.save(compra);
    }

    public void eliminarCompra(Long id) {
        compraRepository.deleteById(id);
    }
}
