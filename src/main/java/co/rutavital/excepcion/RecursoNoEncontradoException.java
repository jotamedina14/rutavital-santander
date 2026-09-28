package co.rutavital.excepcion;

/**
 * Base de los errores por recursos inexistentes (se responden con 404).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
