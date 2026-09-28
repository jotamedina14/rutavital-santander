package co.rutavital.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba 8 (parte 2): POST /api/tramos/{id}/estado, deshacer, eventos y despachos con MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RedVialControllerTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions cambiarEstado(String tramoId, String json) throws Exception {
        return mvc.perform(post("/api/tramos/" + tramoId + "/estado")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("Expone los 22 municipios y los 25 tramos")
    void listaMunicipiosYTramos() throws Exception {
        mvc.perform(get("/api/municipios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(22)))
                .andExpect(jsonPath("$[0].id").value("BUCARAMANGA"))
                .andExpect(jsonPath("$[0].tieneHospital").value(true))
                .andExpect(jsonPath("$[0].nivelHospital").value(3));

        mvc.perform(get("/api/tramos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("$[0].estado").value("ABIERTO"))
                .andExpect(jsonPath("$[0].tiempoEstimadoMin").value(8.0));
    }

    @Test
    @DisplayName("POST /api/tramos/T06/estado cierra el tramo, registra el evento y la ruta lo evita")
    void cerrarTramo() throws Exception {
        cambiarEstado("T06", "{\"estado\":\"CERRADO\",\"motivo\":\"Derrumbe sector Pescadero\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramo.id").value("T06"))
                .andExpect(jsonPath("$.tramo.estado").value("CERRADO"))
                .andExpect(jsonPath("$.tramo.tiempoEstimadoMin", nullValue()))
                .andExpect(jsonPath("$.descripcion").value("Tramo T06: ABIERTO → CERRADO (Derrumbe sector Pescadero)"));

        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"))
                .andExpect(jsonPath("$.encontrada").value(true))
                .andExpect(jsonPath("$.tramos", not(hasItem("T06"))))
                .andExpect(jsonPath("$.municipios", hasItem("VILLANUEVA")))
                .andExpect(jsonPath("$.municipios", hasItem("LOS_SANTOS")));

        mvc.perform(get("/api/eventos"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tramoId").value("T06"))
                .andExpect(jsonPath("$[0].estadoAnterior").value("ABIERTO"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("CERRADO"))
                .andExpect(jsonPath("$[0].motivo").value("Derrumbe sector Pescadero"));

        mvc.perform(get("/api/comandos/historial"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo").value("ComandoCambiarEstadoTramo"));
    }

    @Test
    @DisplayName("RESTRINGIDO multiplica por 1.6 el tiempo que expone la API")
    void restringirTramo() throws Exception {
        cambiarEstado("T01", "{\"estado\":\"RESTRINGIDO\",\"motivo\":\"Obras\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramo.estado").value("RESTRINGIDO"))
                .andExpect(jsonPath("$.tramo.tiempoEstimadoMin").value(8.0 * 1.6));
    }

    @Test
    @DisplayName("POST /api/comandos/deshacer reabre el tramo y la ruta vuelve a pasar por T06")
    void deshacerCierre() throws Exception {
        cambiarEstado("T06", "{\"estado\":\"CERRADO\",\"motivo\":\"Derrumbe\"}");

        mvc.perform(post("/api/comandos/deshacer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deshecho").value(true))
                .andExpect(jsonPath("$.descripcion", containsString("T06")));

        mvc.perform(get("/api/rutas").param("origen", "SAN_GIL").param("destino", "BUCARAMANGA"))
                .andExpect(jsonPath("$.tramos", hasItem("T06")));
        mvc.perform(get("/api/comandos/historial")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/comandos/deshacer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deshecho").value(false))
                .andExpect(jsonPath("$.mensaje").value("No hay acciones para deshacer"));
    }

    @Test
    @DisplayName("Estado inválido o cuerpo vacío responden 400; tramo inexistente responde 404")
    void erroresAlCambiarEstado() throws Exception {
        cambiarEstado("T06", "{\"estado\":\"INUNDADO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("Estado inválido")));
        cambiarEstado("T06", "no es json")
                .andExpect(status().isBadRequest());
        cambiarEstado("T99", "{\"estado\":\"CERRADO\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El tramo 'T99' no existe en la red vial"));
    }

    @Test
    @DisplayName("Un despacho activo se recalcula al cerrar T06 y se puede finalizar")
    void despachoSeRecalcula() throws Exception {
        mvc.perform(post("/api/despachos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"SAN_GIL\",\"destino\":\"BUCARAMANGA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("D-001"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rutaActual.tramos", hasItem("T06")));

        cambiarEstado("T06", "{\"estado\":\"CERRADO\",\"motivo\":\"Derrumbe\"}");

        mvc.perform(get("/api/despachos").param("soloActivos", "true"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vecesRecalculada").value(1))
                .andExpect(jsonPath("$[0].rutaActual.tramos", not(hasItem("T06"))))
                .andExpect(jsonPath("$[0].historialRutas[0].tramos", hasItem("T06")));

        mvc.perform(post("/api/despachos/D-001/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
        mvc.perform(get("/api/despachos").param("soloActivos", "true")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/despachos/D-999/finalizar")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Crear un despacho pasa por el invocador y se puede deshacer")
    void deshacerDespacho() throws Exception {
        mvc.perform(post("/api/despachos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"origen\":\"SAN_GIL\",\"destino\":\"BUCARAMANGA\"}"));

        mvc.perform(post("/api/comandos/deshacer"))
                .andExpect(jsonPath("$.descripcion").value("Crear despacho D-001 SAN_GIL → BUCARAMANGA"));
        mvc.perform(get("/api/despachos"))
                .andExpect(jsonPath("$[0].estado").value("FINALIZADO"));
    }
}
