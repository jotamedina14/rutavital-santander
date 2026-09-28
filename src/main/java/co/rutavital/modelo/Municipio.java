package co.rutavital.modelo;

import java.util.Objects;

/**
 * Municipio de la red vial. Es un nodo del grafo.
 */
public class Municipio {

    private final String id;
    private final String nombre;
    private final double latitud;
    private final double longitud;
    private final boolean tieneHospital;
    /** Nivel de complejidad del hospital: 0 (sin hospital) a 3 (alta complejidad). */
    private final int nivelHospital;

    public Municipio(String id, String nombre, double latitud, double longitud,
                     boolean tieneHospital, int nivelHospital) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del municipio es obligatorio");
        }
        if (nivelHospital < 0 || nivelHospital > 3) {
            throw new IllegalArgumentException("El nivel de hospital debe estar entre 0 y 3: " + id);
        }
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tieneHospital = tieneHospital;
        this.nivelHospital = nivelHospital;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public boolean isTieneHospital() {
        return tieneHospital;
    }

    public int getNivelHospital() {
        return nivelHospital;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Municipio otro)) {
            return false;
        }
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}
