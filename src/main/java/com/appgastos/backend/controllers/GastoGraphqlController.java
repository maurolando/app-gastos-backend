package com.appgastos.backend.controllers;

import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.PagoCompartido;
import com.appgastos.backend.services.CategoriaService;
import com.appgastos.backend.services.CierreService;
import com.appgastos.backend.services.GastoService;
import com.appgastos.backend.services.IngresoService;
import com.appgastos.backend.services.PagoCompartidoService;
import com.appgastos.backend.services.AhorroService;
import com.appgastos.backend.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GastoGraphqlController {
    private final GastoService service;
    private final IngresoService ingresoService;
    private final CierreService cierreService;
    private final CategoriaService categoriaService;
    private final PagoCompartidoService pagoCompartidoService;
    private final ReportService reportService;
    private final AhorroService ahorroService;

    @QueryMapping
    public List<Gasto> getAllGastos(@Argument Integer mes, @Argument Integer anio) {
        try {
            return service.findAll(mes, anio);
        } catch (Exception e) {
            System.err.println("Error en getAllGastos: " + e.getMessage());
            return List.of();
        }
    }

    @QueryMapping
    public Double getGlobalBalance(@Argument Integer mes, @Argument Integer anio) {
        try {
            Double ingresos = ingresoService.getTotalIngresos(mes, anio);
            Double gastos = service.getTotalGastos(mes, anio);
            Double ahorros = ahorroService.getTotalAhorros(mes, anio);
            return ingresos - gastos - ahorros;
        } catch (Exception e) {
            return 0.0;
        }
    }


    @QueryMapping
    public List<Categoria> getAllCategorias(@Argument String tipo) {
        return categoriaService.findAll(tipo);
    }

    @QueryMapping
    public Map<String, String> getLastRecordsDates() {
        Map<String, String> dates = new HashMap<>();
        LocalDate lastGasto = service.getLastGastoDate();
        LocalDate lastIngreso = ingresoService.getLastIngresoDate();
        
        dates.put("lastGasto", lastGasto != null ? lastGasto.toString() : "N/A");
        dates.put("lastIngreso", lastIngreso != null ? lastIngreso.toString() : "N/A");
        
        // El balance usa el más reciente de ambos
        LocalDate lastBalance = null;
        if (lastGasto != null && lastIngreso != null) {
            lastBalance = lastGasto.isAfter(lastIngreso) ? lastGasto : lastIngreso;
        } else {
            lastBalance = (lastGasto != null) ? lastGasto : lastIngreso;
        }
        dates.put("lastBalance", lastBalance != null ? lastBalance.toString() : "N/A");
        
        return dates;
    }

    @QueryMapping
    public String generarReporteMensual(@Argument Integer mes, @Argument Integer anio) {
        try {
            return reportService.generarReporteMensual(mes, anio);
        } catch (Exception e) {
            System.err.println("Error en generarReporteMensual: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @MutationMapping
    public Gasto createGasto(
            @Argument Double amount,
            @Argument Long categoriaId,
            @Argument String date,
            @Argument String description,
            @Argument Long personaId,
            @Argument String formaPago,
            @Argument Boolean recurrent,
            @Argument Boolean pagado,
            @Argument String fechaVencimiento,
            @Argument Boolean esCompartido,
            @Argument Integer cuotaActual,
            @Argument Integer cuotasTotales) {
        try {
            LocalDate parsedVencimiento = (fechaVencimiento != null && !fechaVencimiento.isEmpty()) ? LocalDate.parse(fechaVencimiento) : null;
            return service.createGasto(amount, categoriaId, LocalDate.parse(date), description, personaId, formaPago, recurrent, pagado, parsedVencimiento, esCompartido, cuotaActual, cuotasTotales);
        } catch (Exception e) {
            System.err.println("Error en createGasto: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @MutationMapping
    public Gasto updateGasto(
            @Argument Long id,
            @Argument Double amount,
            @Argument Long categoriaId,
            @Argument String date,
            @Argument String description,
            @Argument Long personaId,
            @Argument String formaPago,
            @Argument Boolean recurrent,
            @Argument Boolean pagado,
            @Argument String fechaVencimiento,
            @Argument Boolean esCompartido,
            @Argument Integer cuotaActual,
            @Argument Integer cuotasTotales) {
        try {
            LocalDate parsedVencimiento = (fechaVencimiento != null && !fechaVencimiento.isEmpty()) ? LocalDate.parse(fechaVencimiento) : null;
            LocalDate parsedDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            return service.updateGasto(id, amount, categoriaId, parsedDate, description, personaId, formaPago, recurrent, pagado, parsedVencimiento, esCompartido, cuotaActual, cuotasTotales);
        } catch (Exception e) {
            System.err.println("Error en updateGasto: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @MutationMapping
    public Gasto pagarGasto(
            @Argument Long id,
            @Argument Double monto,
            @Argument Long personaId,
            @Argument String formaPago,
            @Argument String fechaPago) {
        try {
            LocalDate parsedFechaPago = (fechaPago != null && !fechaPago.isEmpty()) ? LocalDate.parse(fechaPago) : LocalDate.now();
            return service.markAsPaid(id, monto, personaId, formaPago, parsedFechaPago);
        } catch (Exception e) {
            System.err.println("Error en pagarGasto: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @MutationMapping
    public boolean finalizarMes(@Argument Integer mesActual, @Argument Integer anioActual) {
        try {
            if (mesActual == null || anioActual == null) return false;
            return cierreService.finalizarMes(mesActual, anioActual);
        } catch (Exception e) {
            System.err.println("Error en finalizarMes: " + e.getMessage());
            return false;
        }
    }

    @MutationMapping
    public boolean reiniciarDatos() {
        try {
            return cierreService.reiniciarDatos();
        } catch (Exception e) {
            System.err.println("Error en reiniciarDatos: " + e.getMessage());
            return false;
        }
    }

    @MutationMapping
    public Categoria createCategoria(@Argument String nombre, @Argument String icono, @Argument String tipo) {
        return categoriaService.createCategoria(nombre, icono, tipo);
    }

    @MutationMapping
    public boolean deleteCategoria(@Argument Long id) {
        try {
            categoriaService.deleteCategoria(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @MutationMapping
    public Categoria updateCategoria(@Argument Long id, @Argument String nombre, @Argument String icono, @Argument String tipo) {
        return categoriaService.updateCategoria(id, nombre, icono, tipo);
    }

    @QueryMapping
    public List<PagoCompartido> getPagosCompartidos(@Argument Long gastoId) {
        return pagoCompartidoService.getPagosPorGasto(gastoId);
    }

    @MutationMapping
    public PagoCompartido agregarPagoCompartido(
            @Argument Long gastoId,
            @Argument Long personaId,
            @Argument Double monto,
            @Argument String formaPago,
            @Argument String fecha) {
        return pagoCompartidoService.agregarPago(gastoId, personaId, monto, formaPago, fecha);
    }
}
