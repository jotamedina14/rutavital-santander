package co.rutavital.servicio;

import co.rutavital.modelo.Ruta;

/**
 * Servicio de cálculo de rutas. Es el "sujeto" del patrón Proxy:
 * lo implementan el servicio real ({@link NavegadorRutas}) y el proxy
 * con caché ({@link ServicioRutasConCache}).
 */
public interface ServicioRutas {

    Ruta calcularRuta(String origenId, String destinoId, String algoritmo);
}
