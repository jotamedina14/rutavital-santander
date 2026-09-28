package co.rutavital.api.dto;

import java.time.LocalDateTime;

/**
 * Cuerpo estándar de las respuestas de error.
 */
public record ErrorRespuesta(int estado, String error, String mensaje, String ruta, LocalDateTime fechaHora) {
}
