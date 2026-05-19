package com.appgastos.backend.services;

import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Ingreso;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CierreService {

    private final GastoService gastoService;
    private final IngresoService ingresoService;

    @Transactional
    public boolean finalizarMes(int mesActual, int anioActual) {
        System.out.println("Iniciando cierre de mes para " + mesActual + "/" + anioActual);
        
        // Calcular mes siguiente
        LocalDate currentRef = LocalDate.of(anioActual, mesActual, 1);
        LocalDate nextMonth = currentRef.plusMonths(1);

        // 1. Procesar Gastos Recurrentes
        List<Gasto> gastosRecurrentes = gastoService.findAll(mesActual, anioActual).stream()
                .filter(g -> g.getRecurrent() != null && g.getRecurrent())
                .toList();
        
        System.out.println("Gastos recurrentes encontrados: " + gastosRecurrentes.size());

        for (Gasto g : gastosRecurrentes) {
            LocalDate nextDate = g.getDate().plusMonths(1);
            LocalDate nextVencimiento = g.getFechaVencimiento() != null ? g.getFechaVencimiento().plusMonths(1) : null;
            gastoService.createGasto(
                    g.getAmount(),
                    g.getCategoria() != null ? g.getCategoria().getId() : null,
                    nextDate,
                    g.getDescription(),
                    g.getPersona() != null ? g.getPersona().getId() : null,
                    g.getFormaPago(),
                    true,
                    false, // Siempre empieza como no pagado el nuevo mes
                    nextVencimiento,
                    g.getEsCompartido() // Mantiene el flag de compartido
            );
        }

        // 2. Procesar Ingresos Recurrentes
        List<Ingreso> ingresosRecurrentes = ingresoService.getAllIngresos(mesActual, anioActual).stream()
                .filter(i -> i.getRecurrent() != null && i.getRecurrent())
                .toList();

        System.out.println("Ingresos recurrentes encontrados: " + ingresosRecurrentes.size());

        for (Ingreso i : ingresosRecurrentes) {
            LocalDate nextDate = i.getFecha().plusMonths(1);
            ingresoService.createIngreso(
                    i.getMonto(),
                    nextDate,
                    i.getCategoria() != null ? i.getCategoria().getId() : null,
                    i.getPersona() != null ? i.getPersona().getId() : null,
                    true
            );
        }

        System.out.println("Cierre de mes completado.");
        return true;
    }

    @Transactional
    public boolean reiniciarDatos() {
        System.out.println("Reiniciando datos (Gastos e Ingresos)...");
        gastoService.deleteAll();
        ingresoService.deleteAll();
        System.out.println("Datos reiniciados con éxito.");
        return true;
    }
}
