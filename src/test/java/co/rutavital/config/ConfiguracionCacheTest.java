package co.rutavital.config;

import co.rutavital.evento.BitacoraEventos;
import co.rutavital.evento.CentroEstadoVial;
import co.rutavital.evento.InvalidadorCache;
import co.rutavital.evento.ObservadorVial;
import co.rutavital.evento.RecalculadorDespachos;
import co.rutavital.servicio.NavegadorRutas;
import co.rutavital.servicio.ServicioRutas;
import co.rutavital.servicio.ServicioRutasConCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Verifica el cableado según la propiedad rutavital.cache.habilitada.
 */
class ConfiguracionCacheTest {

    private static List<Class<?>> tipos(List<ObservadorVial> observadores) {
        return observadores.stream().<Class<?>>map(Object::getClass).toList();
    }

    @Nested
    @SpringBootTest(properties = "rutavital.cache.habilitada=true")
    class ConCache {

        @Autowired
        private ServicioRutas servicioRutas;
        @Autowired
        private CentroEstadoVial centro;

        @Test
        @DisplayName("El ServicioRutas principal es el proxy y los observadores van en el orden correcto")
        void usaElProxy() {
            assertInstanceOf(ServicioRutasConCache.class, servicioRutas);
            assertEquals(List.of(InvalidadorCache.class, RecalculadorDespachos.class, BitacoraEventos.class),
                    tipos(centro.getObservadores()));
        }
    }

    @Nested
    @SpringBootTest(properties = "rutavital.cache.habilitada=false")
    @AutoConfigureMockMvc
    class SinCache {

        @Autowired
        private ServicioRutas servicioRutas;
        @Autowired
        private CentroEstadoVial centro;
        @Autowired
        private ApplicationContext contexto;
        @Autowired
        private MockMvc mvc;

        @Test
        @DisplayName("El ServicioRutas principal es NavegadorRutas y no hay proxy ni invalidador")
        void usaElNavegadorDirecto() throws Exception {
            assertInstanceOf(NavegadorRutas.class, servicioRutas);
            assertTrue(contexto.getBeansOfType(ServicioRutasConCache.class).isEmpty());
            assertEquals(List.of(RecalculadorDespachos.class, BitacoraEventos.class), tipos(centro.getObservadores()));

            mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"));
            mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"))
                    .andExpect(jsonPath("$.metricas.desdeCache").value(false));
            mvc.perform(get("/api/metricas/cache")).andExpect(jsonPath("$.habilitada").value(false));
        }
    }
}
