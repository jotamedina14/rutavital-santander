package co.rutavital.estrategia;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Devuelve la estrategia de navegación a partir de su nombre:
 * DIJKSTRA, A_ESTRELLA o MENOS_TRAMOS. Las estrategias no guardan estado,
 * así que se reutiliza una sola instancia de cada una.
 */
@Component
public class FabricaEstrategias {

    private final Map<String, EstrategiaNavegacion> estrategias = Stream.of(
                    new EstrategiaDijkstra(),
                    new EstrategiaAEstrella(),
                    new EstrategiaMenosTramos())
            .collect(Collectors.toMap(EstrategiaNavegacion::nombre, Function.identity(),
                    (a, b) -> a, LinkedHashMap::new));

    public EstrategiaNavegacion obtener(String nombre) {
        EstrategiaNavegacion estrategia = estrategias.get(normalizar(nombre));
        if (estrategia == null) {
            throw new AlgoritmoNoSoportadoException(nombre, estrategias.keySet());
        }
        return estrategia;
    }

    public List<String> nombresDisponibles() {
        return List.copyOf(estrategias.keySet());
    }

    public List<EstrategiaNavegacion> todas() {
        return List.copyOf(estrategias.values());
    }

    /**
     * Acepta variantes como "a-estrella" o " dijkstra ".
     */
    public static String normalizar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return EstrategiaDijkstra.NOMBRE;
        }
        return nombre.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
