package co.rutavital.benchmark;

import co.rutavital.modelo.DistanciaGeografica;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.TipoVia;
import co.rutavital.modelo.Tramo;

import java.util.Random;

/**
 * Genera redes sintéticas tipo cuadrícula para el benchmark.
 * <ul>
 *     <li>Nodos separados ~1,1 km (0,01°) con un desplazamiento aleatorio de hasta ±0,003°.</li>
 *     <li>Cada nodo se une con su vecino de la derecha y el de abajo.</li>
 *     <li>La distancia de cada tramo es la línea recta multiplicada por un factor aleatorio en [1,0; 1,5),
 *     así nunca es menor que la haversine y la heurística de A* sigue siendo admisible.</li>
 *     <li>Tipo de vía: 30 % principal, 50 % secundaria, 20 % destapada.
 *     Estado: 90 % abierto, 7 % restringido, 3 % cerrado.</li>
 * </ul>
 * Con la misma semilla siempre se genera la misma red.
 */
public final class GeneradorRedSintetica {

    private static final double LATITUD_BASE = 6.0;
    private static final double LONGITUD_BASE = -74.0;
    private static final double PASO_GRADOS = 0.01;
    private static final double DESPLAZAMIENTO_MAXIMO = 0.003;

    private GeneradorRedSintetica() {
    }

    public static RedVial cuadricula(int filas, int columnas, long semilla) {
        Random azar = new Random(semilla);
        RedVial red = new RedVial();
        Municipio[][] nodos = new Municipio[filas][columnas];
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                double lat = LATITUD_BASE + f * PASO_GRADOS + desplazamiento(azar);
                double lon = LONGITUD_BASE + c * PASO_GRADOS + desplazamiento(azar);
                String id = idNodo(f, c);
                nodos[f][c] = new Municipio(id, id, lat, lon, false, 0);
                red.agregarMunicipio(nodos[f][c]);
            }
        }
        int contador = 0;
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (c + 1 < columnas) {
                    red.agregarTramo(tramo("S" + (++contador), nodos[f][c], nodos[f][c + 1], azar));
                }
                if (f + 1 < filas) {
                    red.agregarTramo(tramo("S" + (++contador), nodos[f][c], nodos[f + 1][c], azar));
                }
            }
        }
        return red;
    }

    public static String idNodo(int fila, int columna) {
        return "N" + fila + "_" + columna;
    }

    private static double desplazamiento(Random azar) {
        return (azar.nextDouble() * 2 - 1) * DESPLAZAMIENTO_MAXIMO;
    }

    private static Tramo tramo(String id, Municipio a, Municipio b, Random azar) {
        double distanciaKm = DistanciaGeografica.haversineKm(a, b) * (1.0 + azar.nextDouble() * 0.5);
        double sorteoTipo = azar.nextDouble();
        TipoVia tipo = sorteoTipo < 0.30 ? TipoVia.PRINCIPAL : sorteoTipo < 0.80 ? TipoVia.SECUNDARIA : TipoVia.DESTAPADA;
        double sorteoEstado = azar.nextDouble();
        EstadoTramo estado = sorteoEstado < 0.90 ? EstadoTramo.ABIERTO
                : sorteoEstado < 0.97 ? EstadoTramo.RESTRINGIDO : EstadoTramo.CERRADO;
        return new Tramo(id, a.getId(), b.getId(), distanciaKm, tipo, estado);
    }
}
