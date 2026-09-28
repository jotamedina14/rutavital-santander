package co.rutavital.servicio;

import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.modelo.MetricasBusqueda;
import co.rutavital.modelo.Ruta;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Patrón Proxy: envuelve a {@link NavegadorRutas} con la misma interfaz y guarda
 * las rutas calculadas en una caché con clave "origen|destino|algoritmo".
 * <p>
 * Solo existe si {@code rutavital.cache.habilitada=true}; en ese caso es el bean
 * {@link ServicioRutas} principal. Si la propiedad es false, el principal es
 * {@link NavegadorRutas} directamente.
 */
@Service
@Primary
@ConditionalOnProperty(name = "rutavital.cache.habilitada", havingValue = "true", matchIfMissing = true)
public class ServicioRutasConCache implements ServicioRutas {

    /**
     * Cada entrada recuerda la generación de la caché en la que se calculó. Si la red
     * cambia mientras se calcula una ruta, esa ruta queda con una generación vieja y no
     * se sirve después de invalidar.
     */
    private record EntradaCache(Ruta ruta, long generacion) {
    }

    private final NavegadorRutas navegador;
    private final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final AtomicLong generacion = new AtomicLong();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public ServicioRutasConCache(NavegadorRutas navegador) {
        this.navegador = navegador;
    }

    @Override
    public Ruta calcularRuta(String origenId, String destinoId, String algoritmo) {
        long inicio = System.nanoTime();
        String nombreAlgoritmo = FabricaEstrategias.normalizar(algoritmo);
        String clave = clave(origenId, destinoId, nombreAlgoritmo);
        long generacionActual = generacion.get();

        EntradaCache entrada = cache.get(clave);
        if (entrada != null && entrada.generacion() == generacionActual) {
            hits.incrementAndGet();
            // En un acierto no se explora ningún nodo; se informa el tiempo de responder desde la caché
            return entrada.ruta().conMetricas(new MetricasBusqueda(0, System.nanoTime() - inicio, true));
        }

        misses.incrementAndGet();
        Ruta ruta = navegador.calcularRuta(origenId, destinoId, nombreAlgoritmo);
        cache.put(clave, new EntradaCache(ruta, generacionActual));
        return ruta;
    }

    /**
     * Vacía la caché. Lo invoca el observador InvalidadorCache cuando cambia un tramo.
     */
    public void limpiar() {
        generacion.incrementAndGet();
        cache.clear();
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    /**
     * Proporción de aciertos entre 0 y 1 (0 si aún no hay consultas).
     */
    public double getTasaAcierto() {
        long total = hits.get() + misses.get();
        return total == 0 ? 0.0 : (double) hits.get() / total;
    }

    public int getEntradas() {
        return cache.size();
    }

    public void reiniciarContadores() {
        hits.set(0);
        misses.set(0);
    }

    public static String clave(String origenId, String destinoId, String algoritmo) {
        return origenId + "|" + destinoId + "|" + algoritmo;
    }
}
