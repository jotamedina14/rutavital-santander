package co.rutavital.modelo;

import java.util.Objects;

/**
 * Tramo vial entre dos municipios. Es una arista no dirigida del grafo:
 * se puede recorrer en ambos sentidos.
 */
public class Tramo {

    private final String id;
    private final String origenId;
    private final String destinoId;
    private final double distanciaKm;
    private final TipoVia tipoVia;
    /** Volátil porque el estado cambia en tiempo de ejecución mientras otras peticiones calculan rutas. */
    private volatile EstadoTramo estado;

    public Tramo(String id, String origenId, String destinoId, double distanciaKm,
                 TipoVia tipoVia, EstadoTramo estado) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del tramo es obligatorio");
        }
        if (distanciaKm <= 0) {
            throw new IllegalArgumentException("La distancia del tramo debe ser positiva: " + id);
        }
        if (Objects.equals(origenId, destinoId)) {
            throw new IllegalArgumentException("Un tramo no puede unir un municipio consigo mismo: " + id);
        }
        this.id = id;
        this.origenId = Objects.requireNonNull(origenId);
        this.destinoId = Objects.requireNonNull(destinoId);
        this.distanciaKm = distanciaKm;
        this.tipoVia = Objects.requireNonNull(tipoVia);
        this.estado = Objects.requireNonNull(estado);
    }

    public Tramo(String id, String origenId, String destinoId, double distanciaKm, TipoVia tipoVia) {
        this(id, origenId, destinoId, distanciaKm, tipoVia, EstadoTramo.ABIERTO);
    }

    /**
     * Tiempo estimado de recorrido en minutos:
     * distanciaKm / velocidad del tipo de vía * 60 * factor del estado.
     * Un tramo CERRADO devuelve infinito (no transitable).
     */
    public double tiempoEstimadoMin() {
        EstadoTramo actual = estado;
        if (!actual.esTransitable()) {
            return Double.POSITIVE_INFINITY;
        }
        double tiempoBase = distanciaKm / tipoVia.getVelocidadKmH() * 60;
        return tiempoBase * actual.getFactor();
    }

    public boolean esTransitable() {
        return estado.esTransitable();
    }

    public boolean conecta(String municipioId) {
        return origenId.equals(municipioId) || destinoId.equals(municipioId);
    }

    /**
     * Devuelve el municipio del otro extremo del tramo.
     */
    public String otroExtremo(String municipioId) {
        if (origenId.equals(municipioId)) {
            return destinoId;
        }
        if (destinoId.equals(municipioId)) {
            return origenId;
        }
        throw new IllegalArgumentException("El tramo " + id + " no conecta con " + municipioId);
    }

    public String getId() {
        return id;
    }

    public String getOrigenId() {
        return origenId;
    }

    public String getDestinoId() {
        return destinoId;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public TipoVia getTipoVia() {
        return tipoVia;
    }

    public EstadoTramo getEstado() {
        return estado;
    }

    public void setEstado(EstadoTramo estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tramo otro)) {
            return false;
        }
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + " " + origenId + "-" + destinoId + " (" + estado + ")";
    }
}
