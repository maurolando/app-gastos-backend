package com.appgastos.backend.controllers;

import com.appgastos.backend.models.Compra;
import com.appgastos.backend.services.CompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CompraGraphqlController {

    private final CompraService compraService;

    @QueryMapping
    public List<Compra> getShoppingList() {
        return compraService.getTodasLasCompras();
    }

    @MutationMapping
    public Compra createShoppingItem(
            @Argument String nombre,
            @Argument Double precio,
            @Argument String lugar) {
        return compraService.agregarCompra(nombre, precio, lugar);
    }

    @MutationMapping
    public Compra toggleShoppingItemStatus(@Argument Long id) {
        return compraService.alternarEstadoCompra(id);
    }

    @MutationMapping
    public Boolean deleteShoppingItem(@Argument Long id) {
        compraService.eliminarCompra(id);
        return true;
    }
}
