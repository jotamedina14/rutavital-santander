package co.rutavital.api;

import co.rutavital.api.dto.SolicitudDespacho;
import co.rutavital.comando.ComandoCrearDespacho;
import co.rutavital.comando.InvocadorComandos;
import co.rutavital.modelo.Despacho;
import co.rutavital.servicio.GestorDespachos;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/despachos")
public class DespachoController {

    private final GestorDespachos gestor;
    private final InvocadorComandos invocador;

    public DespachoController(GestorDespachos gestor, InvocadorComandos invocador) {
        this.gestor = gestor;
        this.invocador = invocador;
    }

    /**
     * Crea el despacho pasando por el invocador (Command), así se puede deshacer.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Despacho crear(@RequestBody SolicitudDespacho solicitud) {
        ComandoCrearDespacho comando = new ComandoCrearDespacho(gestor,
                Parametros.id(solicitud.origen(), "origen"),
                Parametros.id(solicitud.destino(), "destino"),
                solicitud.algoritmo());
        invocador.ejecutar(comando);
        return comando.getDespacho();
    }

    /**
     * Lista todos los despachos, o solo los activos con ?soloActivos=true.
     */
    @GetMapping
    public List<Despacho> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return soloActivos ? gestor.listarActivos() : gestor.listarTodos();
    }

    @PostMapping("/{id}/finalizar")
    public Despacho finalizar(@PathVariable String id) {
        return gestor.finalizar(Parametros.id(id, "id"));
    }
}
