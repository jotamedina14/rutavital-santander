package co.rutavital.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Grafo no dirigido de la red vial representado con lista de adyacencia.
 * La estructura (municipios y tramos) se arma una sola vez; en ejecución
 * solo cambia el estado de los tramos.
 */
public class RedVial {

    private final Map<String, Municipio> municipios = new LinkedHashMap<>();
    private final Map<String, Tramo> tramos = new LinkedHashMap<>();
    private final Map<String, List<Tramo>> adyacencia = new HashMap<>();

    public void agregarMunicipio(Municipio municipio) {
        if (municipios.containsKey(municipio.getId())) {
            throw new IllegalArgumentException("Municipio duplicado: " + municipio.getId());
        }
        municipios.put(municipio.getId(), municipio);
        adyacencia.put(municipio.getId(), new ArrayList<>());
    }

    public void agregarTramo(Tramo tramo) {
        if (tramos.containsKey(tramo.getId())) {
            throw new IllegalArgumentException("Tramo duplicado: " + tramo.getId());
        }
        if (!municipios.containsKey(tramo.getOrigenId()) || !municipios.containsKey(tramo.getDestinoId())) {
            throw new IllegalArgumentException("El tramo " + tramo.getId() + " referencia un municipio inexistente");
        }
        tramos.put(tramo.getId(), tramo);
        // No dirigido: el tramo aparece en la lista de adyacencia de ambos extremos
        adyacencia.get(tramo.getOrigenId()).add(tramo);
        adyacencia.get(tramo.getDestinoId()).add(tramo);
    }

    public List<Municipio> getMunicipios() {
        return List.copyOf(municipios.values());
    }

    public List<Tramo> getTramos() {
        return List.copyOf(tramos.values());
    }

    /**
     * Tramos incidentes al municipio (en cualquier estado, incluidos los cerrados).
     * El vecino se obtiene con {@link Tramo#otroExtremo(String)}.
     */
    public List<Tramo> vecinos(String municipioId) {
        List<Tramo> lista = adyacencia.get(municipioId);
        return lista == null ? List.of() : Collections.unmodifiableList(lista);
    }

    public Optional<Tramo> buscarTramo(String id) {
        return Optional.ofNullable(tramos.get(id));
    }

    public Optional<Municipio> buscarMunicipio(String id) {
        return Optional.ofNullable(municipios.get(id));
    }

    public boolean existeMunicipio(String id) {
        return municipios.containsKey(id);
    }

    public int cantidadMunicipios() {
        return municipios.size();
    }

    public int cantidadTramos() {
        return tramos.size();
    }
}
