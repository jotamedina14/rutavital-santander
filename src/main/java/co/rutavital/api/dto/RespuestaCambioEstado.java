package co.rutavital.api.dto;

/**
 * Resultado de cambiar el estado de un tramo.
 */
public record RespuestaCambioEstado(TramoDTO tramo, String descripcion) {
}
