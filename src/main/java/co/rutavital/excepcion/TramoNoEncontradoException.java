package co.rutavital.excepcion;

public class TramoNoEncontradoException extends RecursoNoEncontradoException {

    public TramoNoEncontradoException(String tramoId) {
        super("El tramo '" + tramoId + "' no existe en la red vial");
    }
}
