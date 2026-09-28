package co.rutavital.modelo;

/**
 * Estado operativo de un tramo. El factor multiplica el tiempo de recorrido.
 */
public enum EstadoTramo {

    ABIERTO(1.0, "Abierto"),
    RESTRINGIDO(1.6, "Paso a un carril"),
    CERRADO(Double.POSITIVE_INFINITY, "No transitable");

    private final double factor;
    private final String descripcion;

    EstadoTramo(double factor, String descripcion) {
        this.factor = factor;
        this.descripcion = descripcion;
    }

    public double getFactor() {
        return factor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esTransitable() {
        return this != CERRADO;
    }

    /**
     * Indica si este estado permite recorrer el tramo más rápido que el otro
     * (por ejemplo, CERRADO -> RESTRINGIDO o RESTRINGIDO -> ABIERTO).
     */
    public boolean esMejorQue(EstadoTramo otro) {
        return this.factor < otro.factor;
    }
}
