package com.appgastos.backend.services;

import com.appgastos.backend.dto.CierreResult;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Ingreso;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CierreService {

    private final GastoService gastoService;
    private final IngresoService ingresoService;
    private final AhorroService ahorroService;

    @Transactional
    public CierreResult finalizarMes(int mesActual, int anioActual) {
        System.out.println("Iniciando cierre de mes para " + mesActual + "/" + anioActual);

        // Calcular mes siguiente
        LocalDate currentRef = LocalDate.of(anioActual, mesActual, 1);
        LocalDate nextMonth = currentRef.plusMonths(1);

        // Lo que ya vive en el mes destino. El cierre no lleva registro de haberse
        // ejecutado, asi que sin esto apretar dos veces duplicaba todos los fijos.
        List<Gasto> gastosDestino = new ArrayList<>(
                gastoService.findAll(nextMonth.getMonthValue(), nextMonth.getYear()));
        List<Ingreso> ingresosDestino = new ArrayList<>(
                ingresoService.getAllIngresos(nextMonth.getMonthValue(), nextMonth.getYear()));

        int gastosCopiados = 0;
        int gastosOmitidos = 0;
        int ingresosCopiados = 0;
        int ingresosOmitidos = 0;

        // 1. Procesar Gastos Recurrentes
        List<Gasto> gastosRecurrentes = gastoService.findAll(mesActual, anioActual).stream()
                .filter(g -> g.getRecurrent() != null && g.getRecurrent())
                .toList();

        System.out.println("Gastos recurrentes encontrados: " + gastosRecurrentes.size());

        for (Gasto g : gastosRecurrentes) {
            Integer cuotaActual = g.getCuotaActual();
            Integer cuotasTotales = g.getCuotasTotales();

            if (cuotasTotales != null && cuotaActual != null) {
                if (cuotaActual >= cuotasTotales) {
                    System.out.println("Gasto fijo recurrente finalizado (todas las cuotas cubiertas): " + g.getDescription());
                    continue; // No se crea para el mes siguiente ya que se completaron las cuotas
                }
                cuotaActual = cuotaActual + 1; // Avanza a la siguiente cuota
            }

            int copia = indiceDeCopia(g, cuotaActual, gastosDestino);
            if (copia >= 0) {
                // Se consume la copia encontrada: si hay dos fijos identicos en el mes
                // origen, el segundo no puede darse por copiado mirando la misma fila.
                gastosDestino.remove(copia);
                System.out.println("Gasto fijo ya presente en el mes destino, se omite: " + g.getDescription());
                gastosOmitidos++;
                continue;
            }

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
                    g.getEsCompartido(), // Mantiene el flag de compartido
                    cuotaActual,
                    cuotasTotales
            );
            gastosCopiados++;
        }

        // 2. Procesar Ingresos Recurrentes
        List<Ingreso> ingresosRecurrentes = ingresoService.getAllIngresos(mesActual, anioActual).stream()
                .filter(i -> i.getRecurrent() != null && i.getRecurrent())
                .toList();

        System.out.println("Ingresos recurrentes encontrados: " + ingresosRecurrentes.size());

        for (Ingreso i : ingresosRecurrentes) {
            int copia = indiceDeCopia(i, ingresosDestino);
            if (copia >= 0) {
                ingresosDestino.remove(copia);
                System.out.println("Ingreso fijo ya presente en el mes destino, se omite.");
                ingresosOmitidos++;
                continue;
            }

            LocalDate nextDate = i.getFecha().plusMonths(1);
            ingresoService.createIngreso(
                    i.getMonto(),
                    nextDate,
                    i.getCategoria() != null ? i.getCategoria().getId() : null,
                    i.getPersona() != null ? i.getPersona().getId() : null,
                    true
            );
            ingresosCopiados++;
        }

        System.out.println("Cierre de mes completado. Gastos copiados: " + gastosCopiados
                + ", omitidos: " + gastosOmitidos + ". Ingresos copiados: " + ingresosCopiados
                + ", omitidos: " + ingresosOmitidos + ".");

        return new CierreResult(gastosCopiados, ingresosCopiados, gastosOmitidos, ingresosOmitidos);
    }

    /**
     * Indice de la copia de este gasto fijo en el mes destino, o -1 si no esta.
     *
     * Equivalente es mismo monto, descripcion, categoria, persona y numero de cuota.
     * No se compara la fecha exacta porque el usuario puede haberla corrido a mano
     * despues del cierre.
     *
     * Devuelve el indice y no un booleano a proposito: quien llama saca del pool la
     * fila que consumio, para que N fijos identicos requieran N copias en destino y
     * no se tapen entre si con una sola.
     */
    private int indiceDeCopia(Gasto origen, Integer cuotaEsperada, List<Gasto> destino) {
        for (int i = 0; i < destino.size(); i++) {
            Gasto d = destino.get(i);
            if (Boolean.TRUE.equals(d.getRecurrent())
                    && Objects.equals(d.getAmount(), origen.getAmount())
                    && Objects.equals(d.getDescription(), origen.getDescription())
                    && Objects.equals(categoriaId(d), categoriaId(origen))
                    && Objects.equals(personaId(d), personaId(origen))
                    && Objects.equals(d.getCuotaActual(), cuotaEsperada)) {
                return i;
            }
        }
        return -1;
    }

    private int indiceDeCopia(Ingreso origen, List<Ingreso> destino) {
        for (int i = 0; i < destino.size(); i++) {
            Ingreso d = destino.get(i);
            if (Boolean.TRUE.equals(d.getRecurrent())
                    && Objects.equals(d.getMonto(), origen.getMonto())
                    && Objects.equals(categoriaId(d), categoriaId(origen))
                    && Objects.equals(personaId(d), personaId(origen))) {
                return i;
            }
        }
        return -1;
    }

    private Long categoriaId(Gasto g) {
        return g.getCategoria() != null ? g.getCategoria().getId() : null;
    }

    private Long personaId(Gasto g) {
        return g.getPersona() != null ? g.getPersona().getId() : null;
    }

    private Long categoriaId(Ingreso i) {
        return i.getCategoria() != null ? i.getCategoria().getId() : null;
    }

    private Long personaId(Ingreso i) {
        return i.getPersona() != null ? i.getPersona().getId() : null;
    }

    @Transactional
    public boolean reiniciarDatos() {
        System.out.println("Reiniciando datos (Gastos, Ingresos y Ahorros)...");
        gastoService.deleteAll();
        ingresoService.deleteAll();
        ahorroService.deleteAll();
        System.out.println("Datos reiniciados con éxito.");
        return true;
    }
}

