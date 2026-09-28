# RutaVital Santander

Prototipo académico de **navegación para ambulancias** entre municipios de Santander (Colombia). Calcula la
ruta de menor tiempo y la **recalcula automáticamente cuando se reporta un cierre vial**, por ejemplo un derrumbe
en el corredor del Cañón del Chicamocha.

> Es un prototipo: la red vial está simplificada (22 municipios y 25 tramos) y las coordenadas y distancias
> son aproximadas. No debe usarse para navegación real.

## El problema

En Santander, muchos traslados de pacientes hacia hospitales de mayor nivel dependen de pocos corredores
viales. Cuando uno se cierra (el Cañón del Chicamocha es el caso típico), la central de despachos tiene que
encontrar a mano una alternativa para cada ambulancia en camino. RutaVital:

- modela la red como un grafo cuyos tramos tienen tipo de vía (velocidad) y estado (abierto, restringido, cerrado);
- calcula rutas con varios algoritmos intercambiables y compara su desempeño;
- recalcula las rutas de los despachos activos en cuanto cambia el estado de un tramo;
- permite deshacer los cambios del operador y guarda una bitácora de eventos.

**Escenario de demostración.** Una ambulancia lleva a un paciente de San Gil a Bucaramanga (hospital de nivel 3).
La ruta normal va por el Chicamocha (T09, T08, T07, T06, T05, T02, T01: 103 km, 103 min). Al cerrar **T06** por un
derrumbe, el despacho se recalcula solo por Villanueva y Los Santos (T12, T11, T10, T02, T01: 121 km, 215,7 min).

## Arquitectura por capas

```
┌──────────────────────────────────────────────────────────────────────────┐
│ Presentación   static/ (index.html, app.js, estilos.css: Leaflet)        │
│                api/    RutaController, RedVialController,                │
│                        DespachoController, MetricasController,           │
│                        ManejadorErrores (@RestControllerAdvice)          │
├──────────────────────────────────────────────────────────────────────────┤
│ Aplicación     comando/  InvocadorComandos, Comando...        (Command)  │
│                evento/   CentroEstadoVial, ObservadorVial...  (Observer) │
│                servicio/ ServicioRutas, NavegadorRutas,                  │
│                          ServicioRutasConCache (Proxy), GestorDespachos  │
├──────────────────────────────────────────────────────────────────────────┤
│ Dominio        estrategia/ EstrategiaNavegacion + 3 algoritmos (Strategy)│
│                modelo/     Municipio, Tramo, RedVial, Ruta, Despacho...  │
├──────────────────────────────────────────────────────────────────────────┤
│ Datos          repositorio/ RepositorioRedVial ← red-vial-santander.json │
└──────────────────────────────────────────────────────────────────────────┘
```

Cada capa solo usa la de abajo. No hay base de datos: la red se carga del JSON al iniciar y el estado
(tramos, despachos, caché, bitácora) vive en memoria.

## Patrones de diseño

| Patrón | Dónde | Participantes | Para qué |
|---|---|---|---|
| **Strategy** | `co.rutavital.estrategia` | `EstrategiaNavegacion` (estrategia), `EstrategiaDijkstra`, `EstrategiaAEstrella`, `EstrategiaMenosTramos` (concretas), `NavegadorRutas` (contexto), `FabricaEstrategias` | Cambiar el algoritmo de búsqueda por nombre sin tocar el resto del sistema. |
| **Proxy** | `co.rutavital.servicio` | `ServicioRutas` (sujeto), `NavegadorRutas` (sujeto real), `ServicioRutasConCache` (proxy) | Guardar rutas ya calculadas (clave `origen\|destino\|algoritmo`) con la misma interfaz. Se activa con `rutavital.cache.habilitada`. |
| **Observer** | `co.rutavital.evento` | `CentroEstadoVial` (sujeto), `ObservadorVial`, `InvalidadorCache`, `RecalculadorDespachos`, `BitacoraEventos` | Reaccionar a un cambio de estado de un tramo: invalidar la caché, recalcular despachos y registrar el evento, en ese orden. |
| **Command** | `co.rutavital.comando` | `Comando`, `ComandoCambiarEstadoTramo`, `ComandoCrearDespacho`, `InvocadorComandos` (pila) | Encapsular las acciones del operador para poder deshacer la última. |

**Algoritmos.**

- **Dijkstra**: cola de prioridad; el peso de cada tramo es su tiempo estimado.
- **A\***: usa la heurística *distancia haversine / 60 km/h*. Es admisible porque 60 km/h es la velocidad máxima de la red y ningún tramo es más corto que la línea recta entre sus extremos. Por eso da el mismo tiempo que Dijkstra explorando menos nodos.
- **Menos tramos** (BFS): minimiza el número de tramos y sirve de línea base.

Tiempo de un tramo = `distanciaKm / velocidad del tipo de vía * 60 * factor del estado`:

- **Tipo de vía**: principal 60 km/h, secundaria 40 km/h, destapada 25 km/h.
- **Estado**: abierto ×1,0, restringido ×1,6, cerrado no transitable.

## Requisitos

- Java 17 o superior. Maven es opcional: el proyecto incluye el wrapper `./mvnw` (`mvnw.cmd` en Windows).
- Para las pruebas de carga: Python 3 y Locust (`pip install locust`). JMeter 5.6+ es opcional.

## Cómo ejecutar

```bash
mvn spring-boot:run          # o ./mvnw spring-boot:run
```

Abrir <http://localhost:8080>.

Sin caché (el `ServicioRutas` principal pasa a ser `NavegadorRutas` directamente):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--rutavital.cache.habilitada=false
```

También se puede empaquetar: `mvn package` y luego `java -jar target/rutavital-santander-0.1.0.jar`.

### Recorrido sugerido para la demo

1. En **Calcular ruta** deja San Gil → Bucaramanga y pulsa *Calcular ruta*. La ruta se resalta en amarillo y el
   panel muestra el tiempo, los km, los nodos explorados, el tiempo de cálculo y si vino de la caché.
2. Pulsa *Comparar algoritmos* para ver la tabla con las tres estrategias.
3. En **Despachos activos** pulsa *Crear despacho*: su ruta aparece en azul.
4. Haz clic en el tramo **T06** (Pescadero – Parque Chicamocha), escribe un motivo y pulsa *Cerrar*. El tramo
   queda en rojo punteado, el despacho muestra "Recalculada 1 vez", su ruta nueva va en azul por Villanueva y
   la anterior en gris punteado. La bitácora registra el evento.
5. Pulsa *Deshacer última acción*: T06 se reabre y el despacho vuelve a la ruta original.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/rutas?origen=&destino=&algoritmo=` | Ruta con el servicio principal (con o sin caché). `algoritmo`: `DIJKSTRA` (por defecto), `A_ESTRELLA`, `MENOS_TRAMOS`. Si no hay ruta responde 200 con `encontrada=false` y un mensaje. |
| GET | `/api/rutas/comparar?origen=&destino=` | Ejecuta las 3 estrategias **sin caché** y devuelve rutas y métricas. |
| GET | `/api/municipios` | Municipios con coordenadas y nivel de hospital. |
| GET | `/api/tramos` | Tramos con estado y tiempo estimado. |
| POST | `/api/tramos/{id}/estado` | Cuerpo `{"estado":"CERRADO","motivo":"Derrumbe sector Pescadero"}`. Pasa por `InvocadorComandos`. |
| POST | `/api/comandos/deshacer` | Deshace la última acción. |
| GET | `/api/comandos/historial` | Pila de comandos (el primero es el próximo a deshacer). |
| GET | `/api/eventos` | Bitácora de eventos viales (más reciente primero). |
| POST | `/api/despachos` | Cuerpo `{"origen":"SAN_GIL","destino":"BUCARAMANGA"}`. Pasa por `InvocadorComandos`. Responde 201. |
| GET | `/api/despachos` | Todos los despachos; `?soloActivos=true` para solo los activos. |
| POST | `/api/despachos/{id}/finalizar` | Finaliza un despacho. |
| GET | `/api/metricas/cache` | `habilitada`, `hits`, `misses`, `tasaAcierto` (0 a 1) y `entradas`. |

Errores: un municipio, tramo o despacho inexistente responde **404**; parámetros o cuerpos inválidos, **400**.
Siempre con el cuerpo `{estado, error, mensaje, ruta, fechaHora}`.

Ejemplo:

```bash
curl "http://localhost:8080/api/rutas?origen=SAN_GIL&destino=BUCARAMANGA&algoritmo=A_ESTRELLA"
curl -X POST -H "Content-Type: application/json" \
     -d '{"estado":"CERRADO","motivo":"Derrumbe sector Pescadero"}' \
     http://localhost:8080/api/tramos/T06/estado
```

## Pruebas

```bash
mvn test
```

Son 60 pruebas JUnit 5. Las que corresponden a los requisitos del proyecto:

| # | Qué verifica | Dónde |
|---|---|---|
| 1 | Dijkstra y A* dan el mismo tiempo total en los 462 pares de la red real (en 3 escenarios) y se registran los nodos explorados | `EstrategiasNavegacionTest.dijkstraYAEstrellaCoincidenEnTodosLosPares` (detalle en `target/pruebas/`) |
| 2 | Con T06 CERRADO la ruta San Gil → Bucaramanga no contiene T06 y tarda más | `EstrategiasNavegacionTest.cierreDeT06ObligaARutaAlterna` |
| 3 | RESTRINGIDO multiplica el tiempo del tramo por 1,6 | `TramoTest.restringidoMultiplicaPorFactor` |
| 4 | Deshacer un cierre deja el tramo ABIERTO y la ruta vuelve a ser la original | `InvocadorComandosTest.deshacerCierreRestauraLaRuta` |
| 5 | El Observer recalcula el despacho activo afectado y guarda la ruta anterior | `ObservadoresVialesTest.recalculaDespachoAfectado` |
| 6 | La caché da hit en la segunda consulta igual y se invalida al cambiar un tramo | `ObservadoresVialesTest.laCacheSeInvalidaAlCambiarUnTramo`, `ServicioRutasConCacheTest` |
| 7 | Sin ruta posible (tramos de Oiba cerrados) devuelve `encontrada=false` | `EstrategiasNavegacionTest.sinRutaPosible` |
| 8 | Controladores con MockMvc para `/api/rutas` y `/api/tramos/{id}/estado` | `RutaControllerTest`, `RedVialControllerTest` |

## Benchmark

```bash
mvn -Pbenchmark              # equivale a: mvn -Pbenchmark compile exec:exec
```

`BenchmarkNavegacion` corre fuera de Spring, en una JVM aparte, y escribe en `resultados/`:

| Archivo | Contenido |
|---|---|
| `benchmark.csv` | Redes tipo cuadrícula de 100, 1.000, 10.000 y 50.000 nodos (semilla fija). Por tamaño y estrategia: 10 consultas de calentamiento y 30 medidas con pares aleatorios. Tiempo promedio, p95, nodos explorados. |
| `benchmark-red-real.csv` | Red de Santander, los 462 pares, resumen por estrategia. |
| `benchmark-red-real-pares.csv` | Detalle por par de la red real. |
| `benchmark-cache.csv` | Primera consulta (miss) frente a consulta repetida (hit) en el proxy, en la red real y en una de 50.000 nodos. |
| `escenario-cierre-t06.csv` | Ruta San Gil → Bucaramanga antes y después de cerrar T06. |
| `benchmark-entorno.txt` | Máquina, JVM y semilla usadas. |

Antes de medir se hace un calentamiento global de la JVM que no se registra, para que el JIT no perjudique a la
primera estrategia medida.

## Pruebas de carga

Con Locust (`pruebas-carga/locustfile.py`). Cada usuario ejecuta estas tareas con pesos 5, 1 y 2, y espera entre 0,1 y 0,5 s entre una y otra:

- calcular una ruta entre un par aleatorio (peso 5);
- comparar algoritmos (peso 1);
- listar despachos (peso 2).

```bash
./pruebas-carga/ejecutar-carga.sh                                     # Linux / macOS
powershell -ExecutionPolicy Bypass -File pruebas-carga\ejecutar-carga.ps1   # Windows
```

Cada script compila la aplicación y la arranca dos veces, con `rutavital.cache.habilitada=true` y con `false`.
En cada arranque corre Locust en modo headless (50 usuarios, 10 por segundo, 60 s) y guarda los resultados en
`resultados/carga-con-cache/` y `resultados/carga-sin-cache/`. Esos resultados incluyen los CSV de Locust, el reporte HTML,
el resumen de consola y las métricas de la caché al terminar. El puerto 8080 debe estar libre.

Plan equivalente para JMeter (opcional), con la aplicación ya corriendo:

```bash
jmeter -n -t pruebas-carga/plan-rutas.jmx -l resultados/jmeter/resultados.jtl -e -o resultados/jmeter/reporte
# propiedades opcionales: -Jusuarios=50 -Jrampa=5 -Jduracion=60 -Jhost=localhost -Jpuerto=8080
```

## Resultados

Los datos medidos están en [`resultados/RESUMEN_RESULTADOS.md`](resultados/RESUMEN_RESULTADOS.md).

## Estructura del proyecto

```
rutavital-santander/
├── src/main/java/co/rutavital/
│   ├── modelo/        Municipio, Tramo, TipoVia, EstadoTramo, RedVial, Ruta, MetricasBusqueda, Despacho
│   ├── estrategia/    EstrategiaNavegacion, EstrategiaDijkstra, EstrategiaAEstrella, EstrategiaMenosTramos, FabricaEstrategias
│   ├── servicio/      ServicioRutas, NavegadorRutas, ServicioRutasConCache, GestorDespachos
│   ├── evento/        EventoVial, ObservadorVial, CentroEstadoVial, RecalculadorDespachos, InvalidadorCache, BitacoraEventos
│   ├── comando/       Comando, ComandoCambiarEstadoTramo, ComandoCrearDespacho, InvocadorComandos
│   ├── repositorio/   RepositorioRedVial
│   ├── api/           controladores REST, ManejadorErrores y dto/
│   ├── benchmark/     BenchmarkNavegacion, GeneradorRedSintetica
│   ├── config/        ConfiguracionRutaVital
│   └── excepcion/     excepciones de dominio
├── src/main/resources/
│   ├── red-vial-santander.json
│   └── static/        index.html, app.js, estilos.css
├── src/test/java/     pruebas JUnit 5
├── pruebas-carga/     locustfile.py, ejecutar-carga.sh, ejecutar-carga.ps1, plan-rutas.jmx
├── resultados/        CSV del benchmark y de las pruebas de carga, RESUMEN_RESULTADOS.md
├── docs/uml/          diagramas UML
├── docs/capturas/     capturas de pantalla
└── CAMBIOS_ARQUITECTURA.md
```

## Capturas

Guarda las imágenes en `docs/capturas/` y descomenta la línea correspondiente.

<!-- ![Vista general del mapa y la red vial](docs/capturas/01-vista-general.png) -->
<!-- ![Ruta San Gil - Bucaramanga por el Chicamocha](docs/capturas/02-ruta-normal.png) -->
<!-- ![Comparación de algoritmos](docs/capturas/03-comparar-algoritmos.png) -->
<!-- ![Cierre de T06 y despacho recalculado](docs/capturas/04-cierre-t06-recalculo.png) -->
<!-- ![Bitácora de eventos y deshacer](docs/capturas/05-bitacora-deshacer.png) -->

## Cambios frente al diseño

Todos los nombres de paquetes, clases, interfaces y métodos del diseño se respetaron. El único cambio es un dato
de la red (T02 pasa de 9 a 10 km para que la heurística de A* sea admisible). Ese cambio, junto con las precisiones de firmas
y las clases auxiliares, está documentado en [`CAMBIOS_ARQUITECTURA.md`](CAMBIOS_ARQUITECTURA.md).

## Limitaciones

- Red simplificada con datos aproximados; los tiempos no consideran tráfico, pendiente ni clima.
- El estado vive en memoria: al reiniciar la aplicación se pierden despachos, bitácora y cierres.
- Sin autenticación: cualquier cliente puede cambiar el estado de los tramos.
