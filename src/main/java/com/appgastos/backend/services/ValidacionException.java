package com.appgastos.backend.services;

/**
 * Un dato del usuario que no se puede aceptar. El mensaje está pensado para
 * mostrarse tal cual en la interfaz.
 */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
