package co.rutavital.evento;

import co.rutavital.modelo.Despacho;
import co.rutavital.modelo.Ruta;
import co.rutavital.servicio.GestorDespachos;
import co.rutavital.servicio.ServicioRutas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observador que recalcula la ruta de los despachos ACTIVOS afectados por un cambio:
 * <ul>
 *     <li>Si el tramo está en la ruta actual del despacho, siempre recalcula y registra el cambio
 *     (aunque el camino sea el mismo, el tiempo cambió).</li>
 *     <li>Si el tramo mejora (se reabre o se levanta una restricción), recalcula todos los despachos
 *     activos y registra solo los que obtienen una ruta distinta.</li>
 * </ul>
 * La ruta anterior queda en el historial del despacho y se incrementa vecesRecalculada.
 */
@Component
public class RecalculadorDespachos implements ObservadorVial {

    private static final Logger log = LoggerFactory.getLogger(RecalculadorDespachos.class);

    private final GestorDespachos gestor;
    private final ServicioRutas servicioRutas;

    public RecalculadorDespachos(GestorDespachos gestor, ServicioRutas servicioRutas) {
        this.gestor = gestor;
        this.servicioRutas = servicioRutas;
    }

    @Override
    public void alCambiarEstado(EventoVial evento) {
        for (Despacho despacho : gestor.listarActivos()) {
            Ruta actual = despacho.getRutaActual();
            boolean afectado = actual.contieneTramo(evento.getTramoId());
            if (!afectado && !evento.esMejora()) {
                continue;
            }
            Ruta nueva = servicioRutas.calcularRuta(despacho.getOrigenId(), despacho.getDestinoId(), actual.getAlgoritmo());
            if (afectado || !nueva.esEquivalenteA(actual)) {
                despacho.actualizarRuta(nueva);
                log.info("Despacho {} recalculado por {}: {} min -> {} min", despacho.getId(), evento,
                        actual.getTiempoTotalMin(), nueva.getTiempoTotalMin());
            }
        }
    }
}
