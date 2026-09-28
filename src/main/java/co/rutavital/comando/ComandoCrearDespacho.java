package co.rutavital.comando;

import co.rutavital.modelo.Despacho;
import co.rutavital.servicio.GestorDespachos;

/**
 * Crea un despacho con el {@link GestorDespachos}. Deshacer lo finaliza.
 */
public class ComandoCrearDespacho implements Comando {

    private final GestorDespachos gestorDespachos;
    private final String origenId;
    private final String destinoId;
    private final String algoritmo;
    private Despacho despacho;

    public ComandoCrearDespacho(GestorDespachos gestorDespachos, String origenId, String destinoId, String algoritmo) {
        this.gestorDespachos = gestorDespachos;
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.algoritmo = algoritmo;
    }

    public ComandoCrearDespacho(GestorDespachos gestorDespachos, String origenId, String destinoId) {
        this(gestorDespachos, origenId, destinoId, null);
    }

    @Override
    public void ejecutar() {
        despacho = gestorDespachos.crear(origenId, destinoId, algoritmo);
    }

    @Override
    public void deshacer() {
        if (despacho == null) {
            throw new IllegalStateException("No se puede deshacer un comando que no se ha ejecutado");
        }
        gestorDespachos.finalizar(despacho.getId());
    }

    @Override
    public String descripcion() {
        String id = despacho == null ? "" : despacho.getId() + " ";
        return "Crear despacho " + id + origenId + " → " + destinoId;
    }

    public Despacho getDespacho() {
        return despacho;
    }
}
