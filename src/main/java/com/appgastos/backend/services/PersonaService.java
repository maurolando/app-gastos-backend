package com.appgastos.backend.services;

import com.appgastos.backend.config.HogarActual;
import com.appgastos.backend.models.Hogar;
import com.appgastos.backend.models.Invitacion;
import com.appgastos.backend.models.Persona;
import com.appgastos.backend.repositories.HogarRepository;
import com.appgastos.backend.repositories.InvitacionRepository;
import com.appgastos.backend.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {
    private static final int LARGO_MINIMO_CLAVE = 6;
    private static final int HORAS_INVITACION = 48;
    // Sin 0/O, 1/I/L: el código se dicta o se tipea desde otro celular.
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 6;
    private static final SecureRandom AZAR = new SecureRandom();

    private final PersonaRepository repository;
    private final HogarRepository hogarRepository;
    private final InvitacionRepository invitacionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Persona> findAll() {
        return repository.findByHogar(HogarActual.get());
    }

    public Persona updatePersona(Long id, String nombre, Boolean activo) {
        return repository.findByIdAndHogar(id, HogarActual.get()).map(p -> {
            if (nombre != null && !nombre.equalsIgnoreCase(p.getNombre())) p.setNombre(nombreLibre(nombre));
            if (activo != null) p.setActivo(activo);
            return repository.save(p);
        }).orElse(null);
    }

    /** Autentica una persona. Retorna la persona si es correcto, null si falla. */
    public Persona login(String nombre, String clave) {
        return repository.findFirstByNombreIgnoreCaseAndHogarIsNotNull(nombre)
            .filter(p -> p.getClave() != null && passwordEncoder.matches(clave, p.getClave()))
            .orElse(null);
    }

    /** Establece o cambia la contraseña de una persona del hogar (guarda hash). */
    public Persona setClave(Long id, String nuevaClave) {
        validarClave(nuevaClave);
        return repository.findByIdAndHogar(id, HogarActual.get()).map(p -> {
            p.setClave(passwordEncoder.encode(nuevaClave));
            return repository.save(p);
        }).orElse(null);
    }

    /**
     * Agrega una persona al hogar. Nace sin contraseña: no puede entrar hasta que
     * alguien del hogar le ponga una. Antes nacia con "123", que ahora dejaria a
     * cualquiera que adivine un nombre entrar a un hogar ajeno.
     */
    public Persona createPersona(String nombre) {
        Persona p = new Persona();
        p.setNombre(nombreLibre(nombre));
        p.setHogar(HogarActual.get());
        return repository.save(p);
    }

    /** Crea una cuenta nueva: un hogar vacio y su primera persona, ya con contraseña. */
    public Persona registrar(String nombre, String clave) {
        return registrar(nombre, clave, null);
    }

    /**
     * Con un código de invitación la persona nueva se suma a ese hogar en vez de
     * crear uno: ve y carga lo mismo que los demás. Solo para cuentas nuevas, así
     * nadie deja atrás datos propios.
     */
    @Transactional
    public Persona registrar(String nombre, String clave, String codigoInvitacion) {
        validarClave(clave);
        Persona p = new Persona();
        p.setNombre(nombreLibre(nombre));
        p.setClave(passwordEncoder.encode(clave));
        p.setHogar(codigoInvitacion == null || codigoInvitacion.isBlank()
                ? hogarRepository.save(new Hogar())
                : usarInvitacion(codigoInvitacion));
        return repository.save(p);
    }

    /** Genera un código de un solo uso para sumar a alguien al hogar de quien lo pide. */
    public Invitacion crearInvitacion() {
        Invitacion inv = new Invitacion();
        inv.setHogar(HogarActual.get());
        inv.setExpiraEn(LocalDateTime.now().plusHours(HORAS_INVITACION));
        String codigo;
        do { codigo = codigoAlAzar(); } while (invitacionRepository.existsByCodigo(codigo));
        inv.setCodigo(codigo);
        return invitacionRepository.save(inv);
    }

    private Hogar usarInvitacion(String codigoTipeado) {
        // Acepta "k7p-3qx" o con espacios: se compara solo letras y números en mayúscula.
        String codigo = codigoTipeado.toUpperCase().replaceAll("[^A-Z0-9]", "");
        Invitacion inv = invitacionRepository.findByCodigo(codigo)
                .filter(i -> i.getUsadaEn() == null && i.getExpiraEn().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ValidacionException("El código de invitación no es válido o ya venció."));
        inv.setUsadaEn(LocalDateTime.now());
        invitacionRepository.save(inv);
        return inv.getHogar();
    }

    private static String codigoAlAzar() {
        StringBuilder sb = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) sb.append(ALFABETO_CODIGO.charAt(AZAR.nextInt(ALFABETO_CODIGO.length())));
        return sb.toString();
    }

    /**
     * El login es solo por nombre, asi que tiene que ser unico entre todos los hogares.
     * Las personas sin hogar son datos de antes de las cuentas: no entran ni ocupan nombre.
     */
    private String nombreLibre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) throw new ValidacionException("El nombre no puede estar vacío.");
        if (repository.existsByNombreIgnoreCaseAndHogarIsNotNull(limpio)) throw new ValidacionException("Ese nombre ya está en uso.");
        return limpio;
    }

    private void validarClave(String clave) {
        if (clave == null || clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ValidacionException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
    }
}
