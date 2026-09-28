package co.rutavital.api.dto;

import co.rutavital.modelo.Ruta;

import java.util.List;

/**
 * Resultado de ejecutar todas las estrategias (sin caché) para el mismo par.
 */
public record ComparacionRutas(String origenId, String destinoId, List<Ruta> rutas) {
}
