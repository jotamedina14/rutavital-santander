package co.rutavital.modelo;

/**
 * Métricas de una búsqueda de ruta.
 * Cuando la ruta viene de la caché, las métricas describen esa consulta:
 * cero nodos explorados y el tiempo que tomó responder desde la caché.
 */
public class MetricasBusqueda {

    private final int nodosExplorados;
    private final long tiempoCalculoNanos;
    private final boolean desdeCache;

    public MetricasBusqueda(int nodosExplorados, long tiempoCalculoNanos, boolean desdeCache) {
        this.nodosExplorados = nodosExplorados;
        this.tiempoCalculoNanos = tiempoCalculoNanos;
        this.desdeCache = desdeCache;
    }

    public int getNodosExplorados() {
        return nodosExplorados;
    }

    public long getTiempoCalculoNanos() {
        return tiempoCalculoNanos;
    }

    public double getTiempoCalculoMs() {
        return tiempoCalculoNanos / 1_000_000.0;
    }

    public boolean isDesdeCache() {
        return desdeCache;
    }

    @Override
    public String toString() {
        return "nodos=" + nodosExplorados + ", ms=" + getTiempoCalculoMs() + ", cache=" + desdeCache;
    }
}
