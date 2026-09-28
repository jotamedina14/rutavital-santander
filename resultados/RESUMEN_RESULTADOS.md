# Resumen de resultados

Todos los valores de este documento salen de ejecuciones reales. Cada sección indica el archivo de origen,
que está en esta misma carpeta, salvo las pruebas unitarias, que salen de la salida de `mvn test`.

**Entorno de medición** (`benchmark-entorno.txt`)

| | |
|---|---|
| Fecha | 2026-09-28 |
| Java | 21.0.12.1 (OpenJDK 64-Bit Server VM, Temurin) |
| Sistema | Linux 7.0.0-34-generic amd64 |
| CPU | AMD Ryzen 7 250 w/ Radeon 780M Graphics, 16 núcleos lógicos |
| Memoria máxima de la JVM del benchmark | 2048 MB (`-Xmx2g`) |
| Semilla de las redes sintéticas | 20260928 |

---

## 1. Pruebas automatizadas (`mvn test`)

**60 pruebas: 60 exitosas, 0 fallos, 0 errores, 0 omitidas. BUILD SUCCESS.**

| Clase de prueba | Pruebas | Resultado |
|---|---:|---|
| `modelo.TramoTest` | 6 | OK |
| `modelo.RedVialTest` | 3 | OK |
| `repositorio.RepositorioRedVialTest` | 2 | OK |
| `estrategia.EstrategiasNavegacionTest` | 11 | OK |
| `servicio.ServicioRutasConCacheTest` | 4 | OK |
| `servicio.GestorDespachosTest` | 3 | OK |
| `evento.ObservadoresVialesTest` | 10 | OK |
| `comando.InvocadorComandosTest` | 6 | OK |
| `api.RutaControllerTest` (MockMvc) | 6 | OK |
| `api.RedVialControllerTest` (MockMvc) | 7 | OK |
| `config.ConfiguracionCacheTest` (con caché / sin caché) | 2 | OK |
| **Total** | **60** | **OK** |

Las 8 pruebas pedidas están cubiertas; el mapa de prueba → método está en el README.

**Prueba 1: nodos explorados registrados.** Dijkstra y A* dieron el mismo tiempo total en los 462 pares
ordenados de la red real, en tres escenarios:

| Escenario | Pares | Nodos explorados Dijkstra (total / promedio) | Nodos explorados A* (total / promedio) |
|---|---:|---:|---:|
| Todo abierto | 462 | 5.544 / 12,00 | 3.905 / 8,45 |
| T06 cerrado | 462 | 5.544 / 12,00 | 4.641 / 10,05 |
| T05 restringido y T11 cerrado | 462 | 5.544 / 12,00 | 4.125 / 8,93 |

Dijkstra siempre promedia 12,00 nodos: con la red conexa se detiene al extraer el destino, y en promedio el destino
es el nodo 12 de 22 en orden de distancia. El detalle por par queda en `target/pruebas/` al ejecutar las pruebas.

---

## 2. Benchmark en redes sintéticas (`benchmark.csv`)

Redes tipo cuadrícula con semilla fija. Por tamaño y estrategia hubo 10 consultas de calentamiento y 30 medidas,
con los mismos 30 pares aleatorios para las tres estrategias. Ejecución: `mvn -Pbenchmark`.

| Nodos | Tramos | Estrategia | Tiempo prom. (ms) | p95 (ms) | Nodos explorados prom. | Tiempo de ruta prom. (min) | Rutas con el tiempo de Dijkstra |
|---:|---:|---|---:|---:|---:|---:|---:|
| 100 | 180 | Dijkstra | 0,0354 | 0,0590 | 54,0 | 13,51 | 30/30 |
| 100 | 180 | A* | 0,0333 | 0,0719 | 31,5 | 13,51 | 30/30 |
| 100 | 180 | Menos tramos | 0,0179 | 0,0337 | 54,6 | 16,29 | 9/30 |
| 1.000 | 1.935 | Dijkstra | 0,2990 | 0,5538 | 544,4 | 42,16 | 30/30 |
| 1.000 | 1.935 | A* | 0,2085 | 0,5368 | 332,8 | 42,16 | 30/30 |
| 1.000 | 1.935 | Menos tramos | 0,1339 | 0,2482 | 539,0 | 53,60 | 0/30 |
| 10.000 | 19.800 | Dijkstra | 3,8052 | 6,4517 | 5.303,5 | 116,95 | 30/30 |
| 10.000 | 19.800 | A* | 2,2684 | 5,7592 | 2.810,9 | 116,95 | 30/30 |
| 10.000 | 19.800 | Menos tramos | 1,1515 | 2,4335 | 5.164,3 | 152,51 | 0/30 |
| 50.000 | 99.550 | Dijkstra | 16,2822 | 29,8160 | 26.924,1 | 275,69 | 30/30 |
| 50.000 | 99.550 | A* | 10,7938 | 22,1781 | 14.519,4 | 275,69 | 30/30 |
| 50.000 | 99.550 | Menos tramos | 9,6082 | 23,0935 | 26.884,5 | 375,69 | 0/30 |

Comparaciones calculadas a partir de la tabla:

| Nodos | A* frente a Dijkstra: nodos explorados | A* frente a Dijkstra: tiempo prom. | Menos tramos: tiempo de ruta prom. sobre Dijkstra |
|---:|---:|---:|---:|
| 100 | −41,7 % | −5,9 % | +20,6 % |
| 1.000 | −38,9 % | −30,3 % | +27,1 % |
| 10.000 | −47,0 % | −40,4 % | +30,4 % |
| 50.000 | −46,1 % | −33,7 % | +36,3 % |

- A* encontró siempre el mismo tiempo óptimo que Dijkstra (120/120 consultas) explorando entre 39 % y 47 % menos nodos.
- Menos tramos es la más rápida de calcular, pero sus rutas son más lentas y desde 1.000 nodos nunca coincidió con la óptima.

---

## 3. Red real de Santander (`benchmark-red-real.csv`)

22 municipios, 25 tramos, todos abiertos. Se midieron los 462 pares ordenados; cada par se ejecutó 20 veces
y se promedió, tras 5 rondas de calentamiento.

| Estrategia | Tiempo de ruta prom. (min) | Distancia prom. (km) | Tramos prom. | Nodos explorados prom. | Cálculo prom. (ms) | Cálculo p95 (ms) | Pares con tiempo óptimo | Sobrecosto prom. |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Dijkstra | 89,50 | 72,55 | 4,62 | 12,00 | 0,00949 | 0,01636 | 462/462 | 0,00 % |
| A* | 89,50 | 72,55 | 4,62 | 8,45 | 0,00778 | 0,01467 | 462/462 | 0,00 % |
| Menos tramos | 127,19 | 77,87 | 3,57 | 12,00 | 0,00118 | 0,00192 | 298/462 | 34,25 % |

- En la red real A* explora 29,6 % menos nodos que Dijkstra y calcula un 18,0 % más rápido, con el mismo resultado.
- Menos tramos da una ruta más lenta que la óptima en 164 de los 462 pares; en promedio tarda un 34,25 % más.

---

## 4. Escenario de la demo: San Gil → Bucaramanga (`escenario-cierre-t06.csv`)

Ruta calculada con Dijkstra; A* da el mismo resultado (prueba 2).

| Momento | Municipios | Tramos | Distancia | Tiempo |
|---|---|---|---:|---:|
| Antes del cierre de T06 | San Gil → Curití → Aratoca → Parque Chicamocha → Pescadero → Piedecuesta → Floridablanca → Bucaramanga | T09 T08 T07 T06 T05 T02 T01 | 103,0 km | 103,0 min |
| Con T06 cerrado | San Gil → Villanueva → Los Santos → Piedecuesta → Floridablanca → Bucaramanga | T12 T11 T10 T02 T01 | 121,0 km | 215,7 min |

El cierre de T06 obliga a tomar la vía destapada Los Santos – Villanueva (T11). El recorrido crece 18 km, pero el
tiempo pasa de 103 a 215,7 min (+112,7 min). En la aplicación el despacho activo se recalcula solo y la ruta de
103 min queda en su historial. Esto también se verificó por la API y en la interfaz web.

---

## 5. Efecto de la caché

### 5.1 Microbenchmark del proxy (`benchmark-cache.csv`)

Algoritmo DIJKSTRA. Se mide primero cada par por primera vez (miss) y luego repetido (hit).

| Red | Consultas | Consulta | Tiempo prom. (ms) | p95 (ms) | Hits acumulados | Misses acumulados |
|---|---:|---|---:|---:|---:|---:|
| Real (22 nodos) | 462 | Primera (miss) | 0,00443 | 0,00589 | 0 | 462 |
| Real (22 nodos) | 462 | Repetida (hit) | 0,00093 | 0,00115 | 462 | 462 |
| Sintética (50.000 nodos) | 30 | Primera (miss) | 13,55699 | 29,69220 | 0 | 30 |
| Sintética (50.000 nodos) | 30 | Repetida (hit) | 0,00072 | 0,00097 | 30 | 30 |

Aceleración de la consulta repetida (cociente de promedios):

- red real: unas 4,8 veces (0,00443 / 0,00093);
- red de 50.000 nodos: unas 18.800 veces (13,557 / 0,00072).

### 5.2 Caché durante la prueba de carga (`carga-con-cache/metricas-cache.json`)

| Hits | Misses | Tasa de acierto | Entradas al final |
|---:|---:|---:|---:|
| 4.634 | 1.368 | 77,2 % | 1.368 |

La red real admite 22 × 21 × 3 = 1.386 claves posibles (par ordenado × algoritmo). Al terminar la prueba había
1.368 en caché, así que la tasa de acierto sube a medida que se llenan.

---

## 6. Prueba de carga con y sin caché

Locust 2.46.6 en modo headless: 50 usuarios, 10 nuevos por segundo, 60 s. Cada usuario espera entre 0,1 y 0,5 s
entre tareas; la mezcla es calcular ruta, comparar y listar despachos con pesos 5, 1 y 2. La aplicación y Locust
corrieron en la misma máquina. Ejecución: `./pruebas-carga/ejecutar-carga.sh`.

Fuente: resumen final de Locust (`carga-*/locust-consola.txt`). Locust informa los percentiles en milisegundos enteros.

| Escenario | Peticiones | req/s | Mediana (ms) | p95 (ms) | p99 (ms) | Máx. (ms) | Fallos |
|---|---:|---:|---:|---:|---:|---:|---:|
| Con caché | 9.598 | 160,30 | 1 | 2 | 4 | 48 | 0 (0,00 %) |
| Sin caché | 9.647 | 161,12 | 1 | 2 | 4 | 77 | 0 (0,00 %) |

Por endpoint:

| Escenario | Endpoint | Peticiones | req/s | Mediana (ms) | p95 (ms) | Fallos |
|---|---|---:|---:|---:|---:|---:|
| Con caché | `GET /api/rutas` | 5.999 | 100,19 | 1 | 2 | 0 |
| Con caché | `GET /api/rutas/comparar` | 1.171 | 19,56 | 1 | 2 | 0 |
| Con caché | `GET /api/despachos` | 2.428 | 40,55 | 1 | 2 | 0 |
| Sin caché | `GET /api/rutas` | 6.069 | 101,36 | 1 | 2 | 0 |
| Sin caché | `GET /api/rutas/comparar` | 1.211 | 20,23 | 1 | 2 | 0 |
| Sin caché | `GET /api/despachos` | 2.367 | 39,53 | 1 | 2 | 0 |

Tiempo de respuesta promedio con decimales (`carga-*/locust_stats.csv`, fila *Aggregated*): 1,351 ms con caché y
1,328 ms sin caché. Ese CSV se escribe aproximadamente un segundo antes del cierre, por eso sus conteos (9.458 y
9.512) son algo menores que los del resumen final.

**Lectura de los resultados.** Con esta red y esta carga, la caché **no produce una diferencia medible** en la
prueba HTTP:

- **El throughput está limitado por la espera de los usuarios, no por el servidor.** Con una espera promedio de
  0,3 s y alrededor de 1,3 ms de respuesta, 50 usuarios pueden generar como máximo unas 166 req/s (cota
  calculada, no medida). Los dos escenarios quedaron en 160–161 req/s.
- **El cálculo de una ruta en 22 nodos es mínimo frente a la petición HTTP.** Tarda alrededor de 0,009 ms
  (sección 3), menos del 1 % de los ~1,3 ms de cada respuesta. Aunque la caché acierta el 77 % de las veces, el
  ahorro queda por debajo de la resolución de Locust.
- **La caché importa cuando el cálculo es costoso.** En la red sintética de 50.000 nodos, una consulta repetida
  pasa de 13,6 ms a 0,0007 ms (sección 5.1).

---

## 7. Archivos

| Archivo | Contenido |
|---|---|
| `benchmark.csv` | Redes sintéticas (sección 2) |
| `benchmark-red-real.csv` | Red real, resumen por estrategia (sección 3) |
| `benchmark-red-real-pares.csv` | Red real, detalle de los 462 pares × 3 estrategias |
| `benchmark-cache.csv` | Primera consulta frente a repetida (sección 5.1) |
| `escenario-cierre-t06.csv` | Escenario de la demo (sección 4) |
| `benchmark-entorno.txt` | Máquina y JVM |
| `carga-con-cache/`, `carga-sin-cache/` | Salidas de Locust: `locust_stats.csv`, `locust_stats_history.csv`, `locust_failures.csv`, `locust_exceptions.csv`, `reporte.html`, `locust-consola.txt` y `metricas-cache.json` |

El plan de JMeter (`pruebas-carga/plan-rutas.jmx`) se validó con una corrida corta de 10 usuarios durante 10 s:
293 peticiones y 0 errores. No se usó para las cifras de este resumen.
