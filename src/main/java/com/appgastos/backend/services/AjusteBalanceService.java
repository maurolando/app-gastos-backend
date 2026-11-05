package com.appgastos.backend.services;

import com.appgastos.backend.config.HogarActual;
import com.appgastos.backend.models.AjusteBalance;
import com.appgastos.backend.repositories.AjusteBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AjusteBalanceService {

    private final PeriodoService periodoService;
    private final AjusteBalanceRepository repository;

    public List<AjusteBalance> getAjustes(Integer mes, Integer anio) {
        List<AjusteBalance> all = repository.findByHogar(HogarActual.get());
        if (mes == null && anio == null) return all;

        var filtro = periodoService.filtro(mes, anio);
        return all.stream().filter(a -> filtro.test(a.getFecha())).toList();
    }

    public AjusteBalance createAjuste(Double monto, LocalDate fecha, String descripcion) {
        if (monto == null || monto == 0 || !Double.isFinite(monto)) {
            throw new ValidacionException("El ajuste tiene que ser un monto distinto de cero.");
        }
        AjusteBalance ajuste = new AjusteBalance();
        ajuste.setMonto(monto);
        ajuste.setFecha(fecha != null ? fecha : LocalDate.now());
        ajuste.setDescripcion(descripcion);
        ajuste.setHogar(HogarActual.get());
        return repository.save(ajuste);
    }

    public void deleteAjuste(Long id) {
        repository.findByIdAndHogar(id, HogarActual.get()).ifPresent(repository::delete);
    }

    public Double getTotalAjustes(Integer mes, Integer anio) {
        return getAjustes(mes, anio).stream().mapToDouble(AjusteBalance::getMonto).sum();
    }

    public void deleteAll() {
        repository.deleteByHogar(HogarActual.get());
    }
}
