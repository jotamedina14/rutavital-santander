# Cambios y precisiones de arquitectura

Este documento sirve de referencia para los diagramas UML del informe.

**Se respetaron exactamente** todos los paquetes, clases, interfaces y métodos del diseño original
(paquete base `co.rutavital`). No se renombró ni se eliminó ninguno. Abajo se listan:

1. El único **cambio** respecto al diseño (un dato de la red vial).
2. Las **precisiones** de firmas que el diseño dejaba abiertas (tipos de retorno, métodos adicionales).
3. Las **clases auxiliares** que se agregaron.

---

## 1. Cambio en los datos: tramo T02 pasa de 9 km a 10 km

| Tramo | Extremos | Diseño original | Implementado |
|---|---|---|---|
| T02 | Floridablanca – Piedecuesta | 9 km | **10 km** |

**Motivo.** La heurística de A* (distancia haversine / 60 km/h) solo es admisible si ningún tramo es más
corto que la línea recta entre sus extremos. Con las coordenadas dadas, la línea recta entre Floridablanca
(7.0622, -73.0864) y Piedecuesta (6.9878, -73.0497) mide **9,21 km**, más que los 9 km del tramo. Con ese dato
la heurística sobreestimaba el tiempo restante y la afirmación "A* es admisible" dejaba de ser cierta.
La distancia real por la autopista ronda los 10–11 km, así que se usó 10 km.

**Efecto.** La ruta normal San Gil → Bucaramanga mide 103 km / 103 min (con 9 km habrían sido 102). El
comportamiento del escenario de demostración no cambia: al cerrar T06 la ruta pasa por Villanueva y Los Santos.

**Verificación.** La prueba `RepositorioRedVialTest.losDatosPermitenUnaHeuristicaAdmisible` comprueba que
todos los tramos cumplen `haversine ≤ distanciaKm`.

---

## 2. Precisiones de firmas (lo que el diseño no especificaba)

### co.rutavital.modelo

| Clase | Precisión |
|---|---|
| `Tramo` | `tiempoEstimadoMin()` devuelve `Double.POSITIVE_INFINITY` si está CERRADO. Métodos extra: `esTransitable()`, `conecta(municipioId)`, `otroExtremo(municipioId)`, `setEstado(EstadoTramo)`. El campo `estado` es `volatile`. |
| `TipoVia` | Getter `getVelocidadKmH()` y método estático `velocidadMaximaKmH()` (60), usado por la heurística de A*. |
| `EstadoTramo` | CERRADO tiene factor `+∞`. Métodos extra: `getFactor()`, `getDescripcion()`, `esTransitable()`, `esMejorQue(EstadoTramo)`. |
| `RedVial` | `vecinos(municipioId)` devuelve `List<Tramo>` (tramos incidentes en cualquier estado; las estrategias filtran los cerrados). `buscarTramo(id)` y `buscarMunicipio(id)` devuelven `Optional`. Métodos extra: `agregarMunicipio`, `agregarTramo`, `existeMunicipio`, `cantidadMunicipios`, `cantidadTramos`. |
| `Ruta` | Inmutable. Se crea con las fábricas estáticas `Ruta.exitosa(...)` y `Ruta.sinRuta(...)`. Métodos extra: `conMetricas(MetricasBusqueda)` (copia usada por la caché), `contieneTramo(tramoId)`, `esEquivalenteA(Ruta)`. |
| `MetricasBusqueda` | Getter adicional `getTiempoCalculoMs()`. En un acierto de caché: `nodosExplorados = 0`, `tiempoCalculoNanos` = tiempo de responder desde la caché y `desdeCache = true`. |
| `Despacho` | El estado es el enum `EstadoDespacho` (ACTIVO / FINALIZADO). Campo extra `fechaCreacion`. Métodos: `actualizarRuta(Ruta)` (mueve la ruta actual al historial e incrementa `vecesRecalculada`), `finalizar()`, `estaActivo()`. |

### co.rutavital.estrategia

| Clase | Precisión |
|---|---|
| `FabricaEstrategias` | `obtener(String nombre)` devuelve la estrategia; acepta variantes como `a-estrella`, y null o vacío equivale a DIJKSTRA. Métodos extra: `nombresDisponibles()`, `todas()`, `normalizar(String)` (estático). Un nombre desconocido lanza `AlgoritmoNoSoportadoException` (HTTP 400). |
| `EstrategiaDijkstra` / `EstrategiaAEstrella` / `EstrategiaMenosTramos` | Constante pública `NOMBRE`. `nodosExplorados` cuenta los nodos extraídos de la cola y expandidos. A* reabre nodos si encuentra un camino mejor, así sigue siendo correcto aunque la heurística solo fuera admisible y no consistente. |

### co.rutavital.servicio

| Clase | Precisión |
|---|---|
| `NavegadorRutas` | Constructor `NavegadorRutas(RedVial, FabricaEstrategias)`. Si el algoritmo es null usa DIJKSTRA. |
| `ServicioRutasConCache` | Constructor `ServicioRutasConCache(NavegadorRutas)`. Anotado `@Primary` y `@ConditionalOnProperty(rutavital.cache.habilitada=true, matchIfMissing=true)`. Métodos extra: `getTasaAcierto()`, `getEntradas()`, `reiniciarContadores()`, `clave(...)` (estático). Cada entrada guarda una "generación": `limpiar()` la incrementa, así una ruta calculada durante un cambio de red nunca se sirve después de invalidar. |
| `GestorDespachos` | `crear(origenId, destinoId)` y `crear(origenId, destinoId, algoritmo)` devuelven `Despacho`; `finalizar(id)` devuelve `Despacho` y es idempotente; método extra `buscar(id)`. Ids con formato `D-001`. |

### co.rutavital.evento

| Clase | Precisión |
|---|---|
| `CentroEstadoVial` | `cambiarEstado(...)` devuelve `Optional<EventoVial>` (vacío si el tramo ya estaba en ese estado; en ese caso no notifica). Es `synchronized`. Métodos extra: `estadoDe(tramoId)` y `getObservadores()`. Si un observador lanza una excepción, se registra en el log y se notifica a los demás. |
| `EventoVial` | Método extra `esMejora()` (el nuevo estado es mejor que el anterior). |
| `RecalculadorDespachos` | Si el tramo está en la ruta de un despacho ACTIVO, siempre recalcula y registra (aunque el camino no cambie, el tiempo sí). Si el tramo mejora (se reabre o se levanta una restricción), recalcula todos los ACTIVOS y registra solo los que obtienen una ruta distinta. |
| Orden de notificación | Se fija en `ConfiguracionRutaVital`: `InvalidadorCache` → `RecalculadorDespachos` → `BitacoraEventos`. Si la caché está deshabilitada, `InvalidadorCache` no existe y el orden es `RecalculadorDespachos` → `BitacoraEventos`. |

### co.rutavital.comando

| Clase | Precisión |
|---|---|
| `ComandoCambiarEstadoTramo` | Constructor `(CentroEstadoVial, tramoId, nuevoEstado, motivo)`. Toma el estado anterior del `EventoVial` devuelto, así la lectura y el cambio son atómicos. `deshacer()` usa el motivo "Deshacer: <motivo>". |
| `ComandoCrearDespacho` | Constructor `(GestorDespachos, origenId, destinoId[, algoritmo])` y getter `getDespacho()`. |
| `InvocadorComandos` | `deshacerUltimo()` devuelve `Optional<Comando>`; `getHistorial()` devuelve `List<Comando>` con el tope de la pila primero. Si un comando lanza una excepción al ejecutarse, no entra al historial. |

### co.rutavital.api

| Endpoint | Precisión |
|---|---|
| `GET /api/despachos` | Parámetro opcional `soloActivos=true`. |
| `POST /api/despachos` | Responde 201. El cuerpo acepta un `algoritmo` opcional (DIJKSTRA por defecto). |
| `POST /api/tramos/{id}/estado` | Responde `{tramo, descripcion}`. Estado inválido o JSON mal formado → 400; tramo inexistente → 404. |
| `POST /api/comandos/deshacer` | Responde `{deshecho, descripcion, mensaje}`; si la pila está vacía, `deshecho=false` (200). |
| Identificadores | Se normalizan a mayúsculas (`san_gil` → `SAN_GIL`). |

---

## 3. Clases auxiliares agregadas

| Paquete | Clase | Para qué |
|---|---|---|
| `co.rutavital` | `RutaVitalAplicacion` | Clase principal de Spring Boot. |
| `co.rutavital.config` | `ConfiguracionRutaVital` | Beans `RedVial`, `CentroEstadoVial` (suscribe los observadores en orden) e `InvocadorComandos`. |
| `co.rutavital.modelo` | `EstadoDespacho` | Enum ACTIVO / FINALIZADO. |
| `co.rutavital.modelo` | `DistanciaGeografica` | Cálculo haversine (heurística de A* y generador sintético). |
| `co.rutavital.estrategia` | `ConstructorRuta` | Utilidad interna (visibilidad de paquete) que valida extremos y reconstruye la `Ruta`. |
| `co.rutavital.estrategia` | `AlgoritmoNoSoportadoException` | Nombre de algoritmo desconocido (400). |
| `co.rutavital.excepcion` | `RecursoNoEncontradoException`, `MunicipioNoEncontradoException`, `TramoNoEncontradoException`, `DespachoNoEncontradoException`, `SolicitudInvalidaException` | Errores de dominio que el manejador traduce a 404 o 400. |
| `co.rutavital.api` | `ManejadorErrores` | `@RestControllerAdvice` con cuerpo de error uniforme. |
| `co.rutavital.api` | `Parametros` | Normaliza y valida identificadores (visibilidad de paquete). |
| `co.rutavital.api.dto` | `TramoDTO`, `SolicitudCambioEstado`, `RespuestaCambioEstado`, `SolicitudDespacho`, `RespuestaDeshacer`, `EntradaHistorialDTO`, `ComparacionRutas`, `MetricasCacheDTO`, `ErrorRespuesta` | Records de entrada y salida de la API. |
| `co.rutavital.benchmark` | `GeneradorRedSintetica` | Genera las cuadrículas con semilla fija para el benchmark. |
