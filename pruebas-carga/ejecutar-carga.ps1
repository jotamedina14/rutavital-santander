# Prueba de carga de RutaVital Santander con y sin caché (Locust en modo headless).
#
# Para cada escenario arranca la aplicación con rutavital.cache.habilitada=true|false,
# ejecuta Locust (50 usuarios, 10 nuevos por segundo, 60 s) y guarda los CSV en
#   resultados\carga-con-cache\   y   resultados\carga-sin-cache\
#
# Requisitos: Java 17+ y Locust (pip install locust).
# Uso: powershell -ExecutionPolicy Bypass -File pruebas-carga\ejecutar-carga.ps1
#      (parámetros opcionales: -Puerto 8080 -Usuarios 50 -Tasa 10 -Duracion 60s)
param(
    [int]$Puerto = 8080,
    [int]$Usuarios = 50,
    [int]$Tasa = 10,
    [string]$Duracion = "60s"
)

$ErrorActionPreference = "Stop"
$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz
$Url = "http://localhost:$Puerto"
$EnWindows = ($env:OS -eq "Windows_NT")

function Responde([string]$Direccion) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $Direccion -TimeoutSec 2 | Out-Null
        return $true
    } catch {
        return $false
    }
}

if (-not (Get-Command locust -ErrorAction SilentlyContinue)) {
    Write-Error "No se encontró Locust. Instálalo con: pip install locust"
}
if (Responde "$Url/api/municipios") {
    Write-Error "Ya hay una aplicación respondiendo en el puerto $Puerto. Detenla o usa -Puerto."
}

Write-Host "Compilando la aplicación..."
if ($EnWindows) { & .\mvnw.cmd -q -DskipTests package } else { & ./mvnw -q -DskipTests package }
if ($LASTEXITCODE -ne 0) { throw "La compilación falló" }
$Jar = Get-ChildItem -Path "target" -Filter "rutavital-santander-*.jar" | Select-Object -First 1

function Ejecutar-Escenario([string]$Nombre, [string]$Cache) {
    $Carpeta = Join-Path "resultados" $Nombre
    New-Item -ItemType Directory -Force -Path $Carpeta | Out-Null
    Write-Host ""
    Write-Host "== Escenario $Nombre (rutavital.cache.habilitada=$Cache)"

    $Argumentos = "-jar `"$($Jar.FullName)`" --server.port=$Puerto --rutavital.cache.habilitada=$Cache"
    $App = Start-Process -FilePath "java" -ArgumentList $Argumentos -PassThru -NoNewWindow `
        -RedirectStandardOutput (Join-Path $Carpeta "app.log") `
        -RedirectStandardError (Join-Path $Carpeta "app-error.log")
    try {
        $Lista = $false
        for ($i = 0; $i -lt 60 -and -not $Lista; $i++) {
            Start-Sleep -Seconds 1
            $Lista = Responde "$Url/api/municipios"
        }
        if (-not $Lista) { throw "La aplicación no arrancó; revisa $Carpeta\app.log" }

        # Locust imprime el resumen final por stderr; se guarda en locust-consola.txt y se muestra.
        # Devuelve código 1 si hubo peticiones fallidas; se informa pero no se detiene el script.
        $Consola = Join-Path $Carpeta "locust-consola.txt"
        $ArgumentosLocust = "-f `"$(Join-Path 'pruebas-carga' 'locustfile.py')`" --headless " +
            "-u $Usuarios -r $Tasa -t $Duracion --host $Url " +
            "--csv `"$(Join-Path $Carpeta 'locust')`" --html `"$(Join-Path $Carpeta 'reporte.html')`" --only-summary"
        $Locust = Start-Process -FilePath "locust" -ArgumentList $ArgumentosLocust -NoNewWindow -Wait -PassThru `
            -RedirectStandardError $Consola
        Get-Content $Consola | Write-Host
        Write-Host "Locust terminó con código $($Locust.ExitCode)"

        Invoke-WebRequest -UseBasicParsing -Uri "$Url/api/metricas/cache" `
            -OutFile (Join-Path $Carpeta "metricas-cache.json")
    } finally {
        Stop-Process -Id $App.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

Ejecutar-Escenario "carga-con-cache" "true"
Ejecutar-Escenario "carga-sin-cache" "false"

Write-Host ""
Write-Host "Listo. Resultados en resultados\carga-con-cache y resultados\carga-sin-cache"
