package co.rutavital.modelo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Despacho de una ambulancia entre dos municipios con su ruta vigente
 * y el historial de rutas anteriores (una por cada recálculo).
 */
public class Despacho {

    private final String id;
    private final String origenId;
    private final String destinoId;
    private final LocalDateTime fechaCreacion;
    private final List<Ruta> historialRutas = new CopyOnWriteArrayList<>();
    private volatile Ruta rutaActual;
    private volatile EstadoDespacho estado = EstadoDespacho.ACTIVO;
    private volatile int vecesRecalculada;

    public Despacho(String id, String origenId, String destinoId, Ruta rutaInicial) {
        this.id = id;
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.rutaActual = rutaInicial;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Reemplaza la ruta vigente: la anterior pasa al historial y se cuenta el recálculo.
     */
    public synchronized void actualizarRuta(Ruta nuevaRuta) {
        historialRutas.add(rutaActual);
        rutaActual = nuevaRuta;
        vecesRecalculada++;
    }

    public synchronized void finalizar() {
        estado = EstadoDespacho.FINALIZADO;
    }

    public boolean estaActivo() {
        return estado == EstadoDespacho.ACTIVO;
    }

    public String getId() {
        return id;
    }

    public String getOrigenId() {
        return origenId;
    }

    public String getDestinoId() {
        return destinoId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Ruta getRutaActual() {
        return rutaActual;
    }

    public List<Ruta> getHistorialRutas() {
        return List.copyOf(historialRutas);
    }

    public EstadoDespacho getEstado() {
        return estado;
    }

    public int getVecesRecalculada() {
        return vecesRecalculada;
    }
}
