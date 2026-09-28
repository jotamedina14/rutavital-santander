package co.rutavital.api;

import co.rutavital.api.dto.ComparacionRutas;
import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.modelo.Ruta;
import co.rutavital.servicio.NavegadorRutas;
import co.rutavital.servicio.ServicioRutas;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    /** Servicio principal: el proxy con caché o el navegador directo, según la configuración. */
    private final ServicioRutas servicioRutas;
    /** Navegador sin caché, para comparar algoritmos con tiempos de cálculo reales. */
    private final NavegadorRutas navegador;
    private final FabricaEstrategias fabrica;

    public RutaController(ServicioRutas servicioRutas, NavegadorRutas navegador, FabricaEstrategias fabrica) {
        this.servicioRutas = servicioRutas;
        this.navegador = navegador;
        this.fabrica = fabrica;
    }

    /**
     * Si no hay ruta posible responde 200 con encontrada=false y un mensaje.
     */
    @GetMapping
    public Ruta calcular(@RequestParam String origen,
                         @RequestParam String destino,
                         @RequestParam(defaultValue = "DIJKSTRA") String algoritmo) {
        return servicioRutas.calcularRuta(Parametros.id(origen, "origen"), Parametros.id(destino, "destino"), algoritmo);
    }

    /**
     * Ejecuta las tres estrategias sin pasar por la caché.
     */
    @GetMapping("/comparar")
    public ComparacionRutas comparar(@RequestParam String origen, @RequestParam String destino) {
        String origenId = Parametros.id(origen, "origen");
        String destinoId = Parametros.id(destino, "destino");
        List<Ruta> rutas = fabrica.nombresDisponibles().stream()
                .map(algoritmo -> navegador.calcularRuta(origenId, destinoId, algoritmo))
                .toList();
        return new ComparacionRutas(origenId, destinoId, rutas);
    }
}
