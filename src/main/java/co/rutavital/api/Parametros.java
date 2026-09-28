package co.rutavital.api;

import co.rutavital.excepcion.SolicitudInvalidaException;

import java.util.Locale;

/**
 * Normalización de los identificadores que llegan por la API ("san_gil" -> "SAN_GIL").
 */
final class Parametros {

    private Parametros() {
    }

    static String id(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new SolicitudInvalidaException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
