package co.rutavital.api;

import co.rutavital.api.dto.EntradaHistorialDTO;
import co.rutavital.api.dto.RespuestaCambioEstado;
import co.rutavital.api.dto.RespuestaDeshacer;
import co.rutavital.api.dto.SolicitudCambioEstado;
import co.rutavital.api.dto.TramoDTO;
import co.rutavital.comando.Comando;
import co.rutavital.comando.ComandoCambiarEstadoTramo;
import co.rutavital.comando.InvocadorComandos;
import co.rutavital.evento.BitacoraEventos;
import co.rutavital.evento.CentroEstadoVial;
import co.rutavital.evento.EventoVial;
import co.rutavital.excepcion.SolicitudInvalidaException;
import co.rutavital.excepcion.TramoNoEncontradoException;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class RedVialController {

    private final RedVial red;
    private final CentroEstadoVial centroEstadoVial;
    private final InvocadorComandos invocador;
    private final BitacoraEventos bitacora;

    public RedVialController(RedVial red, CentroEstadoVial centroEstadoVial, InvocadorComandos invocador,
                             BitacoraEventos bitacora) {
        this.red = red;
        this.centroEstadoVial = centroEstadoVial;
        this.invocador = invocador;
        this.bitacora = bitacora;
    }

    @GetMapping("/municipios")
    public List<Municipio> municipios() {
        return red.getMunicipios();
    }

    @GetMapping("/tramos")
    public List<TramoDTO> tramos() {
        return red.getTramos().stream().map(TramoDTO::desde).toList();
    }

    /**
     * Cambia el estado de un tramo pasando por el invocador (Command), que a su vez
     * usa el CentroEstadoVial (Observer).
     */
    @PostMapping("/tramos/{id}/estado")
    public RespuestaCambioEstado cambiarEstado(@PathVariable String id, @RequestBody SolicitudCambioEstado solicitud) {
        String tramoId = Parametros.id(id, "id");
        red.buscarTramo(tramoId).orElseThrow(() -> new TramoNoEncontradoException(tramoId));
        EstadoTramo nuevoEstado = convertirEstado(solicitud.estado());
        String motivo = solicitud.motivo() == null || solicitud.motivo().isBlank()
                ? "Sin motivo especificado" : solicitud.motivo().trim();

        Comando comando = new ComandoCambiarEstadoTramo(centroEstadoVial, tramoId, nuevoEstado, motivo);
        invocador.ejecutar(comando);
        return new RespuestaCambioEstado(TramoDTO.desde(red.buscarTramo(tramoId).orElseThrow()), comando.descripcion());
    }

    @PostMapping("/comandos/deshacer")
    public RespuestaDeshacer deshacer() {
        return invocador.deshacerUltimo()
                .map(c -> new RespuestaDeshacer(true, c.descripcion(), "Se deshizo: " + c.descripcion()))
                .orElseGet(() -> new RespuestaDeshacer(false, null, "No hay acciones para deshacer"));
    }

    @GetMapping("/comandos/historial")
    public List<EntradaHistorialDTO> historial() {
        List<Comando> comandos = invocador.getHistorial();
        List<EntradaHistorialDTO> resultado = new ArrayList<>();
        for (int i = 0; i < comandos.size(); i++) {
            Comando c = comandos.get(i);
            resultado.add(new EntradaHistorialDTO(i + 1, c.getClass().getSimpleName(), c.descripcion()));
        }
        return resultado;
    }

    @GetMapping("/eventos")
    public List<EventoVial> eventos() {
        return bitacora.getEventos();
    }

    private static EstadoTramo convertirEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new SolicitudInvalidaException("El campo 'estado' es obligatorio");
        }
        try {
            return EstadoTramo.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException("Estado inválido '" + estado + "'. Valores permitidos: "
                    + Arrays.toString(EstadoTramo.values()));
        }
    }
}
