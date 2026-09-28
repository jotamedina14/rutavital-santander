package co.rutavital.excepcion;

public class MunicipioNoEncontradoException extends RecursoNoEncontradoException {

    public MunicipioNoEncontradoException(String municipioId) {
        super("El municipio '" + municipioId + "' no existe en la red vial");
    }
}
