package co.rutavital.benchmark;

import co.rutavital.estrategia.EstrategiaDijkstra;
import co.rutavital.estrategia.EstrategiaNavegacion;
import co.rutavital.estrategia.FabricaEstrategias;
import co.rutavital.modelo.EstadoTramo;
import co.rutavital.modelo.Municipio;
import co.rutavital.modelo.RedVial;
import co.rutavital.modelo.Ruta;
import co.rutavital.repositorio.RepositorioRedVial;
import co.rutavital.servicio.NavegadorRutas;
import co.rutavital.servicio.ServicioRutasConCache;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Benchmark de las estrategias de navegación, fuera de Spring.
 * <p>
 * Ejecutar con {@code mvn -Pbenchmark} (ver README). Escribe en la carpeta resultados/:
 * <ul>
 *     <li>benchmark.csv: redes sintéticas tipo cuadrícula de 100, 1.000, 10.000 y 50.000 nodos.
 *     Por tamaño y estrategia: 10 consultas de calentamiento y 30 medidas con pares aleatorios.</li>
 *     <li>benchmark-red-real.csv: red real de Santander, todos los pares, resumen por estrategia.</li>
 *     <li>benchmark-red-real-pares.csv: detalle por par de la red real.</li>
 *     <li>benchmark-cache.csv: primera consulta (miss) frente a consulta repetida (hit) en el proxy.</li>
 *     <li>escenario-cierre-t06.csv: ruta San Gil -> Bucaramanga antes y después de cerrar T06.</li>
 *     <li>benchmark-entorno.txt: máquina y JVM donde se midió.</li>
 * </ul>
 */
public final class BenchmarkNavegacion {

    private static final long SEMILLA = 20260928L;
    private static final int CALENTAMIENTO = 10;
    private static final int MEDICIONES = 30;
    /** Filas x columnas: 100, 1.000, 10.000 y 50.000 nodos. */
    private static final int[][] TAMANOS = {{10, 10}, {25, 40}, {100, 100}, {200, 250}};
    /** En la red real cada par se mide varias veces y se promedia, porque una consulta dura microsegundos. */
    private static final int REPETICIONES_RED_REAL = 20;
    private static final Path CARPETA = Path.of("resultados");
    private static final double TOLERANCIA = 1e-6;

    private record Medicion(double ms, int nodos, boolean encontrada, double tiempoRutaMin, double km, int tramos) {
    }

    private record Par(String origen, String destino) {
    }

    private BenchmarkNavegacion() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(CARPETA);
        List<EstrategiaNavegacion> estrategias = new FabricaEstrategias().todas();

        registrarEntorno();
        calentarJvm(estrategias);
        benchmarkSintetico(estrategias);
        benchmarkRedReal(estrategias);
        escenarioCierreT06();
        benchmarkCache();
        System.out.println("\nResultados escritos en " + CARPETA.toAbsolutePath());
    }

    /* ---------- Entorno ---------- */

    private static void registrarEntorno() throws IOException {
        Runtime rt = Runtime.getRuntime();
        String cpu = "desconocido";
        Path cpuinfo = Path.of("/proc/cpuinfo");
        if (Files.exists(cpuinfo)) {
            cpu = Files.readAllLines(cpuinfo).stream()
                    .filter(l -> l.startsWith("model name"))
                    .map(l -> l.substring(l.indexOf(':') + 1).trim())
                    .findFirst().orElse(cpu);
        }
        String texto = String.join(System.lineSeparator(),
                "Fecha: " + LocalDateTime.now().withNano(0),
                "Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")",
                "Sistema: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                        + " " + System.getProperty("os.arch"),
                "CPU: " + cpu,
                "Núcleos disponibles: " + rt.availableProcessors(),
                "Memoria máxima JVM: " + rt.maxMemory() / (1024 * 1024) + " MB",
                "Semilla: " + SEMILLA,
                "");
        Files.writeString(CARPETA.resolve("benchmark-entorno.txt"), texto, StandardCharsets.UTF_8);
        System.out.println("== Entorno\n" + texto);
    }

    /**
     * Calentamiento global de la JVM (no se registra) para que el compilador JIT no
     * perjudique a la primera estrategia o al primer tamaño que se mide.
     */
    private static void calentarJvm(List<EstrategiaNavegacion> estrategias) {
        RedVial red = GeneradorRedSintetica.cuadricula(25, 40, SEMILLA - 1);
        List<Par> pares = paresAleatorios(red, 300, new Random(SEMILLA - 1));
        for (EstrategiaNavegacion estrategia : estrategias) {
            for (Par par : pares) {
                estrategia.calcular(red, par.origen(), par.destino());
            }
        }
    }

    /* ---------- Redes sintéticas ---------- */

    private static void benchmarkSintetico(List<EstrategiaNavegacion> estrategias) throws IOException {
        System.out.println("\n== Redes sintéticas (" + CALENTAMIENTO + " de calentamiento + " + MEDICIONES
                + " medidas por estrategia)");
        System.out.printf(Locale.ROOT, "%8s %8s %-13s %10s %10s %12s %12s %10s%n",
                "nodos", "tramos", "estrategia", "prom ms", "p95 ms", "nodos expl.", "ruta min", "=Dijkstra");
        try (PrintWriter csv = escritor("benchmark.csv")) {
            csv.println("nodos,tramos,estrategia,consultas,tiempoPromedioMs,p95Ms,nodosExploradosPromedio,"
                    + "tiempoRutaPromedioMin,rutasEncontradas,coincidenConDijkstra");
            for (int[] tamano : TAMANOS) {
                int nodos = tamano[0] * tamano[1];
                RedVial red = GeneradorRedSintetica.cuadricula(tamano[0], tamano[1], SEMILLA + nodos);
                Random azar = new Random(SEMILLA * 31 + nodos);
                List<Par> calentamiento = paresAleatorios(red, CALENTAMIENTO, azar);
                List<Par> medidos = paresAleatorios(red, MEDICIONES, azar);

                List<Medicion> referencia = null;
                for (EstrategiaNavegacion estrategia : estrategias) {
                    for (Par par : calentamiento) {
                        estrategia.calcular(red, par.origen(), par.destino());
                    }
                    List<Medicion> mediciones = new ArrayList<>();
                    for (Par par : medidos) {
                        mediciones.add(medir(estrategia, red, par, 1));
                    }
                    if (estrategia.nombre().equals(EstrategiaDijkstra.NOMBRE)) {
                        referencia = mediciones;
                    }
                    int coinciden = contarCoincidencias(mediciones, referencia);
                    List<Double> ms = mediciones.stream().map(Medicion::ms).toList();
                    double nodosPromedio = mediciones.stream().mapToInt(Medicion::nodos).average().orElse(0);
                    double rutaPromedio = mediciones.stream().filter(Medicion::encontrada)
                            .mapToDouble(Medicion::tiempoRutaMin).average().orElse(0);
                    long encontradas = mediciones.stream().filter(Medicion::encontrada).count();

                    csv.printf(Locale.ROOT, "%d,%d,%s,%d,%.4f,%.4f,%.1f,%.2f,%d,%d%n", nodos, red.cantidadTramos(),
                            estrategia.nombre(), MEDICIONES, promedio(ms), percentil95(ms), nodosPromedio,
                            rutaPromedio, encontradas, coinciden);
                    System.out.printf(Locale.ROOT, "%8d %8d %-13s %10.4f %10.4f %12.1f %12.2f %7d/%d%n", nodos,
                            red.cantidadTramos(), estrategia.nombre(), promedio(ms), percentil95(ms), nodosPromedio,
                            rutaPromedio, coinciden, MEDICIONES);
                }
            }
        }
    }

    /* ---------- Red real de Santander ---------- */

    private static void benchmarkRedReal(List<EstrategiaNavegacion> estrategias) throws IOException {
        RedVial red = new RepositorioRedVial().getRedVial();
        List<Par> pares = todosLosPares(red);
        System.out.println("\n== Red real de Santander: " + red.cantidadMunicipios() + " municipios, "
                + red.cantidadTramos() + " tramos, " + pares.size() + " pares (cada par se mide "
                + REPETICIONES_RED_REAL + " veces y se promedia)");

        Map<String, List<Medicion>> porEstrategia = new LinkedHashMap<>();
        for (EstrategiaNavegacion estrategia : estrategias) {
            for (int i = 0; i < 5; i++) {
                for (Par par : pares) {
                    estrategia.calcular(red, par.origen(), par.destino());
                }
            }
            List<Medicion> mediciones = new ArrayList<>();
            for (Par par : pares) {
                mediciones.add(medir(estrategia, red, par, REPETICIONES_RED_REAL));
            }
            porEstrategia.put(estrategia.nombre(), mediciones);
        }

        List<Medicion> referencia = porEstrategia.get(EstrategiaDijkstra.NOMBRE);
        try (PrintWriter detalle = escritor("benchmark-red-real-pares.csv")) {
            detalle.println("origen,destino,estrategia,encontrada,tiempoRutaMin,distanciaKm,tramos,"
                    + "nodosExplorados,tiempoCalculoMs");
            for (Map.Entry<String, List<Medicion>> entrada : porEstrategia.entrySet()) {
                for (int i = 0; i < pares.size(); i++) {
                    Medicion m = entrada.getValue().get(i);
                    detalle.printf(Locale.ROOT, "%s,%s,%s,%b,%.2f,%.1f,%d,%d,%.5f%n", pares.get(i).origen(),
                            pares.get(i).destino(), entrada.getKey(), m.encontrada(), m.tiempoRutaMin(), m.km(),
                            m.tramos(), m.nodos(), m.ms());
                }
            }
        }

        System.out.printf(Locale.ROOT, "%-13s %10s %8s %8s %10s %10s %10s %10s %12s%n", "estrategia", "ruta min",
                "km", "tramos", "nodos", "prom ms", "p95 ms", "óptimos", "sobrecosto %");
        try (PrintWriter csv = escritor("benchmark-red-real.csv")) {
            csv.println("estrategia,pares,rutasEncontradas,tiempoRutaPromedioMin,distanciaPromedioKm,tramosPromedio,"
                    + "nodosExploradosPromedio,tiempoCalculoPromedioMs,p95Ms,paresConTiempoOptimo,sobrecostoPromedioPct");
            for (Map.Entry<String, List<Medicion>> entrada : porEstrategia.entrySet()) {
                List<Medicion> ms = entrada.getValue();
                List<Double> tiempos = ms.stream().map(Medicion::ms).toList();
                double sobrecosto = 0;
                for (int i = 0; i < ms.size(); i++) {
                    sobrecosto += (ms.get(i).tiempoRutaMin() / referencia.get(i).tiempoRutaMin() - 1) * 100;
                }
                sobrecosto /= ms.size();
                double rutaMin = ms.stream().mapToDouble(Medicion::tiempoRutaMin).average().orElse(0);
                double km = ms.stream().mapToDouble(Medicion::km).average().orElse(0);
                double tramos = ms.stream().mapToInt(Medicion::tramos).average().orElse(0);
                double nodos = ms.stream().mapToInt(Medicion::nodos).average().orElse(0);
                int optimos = contarCoincidencias(ms, referencia);
                long encontradas = ms.stream().filter(Medicion::encontrada).count();

                csv.printf(Locale.ROOT, "%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.5f,%.5f,%d,%.2f%n", entrada.getKey(),
                        pares.size(), encontradas, rutaMin, km, tramos, nodos, promedio(tiempos),
                        percentil95(tiempos), optimos, sobrecosto);
                System.out.printf(Locale.ROOT, "%-13s %10.2f %8.2f %8.2f %10.2f %10.5f %10.5f %6d/%d %12.2f%n",
                        entrada.getKey(), rutaMin, km, tramos, nodos, promedio(tiempos), percentil95(tiempos),
                        optimos, pares.size(), sobrecosto);
            }
        }
    }

    /* ---------- Escenario de la demo ---------- */

    private static void escenarioCierreT06() throws IOException {
        RedVial red = new RepositorioRedVial().getRedVial();
        EstrategiaNavegacion dijkstra = new EstrategiaDijkstra();
        Ruta antes = dijkstra.calcular(red, "SAN_GIL", "BUCARAMANGA");
        red.buscarTramo("T06").orElseThrow().setEstado(EstadoTramo.CERRADO);
        Ruta despues = dijkstra.calcular(red, "SAN_GIL", "BUCARAMANGA");

        System.out.println("\n== Escenario San Gil -> Bucaramanga (Dijkstra)");
        try (PrintWriter csv = escritor("escenario-cierre-t06.csv")) {
            csv.println("momento,municipios,tramos,distanciaKm,tiempoMin");
            escribirEscenario(csv, red, "antes del cierre de T06", antes);
            escribirEscenario(csv, red, "con T06 cerrado", despues);
        }
    }

    private static void escribirEscenario(PrintWriter csv, RedVial red, String momento, Ruta ruta) {
        String municipios = String.join(" > ", ruta.getMunicipios().stream()
                .map(id -> red.buscarMunicipio(id).map(Municipio::getNombre).orElse(id)).toList());
        csv.printf(Locale.ROOT, "%s,%s,%s,%.1f,%.1f%n", momento, municipios, String.join(" ", ruta.getTramos()),
                ruta.getDistanciaTotalKm(), ruta.getTiempoTotalMin());
        System.out.printf(Locale.ROOT, "%-24s %s | %s | %.1f km | %.1f min%n", momento, municipios, ruta.getTramos(),
                ruta.getDistanciaTotalKm(), ruta.getTiempoTotalMin());
    }

    /* ---------- Efecto de la caché ---------- */

    private static void benchmarkCache() throws IOException {
        System.out.println("\n== Efecto de la caché (DIJKSTRA): primera consulta (miss) vs consulta repetida (hit)");
        System.out.printf(Locale.ROOT, "%-18s %9s %-10s %10s %10s %8s %8s%n", "red", "consultas", "consulta",
                "prom ms", "p95 ms", "hits", "misses");
        FabricaEstrategias fabrica = new FabricaEstrategias();
        try (PrintWriter csv = escritor("benchmark-cache.csv")) {
            csv.println("red,consultas,consulta,tiempoPromedioMs,p95Ms,hits,misses");

            RedVial real = new RepositorioRedVial().getRedVial();
            medirCache("red-real-22", real, todosLosPares(real), fabrica, csv);

            RedVial grande = GeneradorRedSintetica.cuadricula(200, 250, SEMILLA + 50_000);
            List<Par> pares = paresAleatorios(grande, MEDICIONES, new Random(SEMILLA * 7));
            medirCache("sintetica-50000", grande, pares, fabrica, csv);
        }
    }

    private static void medirCache(String nombre, RedVial red, List<Par> pares, FabricaEstrategias fabrica,
                                   PrintWriter csv) {
        // Calentamiento con una caché aparte que se descarta
        ServicioRutasConCache previa = new ServicioRutasConCache(new NavegadorRutas(red, fabrica));
        for (int i = 0; i < 3; i++) {
            for (Par par : pares) {
                previa.calcularRuta(par.origen(), par.destino(), EstrategiaDijkstra.NOMBRE);
            }
        }

        ServicioRutasConCache cache = new ServicioRutasConCache(new NavegadorRutas(red, fabrica));
        List<Double> primera = new ArrayList<>();
        List<Double> repetida = new ArrayList<>();
        for (Par par : pares) {
            primera.add(cronometrar(() -> cache.calcularRuta(par.origen(), par.destino(), EstrategiaDijkstra.NOMBRE)));
        }
        // Los contadores se registran al terminar cada fase (acumulados)
        escribirFilaCache(csv, nombre, "primera", primera, cache);
        for (Par par : pares) {
            repetida.add(cronometrar(() -> cache.calcularRuta(par.origen(), par.destino(), EstrategiaDijkstra.NOMBRE)));
        }
        escribirFilaCache(csv, nombre, "repetida", repetida, cache);
        System.out.printf(Locale.ROOT, "%-18s aceleración de la consulta repetida: %.1fx%n", nombre,
                promedio(primera) / promedio(repetida));
    }

    private static void escribirFilaCache(PrintWriter csv, String red, String consulta, List<Double> tiempos,
                                          ServicioRutasConCache cache) {
        csv.printf(Locale.ROOT, "%s,%d,%s,%.5f,%.5f,%d,%d%n", red, tiempos.size(), consulta, promedio(tiempos),
                percentil95(tiempos), cache.getHits(), cache.getMisses());
        System.out.printf(Locale.ROOT, "%-18s %9d %-10s %10.5f %10.5f %8d %8d%n", red, tiempos.size(), consulta,
                promedio(tiempos), percentil95(tiempos), cache.getHits(), cache.getMisses());
    }

    /* ---------- Utilidades ---------- */

    private static Medicion medir(EstrategiaNavegacion estrategia, RedVial red, Par par, int repeticiones) {
        Ruta ruta = null;
        long total = 0;
        for (int i = 0; i < repeticiones; i++) {
            long inicio = System.nanoTime();
            ruta = estrategia.calcular(red, par.origen(), par.destino());
            total += System.nanoTime() - inicio;
        }
        double ms = total / (double) repeticiones / 1_000_000.0;
        return new Medicion(ms, ruta.getMetricas().getNodosExplorados(), ruta.isEncontrada(), ruta.getTiempoTotalMin(),
                ruta.getDistanciaTotalKm(), ruta.getTramos().size());
    }

    private static double cronometrar(Runnable accion) {
        long inicio = System.nanoTime();
        accion.run();
        return (System.nanoTime() - inicio) / 1_000_000.0;
    }

    private static int contarCoincidencias(List<Medicion> mediciones, List<Medicion> referencia) {
        int coinciden = 0;
        for (int i = 0; i < mediciones.size(); i++) {
            Medicion m = mediciones.get(i);
            Medicion r = referencia.get(i);
            if (m.encontrada() == r.encontrada() && Math.abs(m.tiempoRutaMin() - r.tiempoRutaMin()) < TOLERANCIA) {
                coinciden++;
            }
        }
        return coinciden;
    }

    private static List<Par> paresAleatorios(RedVial red, int cantidad, Random azar) {
        List<Municipio> municipios = red.getMunicipios();
        List<Par> pares = new ArrayList<>();
        while (pares.size() < cantidad) {
            Municipio a = municipios.get(azar.nextInt(municipios.size()));
            Municipio b = municipios.get(azar.nextInt(municipios.size()));
            if (!a.equals(b)) {
                pares.add(new Par(a.getId(), b.getId()));
            }
        }
        return pares;
    }

    private static List<Par> todosLosPares(RedVial red) {
        List<Par> pares = new ArrayList<>();
        for (Municipio a : red.getMunicipios()) {
            for (Municipio b : red.getMunicipios()) {
                if (!a.equals(b)) {
                    pares.add(new Par(a.getId(), b.getId()));
                }
            }
        }
        return pares;
    }

    private static double promedio(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /** Percentil 95 por el método del rango más cercano. */
    private static double percentil95(List<Double> valores) {
        List<Double> ordenados = new ArrayList<>(valores);
        Collections.sort(ordenados);
        int indice = (int) Math.ceil(0.95 * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, indice));
    }

    private static PrintWriter escritor(String archivo) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(CARPETA.resolve(archivo), StandardCharsets.UTF_8));
    }
}
