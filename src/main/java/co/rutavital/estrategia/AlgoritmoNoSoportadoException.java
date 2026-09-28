package co.rutavital.estrategia;

import co.rutavital.excepcion.SolicitudInvalidaException;

import java.util.Collection;

public class AlgoritmoNoSoportadoException extends SolicitudInvalidaException {

    public AlgoritmoNoSoportadoException(String nombre, Collection<String> disponibles) {
        super("Algoritmo no soportado: '" + nombre + "'. Valores permitidos: " + String.join(", ", disponibles));
    }
}
