package co.rutavital.api.dto;

/**
 * Estado del proxy de caché. tasaAcierto va de 0 a 1.
 */
public record MetricasCacheDTO(boolean habilitada, long hits, long misses, double tasaAcierto, int entradas) {
}
