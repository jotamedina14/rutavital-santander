package co.rutavital.evento;

/**
 * Patrón Observer: interesado en los cambios de estado de los tramos.
 */
public interface ObservadorVial {

    void alCambiarEstado(EventoVial evento);
}
