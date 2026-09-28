package co.rutavital.evento;

import co.rutavital.excepcion.TramoNoEncontradoException;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Tramo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sujeto del patrón Observer. Es el único punto por el que cambia el estado de
 * un tramo; después de actualizarlo notifica a los observadores en el orden en
 * que se suscribieron.
 */
public class CentroEstadoVial {

    private static final Logger log = LoggerFactory.getLogger(CentroEstadoVial.class);

    private final RedVial red;
    private final List<ObservadorVial> observadores = new CopyOnWriteArrayList<>();

    public CentroEstadoVial(RedVial red) {
        this.red = red;
    }

    public void suscribir(ObservadorVial observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void desuscribir(ObservadorVial observador) {
        observadores.remove(observador);
    }

    /**
     * Actualiza el estado del tramo y notifica a todos los observadores.
     * Los cambios se serializan para que cada evento se procese completo antes del siguiente.
     *
     * @return el evento generado, o vacío si el tramo ya estaba en ese estado
     */
    public synchronized Optional<EventoVial> cambiarEstado(String tramoId, EstadoTramo nuevoEstado, String motivo) {
        Tramo tramo = red.buscarTramo(tramoId).orElseThrow(() -> new TramoNoEncontradoException(tramoId));
        EstadoTramo anterior = tramo.getEstado();
        if (anterior == nuevoEstado) {
            return Optional.empty();
        }
        tramo.setEstado(nuevoEstado);
        EventoVial evento = new EventoVial(tramoId, anterior, nuevoEstado, motivo, LocalDateTime.now());
        log.info("Cambio de estado: {}", evento);
        notificar(evento);
        return Optional.of(evento);
    }

    public EstadoTramo estadoDe(String tramoId) {
        return red.buscarTramo(tramoId).orElseThrow(() -> new TramoNoEncontradoException(tramoId)).getEstado();
    }

    public List<ObservadorVial> getObservadores() {
        return List.copyOf(observadores);
    }

    private void notificar(EventoVial evento) {
        for (ObservadorVial observador : observadores) {
            try {
                observador.alCambiarEstado(evento);
            } catch (RuntimeException e) {
                // Un observador con error no debe impedir que los demás se enteren
                log.error("El observador {} falló al procesar {}", observador.getClass().getSimpleName(), evento, e);
            }
        }
    }
}
