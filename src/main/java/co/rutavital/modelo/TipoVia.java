package co.rutavital.modelo;

/**
 * Tipo de vía con su velocidad de referencia para una ambulancia.
 */
public enum TipoVia {

    PRINCIPAL(60),
    SECUNDARIA(40),
    DESTAPADA(25);

    private final int velocidadKmH;

    TipoVia(int velocidadKmH) {
        this.velocidadKmH = velocidadKmH;
    }

    public int getVelocidadKmH() {
        return velocidadKmH;
    }

    /**
     * Velocidad máxima entre todos los tipos de vía. La usa la heurística de A*
     * para no sobreestimar el tiempo restante.
     */
    public static int velocidadMaximaKmH() {
        int maxima = 0;
        for (TipoVia tipo : values()) {
            maxima = Math.max(maxima, tipo.velocidadKmH);
        }
        return maxima;
    }
}
