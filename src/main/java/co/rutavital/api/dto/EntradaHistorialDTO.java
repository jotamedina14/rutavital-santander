package co.rutavital.api.dto;

/**
 * Un comando del historial (pila); la posición 1 es el tope, el próximo en deshacerse.
 */
public record EntradaHistorialDTO(int posicion, String tipo, String descripcion) {
}
