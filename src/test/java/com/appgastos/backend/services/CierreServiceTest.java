package com.appgastos.backend.services;

import com.appgastos.backend.dto.CierreResult;
import com.appgastos.backend.models.Categoria;
import com.appgastos.backend.models.Gasto;
import com.appgastos.backend.models.Ingreso;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.CategoriaRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El cierre de mes copia los gastos e ingresos fijos al mes siguiente. Antes no
 * llevaba registro de haberse ejecutado, asi que apretarlo dos veces duplicaba
 * todo sin aviso. Estos tests fijan ese comportamiento.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CierreServiceTest {

    private static final int MES = 3;
    private static final int ANIO = 2026;

    @Autowired private CierreService cierreService;
    @Autowired private GastoService gastoService;
    @Autowired private IngresoService ingresoService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PersonaRepository personaRepository;

    private Long categoriaId;
    private Long personaId;

    @BeforeEach
    void setUp() {
        gastoService.deleteAll();
        ingresoService.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNombre("Alquiler");
        categoria.setTipo("GASTO");
        categoriaId = categoriaRepository.save(categoria).getId();

        Persona persona = new Persona();
        persona.setNombre("Mauro");
        personaId = personaRepository.save(persona).getId();
    }

    @Test
    @DisplayName("copia los gastos fijos al mes siguiente")
    void copiaLosFijos() {
        crearGastoFijo(1_500_000.0, "Alquiler depto");

        CierreResult resultado = cierreService.finalizarMes(MES, ANIO);

        assertThat(resultado.gastosCopiados()).isEqualTo(1);
        assertThat(resultado.gastosOmitidos()).isZero();
        assertThat(gastosDelMesSiguiente()).hasSize(1);
    }

    @Test
    @DisplayName("cerrar dos veces el mismo mes no duplica nada")
    void esIdempotente() {
        crearGastoFijo(1_500_000.0, "Alquiler depto");
        crearIngresoFijo(4_000_000.0);

        cierreService.finalizarMes(MES, ANIO);
        CierreResult segundo = cierreService.finalizarMes(MES, ANIO);

        assertThat(segundo.gastosCopiados()).isZero();
        assertThat(segundo.ingresosCopiados()).isZero();
        assertThat(segundo.gastosOmitidos()).isEqualTo(1);
        assertThat(segundo.ingresosOmitidos()).isEqualTo(1);

        assertThat(gastosDelMesSiguiente()).hasSize(1);
        assertThat(ingresosDelMesSiguiente()).hasSize(1);
    }

    @Test
    @DisplayName("dos gastos fijos identicos se copian los dos, no se colapsan entre si")
    void noColapsaFijosIdenticos() {
        crearGastoFijo(50_000.0, "Cuota gimnasio");
        crearGastoFijo(50_000.0, "Cuota gimnasio");

        CierreResult resultado = cierreService.finalizarMes(MES, ANIO);

        assertThat(resultado.gastosCopiados()).isEqualTo(2);
        assertThat(gastosDelMesSiguiente()).hasSize(2);
    }

    @Test
    @DisplayName("un fijo agregado despues del cierre se copia en el segundo intento")
    void copiaSoloLoNuevoEnUnCierreParcial() {
        crearGastoFijo(1_500_000.0, "Alquiler depto");
        cierreService.finalizarMes(MES, ANIO);

        crearGastoFijo(300_000.0, "Internet");
        CierreResult segundo = cierreService.finalizarMes(MES, ANIO);

        assertThat(segundo.gastosCopiados()).isEqualTo(1);
        assertThat(segundo.gastosOmitidos()).isEqualTo(1);
        assertThat(gastosDelMesSiguiente()).hasSize(2);
    }

    @Test
    @DisplayName("un gasto variable no se copia")
    void noCopiaLosVariables() {
        gastoService.createGasto(80_000.0, categoriaId, LocalDate.of(ANIO, MES, 12), "Supermercado",
                personaId, "Efectivo", false, true, null, false, null, null);

        CierreResult resultado = cierreService.finalizarMes(MES, ANIO);

        assertThat(resultado.gastosCopiados()).isZero();
        assertThat(gastosDelMesSiguiente()).isEmpty();
    }

    @Test
    @DisplayName("avanza la cuota y deja de copiar cuando se completan")
    void avanzaCuotasHastaTerminarlas() {
        gastoService.createGasto(200_000.0, categoriaId, LocalDate.of(ANIO, MES, 5), "Heladera",
                personaId, "Tarjeta de Crédito", true, false, null, false, 2, 3);

        cierreService.finalizarMes(MES, ANIO);

        List<Gasto> siguientes = gastosDelMesSiguiente();
        assertThat(siguientes).hasSize(1);
        assertThat(siguientes.get(0).getCuotaActual()).isEqualTo(3);
        // El nuevo mes siempre arranca pendiente de pago.
        assertThat(siguientes.get(0).getPagado()).isFalse();

        // La cuota 3 de 3 ya es la ultima: cerrar ese mes no genera una cuarta.
        CierreResult ultimo = cierreService.finalizarMes(MES + 1, ANIO);
        assertThat(ultimo.gastosCopiados()).isZero();
        assertThat(gastoService.findAll(MES + 2, ANIO)).isEmpty();
    }

    private void crearGastoFijo(Double monto, String descripcion) {
        gastoService.createGasto(monto, categoriaId, LocalDate.of(ANIO, MES, 10), descripcion,
                personaId, "Transferencia", true, false, LocalDate.of(ANIO, MES, 15), false, null, null);
    }

    private void crearIngresoFijo(Double monto) {
        ingresoService.createIngreso(monto, LocalDate.of(ANIO, MES, 1), categoriaId, personaId, true);
    }

    private List<Gasto> gastosDelMesSiguiente() {
        return gastoService.findAll(MES + 1, ANIO);
    }

    private List<Ingreso> ingresosDelMesSiguiente() {
        return ingresoService.getAllIngresos(MES + 1, ANIO);
    }
}
