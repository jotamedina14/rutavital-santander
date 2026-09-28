package co.rutavital.modelo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedVialTest {

    private RedVial red;

    @BeforeEach
    void crearRed() {
        red = new RedVial();
        red.agregarMunicipio(new Municipio("A", "A", 7.0, -73.0, true, 3));
        red.agregarMunicipio(new Municipio("B", "B", 7.1, -73.0, false, 0));
        red.agregarMunicipio(new Municipio("C", "C", 7.2, -73.0, true, 1));
        red.agregarTramo(new Tramo("AB", "A", "B", 12, TipoVia.PRINCIPAL));
        red.agregarTramo(new Tramo("BC", "B", "C", 12, TipoVia.SECUNDARIA));
    }

    @Test
    void laListaDeAdyacenciaEsNoDirigida() {
        List<Tramo> vecinosDeB = red.vecinos("B");

        assertEquals(2, vecinosDeB.size());
        assertEquals(1, red.vecinos("A").size());
        assertEquals("AB", red.vecinos("A").get(0).getId());
        assertEquals("BC", red.vecinos("C").get(0).getId());
    }

    @Test
    void buscaMunicipiosYTramos() {
        assertTrue(red.buscarMunicipio("A").isPresent());
        assertTrue(red.buscarMunicipio("Z").isEmpty());
        assertTrue(red.buscarTramo("BC").isPresent());
        assertTrue(red.buscarTramo("ZZ").isEmpty());
        assertEquals(3, red.getMunicipios().size());
        assertEquals(2, red.getTramos().size());
    }

    @Test
    void rechazaTramosConMunicipiosInexistentesODuplicados() {
        assertThrows(IllegalArgumentException.class,
                () -> red.agregarTramo(new Tramo("AZ", "A", "Z", 5, TipoVia.PRINCIPAL)));
        assertThrows(IllegalArgumentException.class,
                () -> red.agregarTramo(new Tramo("AB", "A", "C", 5, TipoVia.PRINCIPAL)));
    }
}
