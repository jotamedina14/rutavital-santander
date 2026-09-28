package co.rutavital.servicio;

import co.rutavital.estrategia.EstrategiaNavegacion;
import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import org.springframework.stereotype.Service;

/**
 * Contexto del patrón Strategy: recibe el nombre del algoritmo, pide la
 * estrategia a la {@link FabricaEstrategias} y le delega el cálculo sobre la red.
 * Siempre calcula (no usa caché).
 */
@Service
public class NavegadorRutas implements ServicioRutas {

    private final RedVial red;
    private final FabricaEstrategias fabrica;

    public NavegadorRutas(RedVial red, FabricaEstrategias fabrica) {
        this.red = red;
        this.fabrica = fabrica;
    }

    @Override
    public Ruta calcularRuta(String origenId, String destinoId, String algoritmo) {
        EstrategiaNavegacion estrategia = fabrica.obtener(algoritmo);
        return estrategia.calcular(red, origenId, destinoId);
    }

    public RedVial getRedVial() {
        return red;
    }
}
