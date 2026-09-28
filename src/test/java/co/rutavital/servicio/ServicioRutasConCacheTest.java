package co.rutavital.servicio;

import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.excepcion.MunicipioNoEncontradoException;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.repositorio.RepositorioRedVial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioRutasConCacheTest {

    private RedVial red;
    private ServicioRutasConCache servicio;

    @BeforeEach
    void preparar() {
        red = new RepositorioRedVial().getRedVial();
        servicio = new ServicioRutasConCache(new NavegadorRutas(red, new FabricaEstrategias()));
    }

    @Test
    @DisplayName("La primera consulta es un miss y la segunda igual es un hit marcado desdeCache")
    void segundaConsultaIgualDaHit() {
        Ruta primera = servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");
        Ruta segunda = servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");

        assertFalse(primera.getMetricas().isDesdeCache());
        assertTrue(segunda.getMetricas().isDesdeCache());
        assertEquals(primera.getTramos(), segunda.getTramos());
        assertEquals(primera.getTiempoTotalMin(), segunda.getTiempoTotalMin());
        assertEquals(1, servicio.getHits());
        assertEquals(1, servicio.getMisses());
        assertEquals(0.5, servicio.getTasaAcierto());
    }

    @Test
    @DisplayName("La clave incluye el algoritmo: otro algoritmo es otra entrada")
    void laClaveDistingueAlgoritmos() {
        servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");
        Ruta otra = servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "A_ESTRELLA");
        Ruta repetida = servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "a-estrella");

        assertFalse(otra.getMetricas().isDesdeCache());
        assertTrue(repetida.getMetricas().isDesdeCache());
        assertEquals(2, servicio.getEntradas());
        assertEquals("SAN_GIL|BUCARAMANGA|DIJKSTRA", ServicioRutasConCache.clave("SAN_GIL", "BUCARAMANGA", "DIJKSTRA"));
    }

    @Test
    @DisplayName("limpiar() invalida la caché y la siguiente consulta refleja el estado actual de la red")
    void limpiarInvalida() {
        servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");
        red.buscarTramo("T06").orElseThrow().setEstado(EstadoTramo.CERRADO);

        servicio.limpiar();
        Ruta despues = servicio.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");

        assertFalse(despues.getMetricas().isDesdeCache());
        assertFalse(despues.contieneTramo("T06"));
        assertEquals(2, servicio.getMisses());
        assertEquals(0, servicio.getHits());
    }

    @Test
    @DisplayName("Los errores no se guardan en la caché")
    void losErroresNoSeCachean() {
        assertThrows(MunicipioNoEncontradoException.class,
                () -> servicio.calcularRuta("CALI", "BUCARAMANGA", "DIJKSTRA"));
        assertEquals(0, servicio.getEntradas());
    }
}
