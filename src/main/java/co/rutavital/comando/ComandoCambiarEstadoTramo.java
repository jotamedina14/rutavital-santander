package co.rutavital.comando;

import co.rutavital.evento.CentroEstadoVial;
import co.rutavital.evento.EventoVial;
import co.rutavital.modelo.EstadoTramo;

/**
 * Cambia el estado de un tramo a través del {@link CentroEstadoVial} (que notifica
 * a los observadores) y guarda el estado anterior para poder deshacer.
 */
public class ComandoCambiarEstadoTramo implements Comando {

    private final CentroEstadoVial centroEstadoVial;
    private final String tramoId;
    private final EstadoTramo nuevoEstado;
    private final String motivo;
    private EstadoTramo estadoAnterior;

    public ComandoCambiarEstadoTramo(CentroEstadoVial centroEstadoVial, String tramoId,
                                     EstadoTramo nuevoEstado, String motivo) {
        this.centroEstadoVial = centroEstadoVial;
        this.tramoId = tramoId;
        this.nuevoEstado = nuevoEstado;
        this.motivo = motivo;
    }

    @Override
    public void ejecutar() {
        // El estado anterior se toma del evento para que lectura y cambio sean atómicos;
        // si el tramo ya estaba en ese estado no hay evento y deshacer no cambia nada.
        estadoAnterior = centroEstadoVial.cambiarEstado(tramoId, nuevoEstado, motivo)
                .map(EventoVial::getEstadoAnterior)
                .orElse(nuevoEstado);
    }

    @Override
    public void deshacer() {
        if (estadoAnterior == null) {
            throw new IllegalStateException("No se puede deshacer un comando que no se ha ejecutado");
        }
        centroEstadoVial.cambiarEstado(tramoId, estadoAnterior, "Deshacer: " + motivo);
    }

    @Override
    public String descripcion() {
        String desde = estadoAnterior == null ? "?" : estadoAnterior.name();
        return "Tramo " + tramoId + ": " + desde + " → " + nuevoEstado + " (" + motivo + ")";
    }

    public String getTramoId() {
        return tramoId;
    }

    public EstadoTramo getNuevoEstado() {
        return nuevoEstado;
    }

    public EstadoTramo getEstadoAnterior() {
        return estadoAnterior;
    }
}
