package co.rutavital.servicio;

import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.excepcion.DespachoNoEncontradoException;
import co.rutavital.modelo.Despacho;
import co.rutavital.modelo.Ruta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Administra los despachos de ambulancias en memoria.
 */
@Service
public class GestorDespachos {

    private static final Logger log = LoggerFactory.getLogger(GestorDespachos.class);

    private final ServicioRutas servicioRutas;
    private final Map<String, Despacho> despachos = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicInteger secuencia = new AtomicInteger();

    public GestorDespachos(ServicioRutas servicioRutas) {
        this.servicioRutas = servicioRutas;
    }

    public Despacho crear(String origenId, String destinoId) {
        return crear(origenId, destinoId, null);
    }

    /**
     * Calcula la ruta inicial y registra el despacho como ACTIVO. Si no hay ruta
     * posible, el despacho se crea igual (con encontrada=false) y se recalculará
     * cuando se reabra algún tramo.
     */
    public Despacho crear(String origenId, String destinoId, String algoritmo) {
        Ruta ruta = servicioRutas.calcularRuta(origenId, destinoId, FabricaEstrategias.normalizar(algoritmo));
        String id = String.format("D-%03d", secuencia.incrementAndGet());
        Despacho despacho = new Despacho(id, origenId, destinoId, ruta);
        despachos.put(id, despacho);
        log.info("Despacho {} creado: {} -> {} ({} min)", id, origenId, destinoId, ruta.getTiempoTotalMin());
        return despacho;
    }

    public List<Despacho> listarActivos() {
        return listarTodos().stream().filter(Despacho::estaActivo).toList();
    }

    public List<Despacho> listarTodos() {
        synchronized (despachos) {
            return new ArrayList<>(despachos.values());
        }
    }

    public Optional<Despacho> buscar(String id) {
        return Optional.ofNullable(despachos.get(id));
    }

    /**
     * Marca el despacho como FINALIZADO. Es idempotente.
     */
    public Despacho finalizar(String id) {
        Despacho despacho = buscar(id).orElseThrow(() -> new DespachoNoEncontradoException(id));
        despacho.finalizar();
        return despacho;
    }
}
