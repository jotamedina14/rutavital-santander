package co.rutavital.repositorio;

import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.TipoVia;
import co.rutavital.modelo.Tramo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Carga la red vial desde un archivo JSON del classpath al iniciar la aplicación
 * y la expone al resto de capas. No hay base de datos.
 */
@Repository
public class RepositorioRedVial {

    public static final String ARCHIVO_POR_DEFECTO = "red-vial-santander.json";

    private static final Logger log = LoggerFactory.getLogger(RepositorioRedVial.class);

    private final RedVial redVial;

    @Autowired
    public RepositorioRedVial(@Value("${rutavital.red.archivo:" + ARCHIVO_POR_DEFECTO + "}") String archivo) {
        this.redVial = cargar(archivo);
        log.info("Red vial cargada desde {}: {} municipios y {} tramos",
                archivo, redVial.cantidadMunicipios(), redVial.cantidadTramos());
    }

    /**
     * Constructor para usar el repositorio fuera de Spring (pruebas y benchmark).
     */
    public RepositorioRedVial() {
        this(ARCHIVO_POR_DEFECTO);
    }

    public RedVial getRedVial() {
        return redVial;
    }

    private static RedVial cargar(String archivo) {
        try (InputStream entrada = RepositorioRedVial.class.getClassLoader().getResourceAsStream(archivo)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el archivo de la red vial en el classpath: " + archivo);
            }
            JsonNode raiz = new ObjectMapper().readTree(entrada);
            RedVial red = new RedVial();
            for (JsonNode m : raiz.path("municipios")) {
                red.agregarMunicipio(new Municipio(
                        m.path("id").asText(),
                        m.path("nombre").asText(),
                        m.path("latitud").asDouble(),
                        m.path("longitud").asDouble(),
                        m.path("tieneHospital").asBoolean(),
                        m.path("nivelHospital").asInt()));
            }
            for (JsonNode t : raiz.path("tramos")) {
                red.agregarTramo(new Tramo(
                        t.path("id").asText(),
                        t.path("origenId").asText(),
                        t.path("destinoId").asText(),
                        t.path("distanciaKm").asDouble(),
                        TipoVia.valueOf(t.path("tipoVia").asText()),
                        EstadoTramo.valueOf(t.path("estado").asText(EstadoTramo.ABIERTO.name()))));
            }
            return red;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la red vial: " + archivo, e);
        }
    }
}
