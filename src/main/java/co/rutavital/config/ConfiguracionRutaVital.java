package co.rutavital.config;

import co.rutavital.modelo.RedVial;
import co.rutavital.repositorio.RepositorioRedVial;
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
}
