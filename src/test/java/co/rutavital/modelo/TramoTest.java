package co.rutavital.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TramoTest {

    private static final double TOLERANCIA = 1e-9;

    @Test
    @DisplayName("El tiempo de un tramo abierto es distancia / velocidad * 60")
    void tiempoTramoAbierto() {
        assertEquals(12.0, new Tramo("A", "X", "Y", 12, TipoVia.PRINCIPAL).tiempoEstimadoMin(), TOLERANCIA);
        assertEquals(33.0, new Tramo("B", "X", "Y", 22, TipoVia.SECUNDARIA).tiempoEstimadoMin(), TOLERANCIA);
        assertEquals(115.2, new Tramo("C", "X", "Y", 48, TipoVia.DESTAPADA).tiempoEstimadoMin(), TOLERANCIA);
    }

    @Test
    @DisplayName("Prueba 3: RESTRINGIDO multiplica el tiempo del tramo por 1.6")
    void restringidoMultiplicaPorFactor() {
        for (TipoVia tipo : TipoVia.values()) {
            Tramo tramo = new Tramo("T", "X", "Y", 12, tipo);
            double tiempoAbierto = tramo.tiempoEstimadoMin();

            tramo.setEstado(EstadoTramo.RESTRINGIDO);

            assertEquals(tiempoAbierto * 1.6, tramo.tiempoEstimadoMin(), TOLERANCIA);
            assertTrue(tramo.esTransitable());
        }
    }

    @Test
    @DisplayName("Un tramo CERRADO no es transitable y su tiempo es infinito")
    void cerradoNoTransitable() {
        Tramo tramo = new Tramo("T", "X", "Y", 12, TipoVia.PRINCIPAL, EstadoTramo.CERRADO);

        assertFalse(tramo.esTransitable());
        assertEquals(Double.POSITIVE_INFINITY, tramo.tiempoEstimadoMin());
    }

    @Test
    @DisplayName("El tramo es no dirigido: se obtiene el otro extremo desde cualquiera de los dos")
    void otroExtremo() {
        Tramo tramo = new Tramo("T", "X", "Y", 5, TipoVia.PRINCIPAL);

        assertEquals("Y", tramo.otroExtremo("X"));
        assertEquals("X", tramo.otroExtremo("Y"));
        assertThrows(IllegalArgumentException.class, () -> tramo.otroExtremo("Z"));
    }

    @Test
    @DisplayName("La velocidad máxima de la red es la de vía principal (60 km/h)")
    void velocidadMaxima() {
        assertEquals(60, TipoVia.velocidadMaximaKmH());
    }

    @Test
    @DisplayName("El orden de estados de mejor a peor es ABIERTO, RESTRINGIDO, CERRADO")
    void ordenDeEstados() {
        assertTrue(EstadoTramo.ABIERTO.esMejorQue(EstadoTramo.RESTRINGIDO));
        assertTrue(EstadoTramo.RESTRINGIDO.esMejorQue(EstadoTramo.CERRADO));
        assertFalse(EstadoTramo.CERRADO.esMejorQue(EstadoTramo.ABIERTO));
    }
}
