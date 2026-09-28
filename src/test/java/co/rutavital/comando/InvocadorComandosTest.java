package co.rutavital.comando;

import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.evento.CentroEstadoVial;
import co.rutavital.evento.InvalidadorCache;
import co.rutavital.evento.RecalculadorDespachos;
import co.rutavital.excepcion.TramoNoEncontradoException;
import co.rutavital.modelo.Despacho;
import co.rutavital.modelo.EstadoDespacho;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.repositorio.RepositorioRedVial;
import co.rutavital.servicio.GestorDespachos;
import co.rutavital.servicio.NavegadorRutas;
import co.rutavital.servicio.ServicioRutasConCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocadorComandosTest {

    private RedVial red;
    private ServicioRutasConCache servicioRutas;
    private GestorDespachos gestor;
    private CentroEstadoVial centro;
    private InvocadorComandos invocador;

    @BeforeEach
    void preparar() {
        red = new RepositorioRedVial().getRedVial();
        servicioRutas = new ServicioRutasConCache(new NavegadorRutas(red, new FabricaEstrategias()));
        gestor = new GestorDespachos(servicioRutas);
        centro = new CentroEstadoVial(red);
        centro.suscribir(new InvalidadorCache(servicioRutas));
        centro.suscribir(new RecalculadorDespachos(gestor, servicioRutas));
        invocador = new InvocadorComandos();
    }

    @Test
    @DisplayName("Prueba 4: deshacer un cierre deja el tramo ABIERTO y la ruta vuelve a ser la original")
    void deshacerCierreRestauraLaRuta() {
        Ruta original = servicioRutas.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");

        invocador.ejecutar(new ComandoCambiarEstadoTramo(centro, "T06", EstadoTramo.CERRADO, "Derrumbe sector Pescadero"));
        assertEquals(EstadoTramo.CERRADO, centro.estadoDe("T06"));
        assertFalse(servicioRutas.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA").contieneTramo("T06"));

        invocador.deshacerUltimo();

        assertEquals(EstadoTramo.ABIERTO, centro.estadoDe("T06"));
        Ruta restaurada = servicioRutas.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");
        assertEquals(original.getTramos(), restaurada.getTramos());
        assertEquals(original.getTiempoTotalMin(), restaurada.getTiempoTotalMin());
        assertTrue(invocador.getHistorial().isEmpty());
    }

    @Test
    @DisplayName("Deshacer restaura el estado previo exacto (RESTRINGIDO -> CERRADO -> RESTRINGIDO)")
    void deshacerRestauraEstadoPrevio() {
        invocador.ejecutar(new ComandoCambiarEstadoTramo(centro, "T05", EstadoTramo.RESTRINGIDO, "Obras"));
        invocador.ejecutar(new ComandoCambiarEstadoTramo(centro, "T05", EstadoTramo.CERRADO, "Derrumbe"));

        invocador.deshacerUltimo();
        assertEquals(EstadoTramo.RESTRINGIDO, centro.estadoDe("T05"));

        invocador.deshacerUltimo();
        assertEquals(EstadoTramo.ABIERTO, centro.estadoDe("T05"));
    }

    @Test
    @DisplayName("Deshacer el cierre también devuelve al despacho activo a su ruta original")
    void deshacerCierreRecalculaDespacho() {
        ComandoCrearDespacho crear = new ComandoCrearDespacho(gestor, "SAN_GIL", "BUCARAMANGA");
        invocador.ejecutar(crear);
        Despacho despacho = crear.getDespacho();
        List<String> tramosOriginales = despacho.getRutaActual().getTramos();

        invocador.ejecutar(new ComandoCambiarEstadoTramo(centro, "T06", EstadoTramo.CERRADO, "Derrumbe"));
        invocador.deshacerUltimo();

        assertEquals(tramosOriginales, despacho.getRutaActual().getTramos());
        assertEquals(2, despacho.getVecesRecalculada());
    }

    @Test
    @DisplayName("Deshacer la creación de un despacho lo finaliza")
    void deshacerCrearDespacho() {
        ComandoCrearDespacho comando = new ComandoCrearDespacho(gestor, "SAN_GIL", "BUCARAMANGA");
        invocador.ejecutar(comando);
        assertEquals(1, gestor.listarActivos().size());

        invocador.deshacerUltimo();

        assertEquals(EstadoDespacho.FINALIZADO, comando.getDespacho().getEstado());
        assertTrue(gestor.listarActivos().isEmpty());
    }

    @Test
    @DisplayName("El historial es una pila: el último comando aparece primero")
    void historialEnPila() {
        invocador.ejecutar(new ComandoCambiarEstadoTramo(centro, "T06", EstadoTramo.CERRADO, "Derrumbe"));
        invocador.ejecutar(new ComandoCrearDespacho(gestor, "SAN_GIL", "BUCARAMANGA"));

        List<Comando> historial = invocador.getHistorial();

        assertEquals(2, historial.size());
        assertTrue(historial.get(0) instanceof ComandoCrearDespacho);
        assertEquals("Tramo T06: ABIERTO → CERRADO (Derrumbe)", historial.get(1).descripcion());
    }

    @Test
    @DisplayName("Sin comandos, deshacer no hace nada; un comando que falla no entra al historial")
    void casosBorde() {
        assertTrue(invocador.deshacerUltimo().isEmpty());

        assertThrows(TramoNoEncontradoException.class, () -> invocador.ejecutar(
                new ComandoCambiarEstadoTramo(centro, "T99", EstadoTramo.CERRADO, "No existe")));
        assertTrue(invocador.getHistorial().isEmpty());
    }
}
