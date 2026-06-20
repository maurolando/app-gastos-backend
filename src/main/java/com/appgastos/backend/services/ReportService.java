package com.appgastos.backend.services;

import com.appgastos.backend.dto.TransaccionReportDto;
import com.appgastos.backend.models.Ahorro;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Ingreso;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final GastoService gastoService;
    private final IngresoService ingresoService;
    private final AhorroService ahorroService;

    public String generarReporteMensual(int mes, int anio) {
        try {
            // 1. Obtener datos
            List<Gasto> gastos = gastoService.findAll(mes, anio);
            List<Ingreso> ingresos = ingresoService.getAllIngresos(mes, anio);
            List<Ahorro> ahorros = ahorroService.getAllAhorros(mes, anio);

            // Calcular totales
            double totalIngresos = ingresoService.getTotalIngresos(mes, anio);
            double totalGastos = gastoService.getTotalGastos(mes, anio);
            double totalAhorros = ahorroService.getTotalAhorros(mes, anio);
            double balance = totalIngresos - totalGastos - totalAhorros;

            // 2. Mapear a TransaccionReportDto
            List<TransaccionReportDto> transacciones = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (Ingreso i : ingresos) {
                String catName = i.getCategoria() != null ? i.getCategoria().getNombre() : "Ingreso";
                String persName = i.getPersona() != null ? i.getPersona().getNombre() : "N/A";
                String fechaStr = i.getFecha() != null ? i.getFecha().format(formatter) : "N/A";
                transacciones.add(new TransaccionReportDto(
                        fechaStr,
                        "Ingresos",
                        catName,
                        persName,
                        i.getMonto(),
                        "N/A",
                        "Recibido"
                ));
            }

            for (Gasto g : gastos) {
                String catName = g.getCategoria() != null ? g.getCategoria().getNombre() : "Gasto";
                if (g.getDescription() != null && !g.getDescription().trim().isEmpty()) {
                    catName += " - " + g.getDescription();
                }
                String persName = g.getPersona() != null ? g.getPersona().getNombre() : "N/A";
                String fechaStr = g.getDate() != null ? g.getDate().format(formatter) : "N/A";
                String formaPago = g.getFormaPago() != null ? g.getFormaPago() : "N/A";
                String estado = Boolean.TRUE.equals(g.getPagado()) ? "Pagado" : "Pendiente";
                transacciones.add(new TransaccionReportDto(
                        fechaStr,
                        "Gastos",
                        catName,
                        persName,
                        g.getAmount(),
                        formaPago,
                        estado
                ));
            }

            for (Ahorro a : ahorros) {
                String desc = a.getDescripcion() != null && !a.getDescripcion().trim().isEmpty() 
                        ? a.getDescripcion() : "Ahorro";
                String persName = a.getPersona() != null ? a.getPersona().getNombre() : "N/A";
                String fechaStr = a.getFecha() != null ? a.getFecha().format(formatter) : "N/A";
                transacciones.add(new TransaccionReportDto(
                        fechaStr,
                        "Ahorros",
                        desc,
                        persName,
                        a.getMonto(),
                        "N/A",
                        "Guardado"
                ));
            }

            // Ordenar transacciones por fecha dentro de cada tipo
            transacciones.sort((t1, t2) -> {
                int typeCompare = getTipoPriority(t1.getTipo()) - getTipoPriority(t2.getTipo());
                if (typeCompare != 0) return typeCompare;
                
                try {
                    LocalDate d1 = LocalDate.parse(t1.getFecha(), formatter);
                    LocalDate d2 = LocalDate.parse(t2.getFecha(), formatter);
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return t1.getFecha().compareTo(t2.getFecha());
                }
            });

            // 3. Cargar el reporte
            InputStream jrxmlStream = getClass().getResourceAsStream("/reports/resumen_mensual.jrxml");
            if (jrxmlStream == null) {
                throw new RuntimeException("No se encontró el archivo del reporte resumen_mensual.jrxml en el classpath.");
            }

            // Compilar el JRXML a JasperReport
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            // Parámetros del reporte
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("MES_ANIO", getMonthName(mes) + " " + anio);
            parameters.put("TOTAL_INGRESOS", totalIngresos);
            parameters.put("TOTAL_GASTOS", totalGastos);
            parameters.put("TOTAL_AHORROS", totalAhorros);
            parameters.put("BALANCE", balance);

            // DataSource
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(transacciones);

            // Llenar el reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Exportar a PDF en bytes
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            // Codificar en Base64
            return Base64.getEncoder().encodeToString(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar el reporte PDF: " + e.getMessage(), e);
        }
    }

    private int getTipoPriority(String tipo) {
        if ("Ingresos".equalsIgnoreCase(tipo)) return 1;
        if ("Gastos".equalsIgnoreCase(tipo)) return 2;
        if ("Ahorros".equalsIgnoreCase(tipo)) return 3;
        return 4;
    }

    private String getMonthName(int mes) {
        String[] meses = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", 
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        if (mes >= 1 && mes <= 12) {
            return meses[mes - 1];
        }
        return "";
    }
}
