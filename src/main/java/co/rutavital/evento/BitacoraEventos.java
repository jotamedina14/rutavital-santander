package co.rutavital.evento;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observador que guarda en memoria los eventos viales para mostrarlos.
 */
@Component
public class BitacoraEventos implements ObservadorVial {

    private final List<EventoVial> eventos = new CopyOnWriteArrayList<>();

    @Override
    public void alCambiarEstado(EventoVial evento) {
        eventos.add(evento);
    }

    /**
     * Eventos del más reciente al más antiguo.
     */
    public List<EventoVial> getEventos() {
        List<EventoVial> copia = new ArrayList<>(eventos);
        Collections.reverse(copia);
        return copia;
    }

    public int cantidad() {
        return eventos.size();
    }
}
