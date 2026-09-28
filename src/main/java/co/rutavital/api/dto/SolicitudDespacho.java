package co.rutavital.api.dto;

/**
 * Cuerpo de POST /api/despachos, por ejemplo {"origen":"SAN_GIL","destino":"BUCARAMANGA"}.
 * El algoritmo es opcional (por defecto DIJKSTRA).
 */
public record SolicitudDespacho(String origen, String destino, String algoritmo) {
}
