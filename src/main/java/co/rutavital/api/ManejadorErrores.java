package co.rutavital.api;

import co.rutavital.api.dto.ErrorRespuesta;
import co.rutavital.excepcion.RecursoNoEncontradoException;
import co.rutavital.excepcion.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Traduce las excepciones de la aplicación a respuestas HTTP con un cuerpo uniforme.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest peticion) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), peticion);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(SolicitudInvalidaException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), peticion);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> faltaParametro(MissingServletRequestParameterException e,
                                                         HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + e.getParameterName() + "'", peticion);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoInvalido(HttpMessageNotReadableException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", peticion);
    }

    private static ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje, HttpServletRequest peticion) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje,
                peticion.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
