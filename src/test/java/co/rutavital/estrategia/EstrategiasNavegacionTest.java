package co.rutavital.estrategia;

import co.rutavital.excepcion.MunicipioNoEncontradoException;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.repositorio.RepositorioRedVial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstrategiasNavegacionTest {

    private static final double TOLERANCIA = 1e-9;
    private static final List<String> TRAMOS_RUTA_CHICAMOCHA = List.of("T09", "T08", "T07", "T06", "T05", "T02", "T01");

    private final FabricaEstrategias fabrica = new FabricaEstrategias();
    private final EstrategiaNavegacion dijkstra = new EstrategiaDijkstra();
    private final EstrategiaNavegacion aEstrella = new EstrategiaAEstrella();
    private final EstrategiaNavegacion menosTramos = new EstrategiaMenosTramos();
    private RedVial red;

    @BeforeEach
    void cargarRed() {
        red = new RepositorioRedVial().getRedVial();
    }

    @ParameterizedTest(name = "escenario: {0}")
    @ValueSource(strings = {"todo abierto", "T06 cerrado", "T05 restringido y T11 cerrado"})
    @DisplayName("Prueba 1: Dijkstra y A* dan el mismo tiempo total para todos los pares de la red real")
    void dijkstraYAEstrellaCoincidenEnTodosLosPares(String escenario) throws IOException {
        switch (escenario) {
            case "T06 cerrado" -> cambiar("T06", EstadoTramo.CERRADO);
            case "T05 restringido y T11 cerrado" -> {
                cambiar("T05", EstadoTramo.RESTRINGIDO);
                cambiar("T11", EstadoTramo.CERRADO);
            }
            default -> { }
        }

        long nodosDijkstra = 0;
        long nodosAEstrella = 0;
        int pares = 0;
        Path archivo = Path.of("target", "pruebas", "nodos-explorados-" + escenario.replace(' ', '-') + ".csv");
        Files.createDirectories(archivo.getParent());
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(archivo))) {
            csv.println("origen,destino,tiempoMin,nodosDijkstra,nodosAEstrella");
            for (Municipio origen : red.getMunicipios()) {
                for (Municipio destino : red.getMunicipios()) {
                    if (origen.equals(destino)) {
                        continue;
                    }
                    Ruta rutaDijkstra = dijkstra.calcular(red, origen.getId(), destino.getId());
                    Ruta rutaAEstrella = aEstrella.calcular(red, origen.getId(), destino.getId());

                    String par = origen.getId() + " -> " + destino.getId();
                    assertEquals(rutaDijkstra.isEncontrada(), rutaAEstrella.isEncontrada(), par);
                    assertEquals(rutaDijkstra.getTiempoTotalMin(), rutaAEstrella.getTiempoTotalMin(), TOLERANCIA, par);

                    int nd = rutaDijkstra.getMetricas().getNodosExplorados();
                    int na = rutaAEstrella.getMetricas().getNodosExplorados();
                    nodosDijkstra += nd;
                    nodosAEstrella += na;
                    pares++;
                    csv.printf(Locale.ROOT, "%s,%s,%.2f,%d,%d%n", origen.getId(), destino.getId(),
                            rutaDijkstra.getTiempoTotalMin(), nd, na);
                }
            }
        }
        System.out.printf(Locale.ROOT, "[%s] %d pares | nodos explorados Dijkstra: total=%d prom=%.2f | "
                        + "A*: total=%d prom=%.2f | detalle en %s%n",
                escenario, pares, nodosDijkstra, (double) nodosDijkstra / pares,
                nodosAEstrella, (double) nodosAEstrella / pares, archivo);
        assertEquals(22 * 21, pares);
        assertTrue(nodosAEstrella <= nodosDijkstra, "A* no debería explorar más nodos que Dijkstra en total");
    }

    @Test
    @DisplayName("La ruta normal San Gil -> Bucaramanga va por el Cañón del Chicamocha")
    void rutaNormalPorElChicamocha() {
        Ruta ruta = dijkstra.calcular(red, "SAN_GIL", "BUCARAMANGA");

        assertTrue(ruta.isEncontrada());
        assertEquals(TRAMOS_RUTA_CHICAMOCHA, ruta.getTramos());
        assertEquals(List.of("SAN_GIL", "CURITI", "ARATOCA", "PARQUE_CHICAMOCHA", "PESCADERO",
                "PIEDECUESTA", "FLORIDABLANCA", "BUCARAMANGA"), ruta.getMunicipios());
        assertEquals(103.0, ruta.getDistanciaTotalKm(), TOLERANCIA);
        assertEquals(103.0, ruta.getTiempoTotalMin(), TOLERANCIA);
    }

    @Test
    @DisplayName("Prueba 2: con T06 CERRADO la ruta San Gil -> Bucaramanga evita T06 y tarda más")
    void cierreDeT06ObligaARutaAlterna() {
        Ruta original = dijkstra.calcular(red, "SAN_GIL", "BUCARAMANGA");

        cambiar("T06", EstadoTramo.CERRADO);

        for (EstrategiaNavegacion estrategia : List.of(dijkstra, aEstrella)) {
            Ruta alterna = estrategia.calcular(red, "SAN_GIL", "BUCARAMANGA");
            assertTrue(alterna.isEncontrada());
            assertFalse(alterna.contieneTramo("T06"));
            assertTrue(alterna.getTiempoTotalMin() > original.getTiempoTotalMin());
            // La alterna sube por Villanueva y Los Santos
            assertEquals(List.of("T12", "T11", "T10", "T02", "T01"), alterna.getTramos());
            assertTrue(alterna.getMunicipios().containsAll(List.of("VILLANUEVA", "LOS_SANTOS")));
            assertEquals(215.7, alterna.getTiempoTotalMin(), TOLERANCIA);
        }
    }

    @Test
    @DisplayName("Prueba 7: si se cierran todos los tramos de Oiba no hay ruta posible")
    void sinRutaPosible() {
        red.getTramos().stream()
                .filter(t -> t.conecta("OIBA"))
                .forEach(t -> t.setEstado(EstadoTramo.CERRADO));

        for (EstrategiaNavegacion estrategia : fabrica.todas()) {
            Ruta ruta = estrategia.calcular(red, "SAN_GIL", "OIBA");
            assertFalse(ruta.isEncontrada(), estrategia.nombre());
            assertTrue(ruta.getTramos().isEmpty());
            assertTrue(ruta.getMensaje().contains("No existe una ruta transitable entre San Gil y Oiba"));
        }
    }

    @Test
    @DisplayName("Un tramo RESTRINGIDO en la ruta aumenta su tiempo total")
    void restriccionAumentaElTiempo() {
        cambiar("T05", EstadoTramo.RESTRINGIDO);

        Ruta ruta = dijkstra.calcular(red, "SAN_GIL", "BUCARAMANGA");

        // T05 pasa de 40 a 64 min; sigue siendo mejor que el desvío por Villanueva
        assertEquals(TRAMOS_RUTA_CHICAMOCHA, ruta.getTramos());
        assertEquals(103.0 + 40 * 0.6, ruta.getTiempoTotalMin(), TOLERANCIA);
    }

    @Test
    @DisplayName("Menos tramos minimiza el número de tramos, no el tiempo")
    void menosTramosEsLineaBase() {
        Ruta porTramos = menosTramos.calcular(red, "SAN_GIL", "BUCARAMANGA");
        Ruta porTiempo = dijkstra.calcular(red, "SAN_GIL", "BUCARAMANGA");

        // San Gil -> Barichara -> Zapatoca -> Girón -> Bucaramanga: 4 tramos, pero con 45 km destapados
        assertEquals(List.of("T14", "T16", "T15", "T03"), porTramos.getTramos());
        assertEquals(229.5, porTramos.getTiempoTotalMin(), TOLERANCIA);
        assertTrue(porTramos.getTramos().size() < porTiempo.getTramos().size());
        assertTrue(porTramos.getTiempoTotalMin() > porTiempo.getTiempoTotalMin());
    }

    @Test
    @DisplayName("Origen igual a destino da una ruta vacía de cero minutos")
    void origenIgualADestino() {
        for (EstrategiaNavegacion estrategia : fabrica.todas()) {
            Ruta ruta = estrategia.calcular(red, "SOCORRO", "SOCORRO");
            assertTrue(ruta.isEncontrada());
            assertEquals(List.of("SOCORRO"), ruta.getMunicipios());
            assertEquals(0.0, ruta.getTiempoTotalMin());
        }
    }

    @Test
    @DisplayName("Un municipio inexistente lanza MunicipioNoEncontradoException")
    void municipioInexistente() {
        assertThrows(MunicipioNoEncontradoException.class, () -> dijkstra.calcular(red, "MEDELLIN", "SAN_GIL"));
    }

    @Test
    @DisplayName("La fábrica entrega la estrategia por nombre y rechaza nombres desconocidos")
    void fabricaDeEstrategias() {
        assertEquals(List.of("DIJKSTRA", "A_ESTRELLA", "MENOS_TRAMOS"), fabrica.nombresDisponibles());
        assertTrue(fabrica.obtener("a-estrella") instanceof EstrategiaAEstrella);
        assertTrue(fabrica.obtener(null) instanceof EstrategiaDijkstra);
        assertThrows(AlgoritmoNoSoportadoException.class, () -> fabrica.obtener("BELLMAN_FORD"));
    }

    private void cambiar(String tramoId, EstadoTramo estado) {
        red.buscarTramo(tramoId).orElseThrow().setEstado(estado);
    }
}
