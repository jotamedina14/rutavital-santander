package co.rutavital.evento;

import co.rutavital.modelo.EstadoTramo;

import java.time.LocalDateTime;

/**
 * Notificación de que un tramo cambió de estado.
 */
public class EventoVial {

    private final String tramoId;
    private final EstadoTramo estadoAnterior;
    private final EstadoTramo estadoNuevo;
    private final String motivo;
    private final LocalDateTime fechaHora;

    public EventoVial(String tramoId, EstadoTramo estadoAnterior, EstadoTramo estadoNuevo,
                      String motivo, LocalDateTime fechaHora) {
        this.tramoId = tramoId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.fechaHora = fechaHora;
    }

    /**
     * El tramo quedó en mejores condiciones que antes (se reabrió o se levantó una restricción).
     */
    public boolean esMejora() {
        return estadoNuevo.esMejorQue(estadoAnterior);
    }

    public String getTramoId() {
        return tramoId;
    }

    public EstadoTramo getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoTramo getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    @Override
    public String toString() {
        return tramoId + ": " + estadoAnterior + " -> " + estadoNuevo + " (" + motivo + ")";
    }
}
