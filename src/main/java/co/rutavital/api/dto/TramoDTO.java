package co.rutavital.api.dto;

import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.TipoVia;
import co.rutavital.modelo.Tramo;

/**
 * Vista de un tramo para la API. El tiempo estimado es null si el tramo está cerrado.
 */
public record TramoDTO(String id, String origenId, String destinoId, double distanciaKm, TipoVia tipoVia,
                       int velocidadKmH, EstadoTramo estado, Double tiempoEstimadoMin) {

    public static TramoDTO desde(Tramo tramo) {
        double tiempo = tramo.tiempoEstimadoMin();
        return new TramoDTO(tramo.getId(), tramo.getOrigenId(), tramo.getDestinoId(), tramo.getDistanciaKm(),
                tramo.getTipoVia(), tramo.getTipoVia().getVelocidadKmH(), tramo.getEstado(),
                Double.isInfinite(tiempo) ? null : tiempo);
    }
}
