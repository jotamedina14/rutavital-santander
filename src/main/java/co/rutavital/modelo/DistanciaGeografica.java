package co.rutavital.modelo;

/**
 * Utilidad para calcular distancias en línea recta sobre la superficie terrestre.
 */
public final class DistanciaGeografica {

    public static final double RADIO_TIERRA_KM = 6371.0;

    private DistanciaGeografica() {
    }

    /**
     * Distancia haversine en kilómetros entre dos coordenadas (en grados).
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
    }

    public static double haversineKm(Municipio a, Municipio b) {
        return haversineKm(a.getLatitud(), a.getLongitud(), b.getLatitud(), b.getLongitud());
    }
}
