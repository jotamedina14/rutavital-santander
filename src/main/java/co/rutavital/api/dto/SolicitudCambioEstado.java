package co.rutavital.api.dto;

/**
 * Cuerpo de POST /api/tramos/{id}/estado, por ejemplo
 * {"estado":"CERRADO","motivo":"Derrumbe sector Pescadero"}.
 */
public record SolicitudCambioEstado(String estado, String motivo) {
}
