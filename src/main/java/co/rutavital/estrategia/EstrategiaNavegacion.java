package co.rutavital.estrategia;

import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;

/**
 * Patrón Strategy: cada implementación es un algoritmo intercambiable
 * para calcular la ruta entre dos municipios.
 */
public interface EstrategiaNavegacion {

    Ruta calcular(RedVial red, String origenId, String destinoId);

    String nombre();
}
