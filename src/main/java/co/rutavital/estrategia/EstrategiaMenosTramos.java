package co.rutavital.estrategia;

import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.modelo.Tramo;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Búsqueda en anchura (BFS) que minimiza el número de tramos, sin mirar tiempos.
 * Sirve como línea base para comparar con Dijkstra y A*.
 */
public class EstrategiaMenosTramos implements EstrategiaNavegacion {

    public static final String NOMBRE = "MENOS_TRAMOS";

    @Override
    public Ruta calcular(RedVial red, String origenId, String destinoId) {
        long inicio = System.nanoTime();
        ConstructorRuta.validarExtremos(red, origenId, destinoId);

        Queue<String> cola = new ArrayDeque<>();
        Set<String> descubiertos = new HashSet<>();
        Map<String, Tramo> tramoPrevio = new HashMap<>();
        cola.add(origenId);
        descubiertos.add(origenId);
        int nodosExplorados = 0;

        while (!cola.isEmpty()) {
            String actual = cola.poll();
            nodosExplorados++;
            if (actual.equals(destinoId)) {
                break;
            }
            for (Tramo tramo : red.vecinos(actual)) {
                if (!tramo.esTransitable()) {
                    continue;
                }
                String vecino = tramo.otroExtremo(actual);
                if (descubiertos.add(vecino)) {
                    tramoPrevio.put(vecino, tramo);
                    cola.add(vecino);
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
