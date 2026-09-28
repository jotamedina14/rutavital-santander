package co.rutavital.modelo;

import java.util.List;

/**
 * Resultado de calcular una ruta. Es inmutable para poder compartirla
 * (por ejemplo, desde la caché) sin riesgo.
 */
public class Ruta {

    private final List<String> municipios;
    private final List<String> tramos;
    private final double distanciaTotalKm;
    private final double tiempoTotalMin;
    private final String algoritmo;
    private final MetricasBusqueda metricas;
    private final boolean encontrada;
    private final String mensaje;

    private Ruta(List<String> municipios, List<String> tramos, double distanciaTotalKm, double tiempoTotalMin,
                 String algoritmo, MetricasBusqueda metricas, boolean encontrada, String mensaje) {
        this.municipios = List.copyOf(municipios);
        this.tramos = List.copyOf(tramos);
        this.distanciaTotalKm = distanciaTotalKm;
        this.tiempoTotalMin = tiempoTotalMin;
        this.algoritmo = algoritmo;
        this.metricas = metricas;
        this.encontrada = encontrada;
        this.mensaje = mensaje;
    }

    public static Ruta exitosa(List<String> municipios, List<String> tramos, double distanciaTotalKm,
                               double tiempoTotalMin, String algoritmo, MetricasBusqueda metricas) {
        return new Ruta(municipios, tramos, distanciaTotalKm, tiempoTotalMin, algoritmo, metricas, true,
                "Ruta encontrada con " + tramos.size() + " tramo(s)");
    }

    public static Ruta sinRuta(String algoritmo, MetricasBusqueda metricas, String mensaje) {
        return new Ruta(List.of(), List.of(), 0, 0, algoritmo, metricas, false, mensaje);
    }

    /**
     * Copia de esta ruta con otras métricas (la usa el proxy de caché).
     */
    public Ruta conMetricas(MetricasBusqueda nuevasMetricas) {
        return new Ruta(municipios, tramos, distanciaTotalKm, tiempoTotalMin, algoritmo, nuevasMetricas,
                encontrada, mensaje);
    }

    public boolean contieneTramo(String tramoId) {
        return tramos.contains(tramoId);
    }

    /**
     * Dos rutas son equivalentes si recorren los mismos tramos con el mismo tiempo.
     */
    public boolean esEquivalenteA(Ruta otra) {
        return otra != null
                && encontrada == otra.encontrada
                && tramos.equals(otra.tramos)
                && Math.abs(tiempoTotalMin - otra.tiempoTotalMin) < 1e-9;
    }

    public List<String> getMunicipios() {
        return municipios;
    }

    public List<String> getTramos() {
        return tramos;
    }

    public double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }

    public double getTiempoTotalMin() {
        return tiempoTotalMin;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public MetricasBusqueda getMetricas() {
        return metricas;
    }

    public boolean isEncontrada() {
        return encontrada;
    }

    public String getMensaje() {
        return mensaje;
    }

    @Override
    public String toString() {
        return encontrada
                ? algoritmo + " " + municipios + " " + tramos + " " + distanciaTotalKm + " km, " + tiempoTotalMin + " min"
                : algoritmo + " sin ruta: " + mensaje;
    }
}
