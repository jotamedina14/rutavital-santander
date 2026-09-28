package co.rutavital.estrategia;

import co.rutavital.excepcion.MunicipioNoEncontradoException;
import co.rutavital.modelo.MetricasBusqueda;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.modelo.Tramo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utilidad compartida por las estrategias: valida los extremos y reconstruye
 * la {@link Ruta} a partir del árbol de tramos previos que deja la búsqueda.
 */
final class ConstructorRuta {

    private ConstructorRuta() {
    }

    static void validarExtremos(RedVial red, String origenId, String destinoId) {
        if (!red.existeMunicipio(origenId)) {
            throw new MunicipioNoEncontradoException(origenId);
        }
        if (!red.existeMunicipio(destinoId)) {
            throw new MunicipioNoEncontradoException(destinoId);
        }
    }

    /**
     * @param tramoPrevio para cada municipio alcanzado, el tramo por el que se llegó a él
     */
    static Ruta construir(RedVial red, String origenId, String destinoId, Map<String, Tramo> tramoPrevio,
                          String algoritmo, int nodosExplorados, long inicioNanos) {
        boolean alcanzado = origenId.equals(destinoId) || tramoPrevio.containsKey(destinoId);
        if (!alcanzado) {
            MetricasBusqueda metricas = new MetricasBusqueda(nodosExplorados, System.nanoTime() - inicioNanos, false);
            return Ruta.sinRuta(algoritmo, metricas, mensajeSinRuta(red, origenId, destinoId));
        }

        List<Tramo> camino = new ArrayList<>();
        String actual = destinoId;
        while (!actual.equals(origenId)) {
            Tramo tramo = tramoPrevio.get(actual);
            camino.add(tramo);
            actual = tramo.otroExtremo(actual);
        }
        Collections.reverse(camino);

        List<String> municipios = new ArrayList<>();
        List<String> tramos = new ArrayList<>();
        double distanciaKm = 0;
        double tiempoMin = 0;
        String posicion = origenId;
        municipios.add(posicion);
        for (Tramo tramo : camino) {
            tramos.add(tramo.getId());
            distanciaKm += tramo.getDistanciaKm();
            tiempoMin += tramo.tiempoEstimadoMin();
            posicion = tramo.otroExtremo(posicion);
            municipios.add(posicion);
        }
        MetricasBusqueda metricas = new MetricasBusqueda(nodosExplorados, System.nanoTime() - inicioNanos, false);
        return Ruta.exitosa(municipios, tramos, distanciaKm, tiempoMin, algoritmo, metricas);
    }

    private static String mensajeSinRuta(RedVial red, String origenId, String destinoId) {
        return "No existe una ruta transitable entre " + nombre(red, origenId) + " y " + nombre(red, destinoId)
                + ": todos los caminos posibles pasan por tramos cerrados.";
    }

    private static String nombre(RedVial red, String municipioId) {
        return red.buscarMunicipio(municipioId).map(Municipio::getNombre).orElse(municipioId);
    }
}
