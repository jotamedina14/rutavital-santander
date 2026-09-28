package co.rutavital.evento;

import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.excepcion.TramoNoEncontradoException;
import co.rutavital.modelo.Despacho;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservadoresVialesTest {

    private RedVial red;
    private ServicioRutasConCache cache;
    private GestorDespachos gestor;
    private CentroEstadoVial centro;
    private InvalidadorCache invalidador;
    private RecalculadorDespachos recalculador;
    private BitacoraEventos bitacora;

    @BeforeEach
    void preparar() {
        red = new RepositorioRedVial().getRedVial();
        cache = new ServicioRutasConCache(new NavegadorRutas(red, new FabricaEstrategias()));
        gestor = new GestorDespachos(cache);
        centro = new CentroEstadoVial(red);
        invalidador = new InvalidadorCache(cache);
        recalculador = new RecalculadorDespachos(gestor, cache);
        bitacora = new BitacoraEventos();
    }

    private void suscribirEnOrdenCorrecto() {
        centro.suscribir(invalidador);
        centro.suscribir(recalculador);
        centro.suscribir(bitacora);
    }

    @Test
    @DisplayName("Prueba 5: el Observer recalcula el despacho activo afectado y guarda la ruta anterior")
    void recalculaDespachoAfectado() {
        suscribirEnOrdenCorrecto();
        Despacho despacho = gestor.crear("SAN_GIL", "BUCARAMANGA");
        Ruta original = despacho.getRutaActual();
        assertTrue(original.contieneTramo("T06"));

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe sector Pescadero");

        assertEquals(1, despacho.getVecesRecalculada());
        assertEquals(List.of(original), despacho.getHistorialRutas());
        Ruta nueva = despacho.getRutaActual();
        assertFalse(nueva.contieneTramo("T06"));
        assertTrue(nueva.getMunicipios().containsAll(List.of("VILLANUEVA", "LOS_SANTOS")));
        assertTrue(nueva.getTiempoTotalMin() > original.getTiempoTotalMin());
    }

    @Test
    @DisplayName("Al reabrir el tramo el despacho vuelve a la ruta original")
    void reabrirTramoRecuperaLaRutaOriginal() {
        suscribirEnOrdenCorrecto();
        Despacho despacho = gestor.crear("SAN_GIL", "BUCARAMANGA");
        Ruta original = despacho.getRutaActual();

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");
        centro.cambiarEstado("T06", EstadoTramo.ABIERTO, "Vía despejada");

        assertEquals(2, despacho.getVecesRecalculada());
        assertEquals(2, despacho.getHistorialRutas().size());
        assertEquals(original.getTramos(), despacho.getRutaActual().getTramos());
    }

    @Test
    @DisplayName("No recalcula despachos cuya ruta no pasa por el tramo cerrado ni los finalizados")
    void ignoraDespachosNoAfectados() {
        suscribirEnOrdenCorrecto();
        Despacho noAfectado = gestor.crear("SOCORRO", "OIBA");
        Despacho finalizado = gestor.crear("SAN_GIL", "BUCARAMANGA");
        gestor.finalizar(finalizado.getId());

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");

        assertEquals(0, noAfectado.getVecesRecalculada());
        assertEquals(0, finalizado.getVecesRecalculada());
        assertTrue(finalizado.getRutaActual().contieneTramo("T06"));
    }

    @Test
    @DisplayName("Un despacho sin ruta se recalcula cuando se reabre el tramo que lo bloqueaba")
    void despachoSinRutaSeRecuperaAlReabrir() {
        suscribirEnOrdenCorrecto();
        centro.cambiarEstado("T20", EstadoTramo.CERRADO, "Puente caído");
        Despacho despacho = gestor.crear("SAN_GIL", "OIBA");
        assertFalse(despacho.getRutaActual().isEncontrada());

        centro.cambiarEstado("T20", EstadoTramo.ABIERTO, "Puente habilitado");

        assertTrue(despacho.getRutaActual().isEncontrada());
        assertEquals(1, despacho.getVecesRecalculada());
    }

    @Test
    @DisplayName("Prueba 6: la caché da hit en la segunda consulta y se invalida cuando cambia un tramo")
    void laCacheSeInvalidaAlCambiarUnTramo() {
        suscribirEnOrdenCorrecto();
        cache.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");
        Ruta repetida = cache.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");
        assertTrue(repetida.getMetricas().isDesdeCache());
        assertEquals(1, cache.getHits());

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");
        Ruta despuesDelCierre = cache.calcularRuta("SAN_GIL", "BUCARAMANGA", "DIJKSTRA");

        assertFalse(despuesDelCierre.getMetricas().isDesdeCache());
        assertFalse(despuesDelCierre.contieneTramo("T06"));
        assertEquals(2, cache.getMisses());
        assertEquals(1, cache.getEntradas());
    }

    @Test
    @DisplayName("El orden importa: si se recalcula antes de invalidar, el despacho recibe la ruta vieja de la caché")
    void ordenDeNotificacionEvitaRutasViejas() {
        // Orden incorrecto a propósito
        centro.suscribir(recalculador);
        centro.suscribir(invalidador);
        Despacho despacho = gestor.crear("SAN_GIL", "BUCARAMANGA");

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");

        assertTrue(despacho.getRutaActual().contieneTramo("T06"), "con el orden incorrecto se usa la caché vieja");
    }

    @Test
    @DisplayName("La bitácora guarda los eventos del más reciente al más antiguo")
    void bitacoraRegistraEventos() {
        suscribirEnOrdenCorrecto();

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");
        centro.cambiarEstado("T05", EstadoTramo.RESTRINGIDO, "Paso a un carril");

        List<EventoVial> eventos = bitacora.getEventos();
        assertEquals(2, eventos.size());
        assertEquals("T05", eventos.get(0).getTramoId());
        assertEquals(EstadoTramo.ABIERTO, eventos.get(1).getEstadoAnterior());
        assertEquals(EstadoTramo.CERRADO, eventos.get(1).getEstadoNuevo());
        assertEquals("Derrumbe", eventos.get(1).getMotivo());
    }

    @Test
    @DisplayName("Notifica en orden de suscripción y un observador con error no detiene a los demás")
    void notificacionOrdenadaYRobusta() {
        List<String> llamadas = new ArrayList<>();
        centro.suscribir(e -> llamadas.add("primero"));
        centro.suscribir(e -> {
            throw new IllegalStateException("falla simulada");
        });
        centro.suscribir(e -> llamadas.add("tercero"));

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");

        assertEquals(List.of("primero", "tercero"), llamadas);
    }

    @Test
    @DisplayName("Cambiar al mismo estado no genera evento; un tramo inexistente falla")
    void casosBorde() {
        suscribirEnOrdenCorrecto();

        assertTrue(centro.cambiarEstado("T06", EstadoTramo.ABIERTO, "Sin cambio").isEmpty());
        assertEquals(0, bitacora.cantidad());
        assertThrows(TramoNoEncontradoException.class,
                () -> centro.cambiarEstado("T99", EstadoTramo.CERRADO, "No existe"));
    }

    @Test
    @DisplayName("Se puede desuscribir un observador")
    void desuscribir() {
        suscribirEnOrdenCorrecto();
        centro.desuscribir(bitacora);

        centro.cambiarEstado("T06", EstadoTramo.CERRADO, "Derrumbe");

        assertEquals(0, bitacora.cantidad());
        assertEquals(List.of(invalidador, recalculador), centro.getObservadores());
    }
}
