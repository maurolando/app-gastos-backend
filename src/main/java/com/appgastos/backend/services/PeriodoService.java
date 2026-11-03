package com.appgastos.backend.services;

import com.appgastos.backend.config.HogarActual;
import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.repositories.HogarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Predicate;

/**
 * El "mes" de un hogar no tiene por que ser el del calendario: si cobran el 4,
 * octubre va del 4/10 al 3/11. El periodo se nombra por el mes en que empieza.
 */
@Service
@RequiredArgsConstructor
public class PeriodoService {

    private final HogarRepository hogarRepository;

    /** Periodo al que pertenece una fecha: antes del dia de inicio cuenta en el mes anterior. */
    public static YearMonth periodoDe(LocalDate fecha, int diaInicio) {
        return YearMonth.from(fecha.getDayOfMonth() >= diaInicio ? fecha : fecha.minusMonths(1));
    }

    /** El hogar recien leido: el del token puede ser un proxy sin sesion o estar desactualizado. */
    public Hogar hogar() {
        return hogarRepository.findById(HogarActual.get().getId())
                .orElseThrow(() -> new ValidacionException("El hogar no existe."));
    }

    public int diaInicio() {
        return diaInicio(hogar());
    }

    public static int diaInicio(Hogar hogar) {
        return hogar.getDiaInicioMes() != null ? hogar.getDiaInicioMes() : 1;
    }

    /** Anota un periodo como cerrado sin retroceder si ya habia uno posterior. */
    public static void marcarCerrado(Hogar hogar, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        if (hogar.getCerradoHasta() == null || hogar.getCerradoHasta().isBefore(inicio)) {
            hogar.setCerradoHasta(inicio);
        }
    }

    /**
     * Guarda la configuracion. Al prender el cierre automatico se da por cerrado
     * el periodo anterior a hoy: no se adivina que paso con los meses viejos.
     */
    @Transactional
    public Hogar configurar(int diaInicioMes, boolean cierreAutomatico, LocalDate hoy) {
        if (diaInicioMes < 1 || diaInicioMes > 28) {
            // Del 29 en adelante, febrero no tendria dia de inicio.
            throw new ValidacionException("El dia de inicio tiene que ser entre 1 y 28.");
        }
        Hogar hogar = hogar();
        hogar.setDiaInicioMes(diaInicioMes);
        if (cierreAutomatico && !Boolean.TRUE.equals(hogar.getCierreAutomatico())) {
            marcarCerrado(hogar, periodoDe(hoy, diaInicioMes).minusMonths(1));
        }
        hogar.setCierreAutomatico(cierreAutomatico);
        return hogarRepository.save(hogar);
    }

    /** Filtro de gastos, ingresos y ahorros por periodo; mes o anio en null no filtran ese campo. */
    public Predicate<LocalDate> filtro(Integer mes, Integer anio) {
        int dia = diaInicio();
        return fecha -> {
            if (fecha == null) return false;
            YearMonth p = periodoDe(fecha, dia);
            return (mes == null || p.getMonthValue() == mes) && (anio == null || p.getYear() == anio);
        };
    }
}
