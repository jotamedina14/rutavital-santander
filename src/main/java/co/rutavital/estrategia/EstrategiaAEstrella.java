package co.rutavital.estrategia;

import co.rutavital.modelo.DistanciaGeografica;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.modelo.TipoVia;
import co.rutavital.modelo.Tramo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* con heurística = distancia haversine en km / 60 km/h * 60 (minutos).
 * <p>
 * La heurística es admisible: ningún tramo es más corto que la línea recta entre
 * sus extremos y 60 km/h es la velocidad máxima de la red, así que nunca
 * sobreestima el tiempo real restante. Por eso A* encuentra el mismo tiempo
 * óptimo que Dijkstra explorando menos nodos.
 */
public class EstrategiaAEstrella implements EstrategiaNavegacion {

    public static final String NOMBRE = "A_ESTRELLA";

    private record NodoEnCola(String municipioId, double costoMin, double prioridad) {
    }

    @Override
    public Ruta calcular(RedVial red, String origenId, String destinoId) {
        long inicio = System.nanoTime();
        ConstructorRuta.validarExtremos(red, origenId, destinoId);
        Municipio destino = red.buscarMunicipio(destinoId).orElseThrow();

        Map<String, Double> costoMinimo = new HashMap<>();
        Map<String, Tramo> tramoPrevio = new HashMap<>();
        // Prioridad f = g + h; en empate se prefiere el nodo con mayor g (más cerca del destino)
        PriorityQueue<NodoEnCola> cola = new PriorityQueue<>(Comparator
                .comparingDouble(NodoEnCola::prioridad)
                .thenComparing(Comparator.comparingDouble(NodoEnCola::costoMin).reversed()));
        costoMinimo.put(origenId, 0.0);
        cola.add(new NodoEnCola(origenId, 0.0, heuristicaMin(red, origenId, destino)));
        int nodosExplorados = 0;

        while (!cola.isEmpty()) {
            NodoEnCola actual = cola.poll();
            if (actual.costoMin() > costoMinimo.get(actual.municipioId())) {
                continue; // entrada obsoleta
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
                    cola.add(new NodoEnCola(vecino, nuevoCosto, nuevoCosto + heuristicaMin(red, vecino, destino)));
                }
            }
        }
        return ConstructorRuta.construir(red, origenId, destinoId, tramoPrevio, NOMBRE, nodosExplorados, inicio);
    }

    /**
     * Cota inferior del tiempo (min) desde el municipio hasta el destino:
     * recorrer la línea recta a la velocidad máxima de la red.
     */
    static double heuristicaMin(RedVial red, String municipioId, Municipio destino) {
        Municipio desde = red.buscarMunicipio(municipioId).orElseThrow();
        return DistanciaGeografica.haversineKm(desde, destino) / TipoVia.velocidadMaximaKmH() * 60;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }
}
