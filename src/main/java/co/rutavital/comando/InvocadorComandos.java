package co.rutavital.comando;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Invocador del patrón Command: ejecuta los comandos y los guarda en una pila
 * para poder deshacer el último.
 */
public class InvocadorComandos {

    private final Deque<Comando> historial = new ArrayDeque<>();

    /**
     * Ejecuta el comando y, si no lanza excepción, lo apila en el historial.
     */
    public synchronized void ejecutar(Comando comando) {
        comando.ejecutar();
        historial.push(comando);
    }

    /**
     * Deshace el último comando ejecutado.
     *
     * @return el comando deshecho, o vacío si la pila estaba vacía
     */
    public synchronized Optional<Comando> deshacerUltimo() {
        Comando ultimo = historial.peek();
        if (ultimo == null) {
            return Optional.empty();
        }
        ultimo.deshacer();
        historial.pop();
        return Optional.of(ultimo);
    }

    /**
     * Comandos ejecutados, del más reciente (tope de la pila) al más antiguo.
     */
    public synchronized List<Comando> getHistorial() {
        return List.copyOf(historial);
    }
}
