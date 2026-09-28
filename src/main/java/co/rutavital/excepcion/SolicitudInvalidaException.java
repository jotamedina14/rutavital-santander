package co.rutavital.excepcion;

/**
 * Error por datos de entrada inválidos (se responde con 400).
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
