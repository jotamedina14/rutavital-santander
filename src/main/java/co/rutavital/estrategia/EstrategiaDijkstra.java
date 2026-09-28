package co.rutavital.estrategia;

import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.modelo.Tramo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra con cola de prioridad. El peso de cada tramo es su tiempo estimado
 * en minutos; los tramos CERRADOS se ignoran.
 */
public class EstrategiaDijkstra implements EstrategiaNavegacion {

    public static final String NOMBRE = "DIJKSTRA";

    private record NodoEnCola(String municipioId, double costoMin) {
    }

    @Override
    public Ruta calcular(RedVial red, String origenId, String destinoId) {
        long inicio = System.nanoTime();
        ConstructorRuta.validarExtremos(red, origenId, destinoId);

        Map<String, Double> costoMinimo = new HashMap<>();
        Map<String, Tramo> tramoPrevio = new HashMap<>();
        PriorityQueue<NodoEnCola> cola = new PriorityQueue<>(Comparator.comparingDouble(NodoEnCola::costoMin));
        costoMinimo.put(origenId, 0.0);
        cola.add(new NodoEnCola(origenId, 0.0));
        int nodosExplorados = 0;

        while (!cola.isEmpty()) {
            NodoEnCola actual = cola.poll();
            if (actual.costoMin() > costoMinimo.get(actual.municipioId())) {
                continue; // entrada obsoleta: ya se encontró un camino mejor a este nodo
            }
            nodosExplorados++;
            if (actual.municipioId().equals(destinoId)) {
                break;
            }
            for (Tramo tramo : red.vecinos(actual.municipioId())) {
                if (!tramo.esTransitable()) {
                    continue;
                }
                String vecino = tramo.otroExtremo(actual.municipioId());
                double nuevoCosto = actual.costoMin() + tramo.tiempoEstimadoMin();
                if (nuevoCosto < costoMinimo.getOrDefault(vecino, Double.POSITIVE_INFINITY)) {
                    costoMinimo.put(vecino, nuevoCosto);
                    tramoPrevio.put(vecino, tramo);
                    cola.add(new NodoEnCola(vecino, nuevoCosto));
                }
            }
        }
        return ConstructorRuta.construir(red, origenId, destinoId, tramoPrevio, NOMBRE, nodosExplorados, inicio);
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }
}
