#!/usr/bin/env bash
# Prueba de carga de RutaVital Santander con y sin caché (Locust en modo headless).
#
# Para cada escenario arranca la aplicación con rutavital.cache.habilitada=true|false,
# ejecuta Locust (50 usuarios, 10 nuevos por segundo, 60 s) y guarda los CSV en
#   resultados/carga-con-cache/   y   resultados/carga-sin-cache/
#
# Requisitos: Java 17+, curl y Locust (pip install locust).
# Parámetros opcionales por variable de entorno: PUERTO, USUARIOS, TASA, DURACION, LOCUST.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

PUERTO="${PUERTO:-8080}"
USUARIOS="${USUARIOS:-50}"
TASA="${TASA:-10}"
DURACION="${DURACION:-60s}"
LOCUST="${LOCUST:-locust}"
URL="http://localhost:${PUERTO}"

if ! command -v "$LOCUST" >/dev/null 2>&1; then
    echo "No se encontró Locust. Instálalo con: pip install locust" >&2
    exit 1
fi
if curl -s -o /dev/null "$URL"; then
    echo "Ya hay algo escuchando en el puerto $PUERTO. Detén esa aplicación o usa PUERTO=otro." >&2
    exit 1
fi

echo "Compilando la aplicación..."
if [ -x ./mvnw ]; then MVN=./mvnw; else MVN=mvn; fi
"$MVN" -q -DskipTests package
JAR="$(ls target/rutavital-santander-*.jar | grep -v '\.original$' | head -n 1)"

PID_APP=""
detener_app() {
    if [ -n "$PID_APP" ] && kill -0 "$PID_APP" 2>/dev/null; then
        kill "$PID_APP"
        wait "$PID_APP" 2>/dev/null || true
    fi
    PID_APP=""
}
trap detener_app EXIT

ejecutar_escenario() {
    local nombre="$1" cache="$2"
    local carpeta="resultados/$nombre"
    mkdir -p "$carpeta"
    echo
    echo "== Escenario $nombre (rutavital.cache.habilitada=$cache)"

    java -jar "$JAR" --server.port="$PUERTO" --rutavital.cache.habilitada="$cache" > "$carpeta/app.log" 2>&1 &
    PID_APP=$!
    for _ in $(seq 1 60); do
        curl -sf -o /dev/null "$URL/api/municipios" && break
        sleep 1
    done
    if ! curl -sf -o /dev/null "$URL/api/municipios"; then
        echo "La aplicación no arrancó; revisa $carpeta/app.log" >&2
        exit 1
    fi

    # Locust imprime el resumen final por stderr; se guarda también en locust-consola.txt.
    # Devuelve código 1 si hubo peticiones fallidas; se registra pero no detiene el script.
    set +e
    "$LOCUST" -f pruebas-carga/locustfile.py --headless \
        -u "$USUARIOS" -r "$TASA" -t "$DURACION" --host "$URL" \
        --csv "$carpeta/locust" --html "$carpeta/reporte.html" --only-summary 2>&1 | tee "$carpeta/locust-consola.txt"
    echo "Locust terminó con código ${PIPESTATUS[0]}"
    set -e

    curl -sf "$URL/api/metricas/cache" > "$carpeta/metricas-cache.json" || true
    detener_app
}

ejecutar_escenario carga-con-cache true
ejecutar_escenario carga-sin-cache false

echo
echo "Listo. Resultados en resultados/carga-con-cache y resultados/carga-sin-cache"
