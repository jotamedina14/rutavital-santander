package co.rutavital.excepcion;

public class DespachoNoEncontradoException extends RecursoNoEncontradoException {

    public DespachoNoEncontradoException(String despachoId) {
        super("El despacho '" + despachoId + "' no existe");
    }
}
