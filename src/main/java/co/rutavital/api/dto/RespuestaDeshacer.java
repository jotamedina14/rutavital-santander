package co.rutavital.api.dto;

/**
 * Resultado de POST /api/comandos/deshacer.
 */
public record RespuestaDeshacer(boolean deshecho, String descripcion, String mensaje) {
}
