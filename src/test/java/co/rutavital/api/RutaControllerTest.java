package co.rutavital.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba 8 (parte 1): GET /api/rutas y /api/rutas/comparar con MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RutaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /api/rutas devuelve la ruta San Gil -> Bucaramanga por el Chicamocha")
    void calculaRuta() throws Exception {
        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA")
                        .param("algoritmo", "DIJKSTRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontrada").value(true))
                .andExpect(jsonPath("$.algoritmo").value("DIJKSTRA"))
                .andExpect(jsonPath("$.tramos", hasSize(7)))
                .andExpect(jsonPath("$.tramos[3]").value("T06"))
                .andExpect(jsonPath("$.municipios[0]").value("SAN_GIL"))
                .andExpect(jsonPath("$.tiempoTotalMin", closeTo(103.0, 1e-9)))
                .andExpect(jsonPath("$.distanciaTotalKm", closeTo(103.0, 1e-9)))
                .andExpect(jsonPath("$.metricas.desdeCache").value(false))
                .andExpect(jsonPath("$.metricas.nodosExplorados").isNumber());
    }

    @Test
    @DisplayName("La segunda consulta igual viene de la caché")
    void segundaConsultaDesdeCache() throws Exception {
        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"));

        mvc.perform(get("/api/rutas").param("origen", "san_gil").param("destino", "bucaramanga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas.desdeCache").value(true));

        mvc.perform(get("/api/metricas/cache"))
                .andExpect(jsonPath("$.habilitada").value(true))
                .andExpect(jsonPath("$.hits").value(1))
                .andExpect(jsonPath("$.misses").value(1))
                .andExpect(jsonPath("$.tasaAcierto").value(0.5));
    }

    @Test
    @DisplayName("Un municipio inexistente responde 404 con un mensaje claro")
    void municipioInexistente() throws Exception {
        mvc.perform(get("/api/rutas").param("origen", "MEDELLIN").param("destino", "BUCARAMANGA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("El municipio 'MEDELLIN' no existe en la red vial"));
    }

    @Test
    @DisplayName("Parámetros faltantes o algoritmo desconocido responden 400")
    void solicitudesInvalidas() throws Exception {
        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("destino")));

        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA")
                        .param("algoritmo", "BELLMAN_FORD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("A_ESTRELLA")));
    }

    @Test
    @DisplayName("Sin ruta posible responde 200 con encontrada=false y un mensaje")
    void sinRutaPosible() throws Exception {
        mvc.perform(post("/api/tramos/T20/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CERRADO\",\"motivo\":\"Puente caído\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "OIBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontrada").value(false))
                .andExpect(jsonPath("$.tramos", hasSize(0)))
                .andExpect(jsonPath("$.mensaje", containsString("No existe una ruta transitable")));
    }

    @Test
    @DisplayName("GET /api/rutas/comparar ejecuta las 3 estrategias sin caché")
    void comparaAlgoritmos() throws Exception {
        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"));

        mvc.perform(get("/api/rutas/comparar").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutas", hasSize(3)))
                .andExpect(jsonPath("$.rutas[*].algoritmo", hasItem("MENOS_TRAMOS")))
                .andExpect(jsonPath("$.rutas[*].metricas.desdeCache", not(hasItem(true))))
                .andExpect(jsonPath("$.rutas[0].tiempoTotalMin", closeTo(103.0, 1e-9)))
                .andExpect(jsonPath("$.rutas[1].tiempoTotalMin", closeTo(103.0, 1e-9)));
    }
}
