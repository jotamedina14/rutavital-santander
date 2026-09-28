package co.rutavital.comando;

/**
 * Patrón Command: una acción del operador que se puede ejecutar y deshacer.
 */
public interface Comando {

    void ejecutar();

    void deshacer();

    String descripcion();
}
