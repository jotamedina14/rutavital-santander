package co.rutavital.api;

import co.rutavital.api.dto.MetricasCacheDTO;
import co.rutavital.servicio.ServicioRutasConCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metricas")
public class MetricasController {

    /** Puede no existir si rutavital.cache.habilitada=false. */
    private final ObjectProvider<ServicioRutasConCache> cache;

    public MetricasController(ObjectProvider<ServicioRutasConCache> cache) {
        this.cache = cache;
    }

    @GetMapping("/cache")
    public MetricasCacheDTO cache() {
        ServicioRutasConCache proxy = cache.getIfAvailable();
        if (proxy == null) {
            return new MetricasCacheDTO(false, 0, 0, 0.0, 0);
        }
        return new MetricasCacheDTO(true, proxy.getHits(), proxy.getMisses(), proxy.getTasaAcierto(), proxy.getEntradas());
    }
}
