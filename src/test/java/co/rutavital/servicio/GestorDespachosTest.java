package co.rutavital.servicio;

import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.excepcion.DespachoNoEncontradoException;
import co.rutavital.modelo.Despacho;
import co.rutavital.modelo.EstadoDespacho;
import co.rutavital.repositorio.RepositorioRedVial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorDespachosTest {

    private GestorDespachos gestor;

    @BeforeEach
    void preparar() {
        gestor = new GestorDespachos(new NavegadorRutas(new RepositorioRedVial().getRedVial(), new FabricaEstrategias()));
    }

    @Test
    void creaUnDespachoActivoConSuRuta() {
        Despacho despacho = gestor.crear("SAN_GIL", "BUCARAMANGA");

        assertEquals("D-001", despacho.getId());
        assertEquals(EstadoDespacho.ACTIVO, despacho.getEstado());
        assertTrue(despacho.getRutaActual().isEncontrada());
        assertEquals("DIJKSTRA", despacho.getRutaActual().getAlgoritmo());
        assertEquals(0, despacho.getVecesRecalculada());
        assertTrue(despacho.getHistorialRutas().isEmpty());
    }

    @Test
    void finalizarLoSacaDeLosActivos() {
        Despacho primero = gestor.crear("SAN_GIL", "BUCARAMANGA");
        Despacho segundo = gestor.crear("SOCORRO", "BUCARAMANGA", "A_ESTRELLA");

        gestor.finalizar(primero.getId());

        assertEquals(List.of(segundo), gestor.listarActivos());
        assertEquals(2, gestor.listarTodos().size());
        assertEquals(EstadoDespacho.FINALIZADO, primero.getEstado());
    }

    @Test
    void finalizarUnDespachoInexistenteFalla() {
        assertThrows(DespachoNoEncontradoException.class, () -> gestor.finalizar("D-999"));
    }
}
