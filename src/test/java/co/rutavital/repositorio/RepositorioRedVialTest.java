package co.rutavital.repositorio;

import co.rutavital.modelo.DistanciaGeografica;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Tramo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioRedVialTest {

    private final RedVial red = new RepositorioRedVial().getRedVial();

    @Test
    @DisplayName("Carga los 22 municipios y los 25 tramos de Santander, todos abiertos")
    void cargaLaRedCompleta() {
        assertEquals(22, red.cantidadMunicipios());
        assertEquals(25, red.cantidadTramos());
        assertTrue(red.getTramos().stream().allMatch(t -> t.getEstado() == EstadoTramo.ABIERTO));

        Municipio bucaramanga = red.buscarMunicipio("BUCARAMANGA").orElseThrow();
        assertTrue(bucaramanga.isTieneHospital());
        assertEquals(3, bucaramanga.getNivelHospital());
    }

    @Test
    @DisplayName("Ningún tramo es más corto que la línea recta entre sus extremos (A* admisible)")
    void losDatosPermitenUnaHeuristicaAdmisible() {
        for (Tramo tramo : red.getTramos()) {
            Municipio a = red.buscarMunicipio(tramo.getOrigenId()).orElseThrow();
            Municipio b = red.buscarMunicipio(tramo.getDestinoId()).orElseThrow();
            double lineaRecta = DistanciaGeografica.haversineKm(a, b);

            assertTrue(lineaRecta <= tramo.getDistanciaKm(),
                    () -> tramo.getId() + ": línea recta " + lineaRecta + " km > " + tramo.getDistanciaKm() + " km");
        }
    }
}
