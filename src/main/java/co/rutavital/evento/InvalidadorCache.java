package co.rutavital.evento;

import co.rutavital.servicio.ServicioRutasConCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Observador que vacía la caché de rutas cuando cambia cualquier tramo.
 * Solo existe si la caché está habilitada.
 */
@Component
@ConditionalOnProperty(name = "rutavital.cache.habilitada", havingValue = "true", matchIfMissing = true)
public class InvalidadorCache implements ObservadorVial {

    private final ServicioRutasConCache cache;

    public InvalidadorCache(ServicioRutasConCache cache) {
        this.cache = cache;
    }

    @Override
    public void alCambiarEstado(EventoVial evento) {
        cache.limpiar();
    }
}
