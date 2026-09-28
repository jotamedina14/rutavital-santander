package co.rutavital.config;

import co.rutavital.comando.InvocadorComandos;
import co.rutavital.evento.BitacoraEventos;
import co.rutavital.evento.CentroEstadoVial;
import co.rutavital.evento.InvalidadorCache;
import co.rutavital.evento.RecalculadorDespachos;
import co.rutavital.modelo.RedVial;
import co.rutavital.repositorio.RepositorioRedVial;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de beans que no se descubren por anotación.
 */
@Configuration
public class ConfiguracionRutaVital {

    @Bean
    public RedVial redVial(RepositorioRedVial repositorio) {
        return repositorio.getRedVial();
    }

    /**
     * Sujeto del Observer con los observadores en orden: primero se invalida la caché,
     * luego se recalculan los despachos (así no usan rutas viejas de la caché) y por
     * último se registra el evento en la bitácora.
     */
    @Bean
    public CentroEstadoVial centroEstadoVial(RedVial red,
                                             ObjectProvider<InvalidadorCache> invalidadorCache,
                                             RecalculadorDespachos recalculadorDespachos,
                                             BitacoraEventos bitacoraEventos) {
        CentroEstadoVial centro = new CentroEstadoVial(red);
        invalidadorCache.ifAvailable(centro::suscribir);
        centro.suscribir(recalculadorDespachos);
        centro.suscribir(bitacoraEventos);
        return centro;
    }

    @Bean
    public InvocadorComandos invocadorComandos() {
        return new InvocadorComandos();
    }
}
