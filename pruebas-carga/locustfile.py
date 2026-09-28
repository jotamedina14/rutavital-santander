"""
Prueba de carga de RutaVital Santander con Locust.

Cada usuario simula un operador de la central de despachos. Tareas y pesos:
  - calcular una ruta entre un par aleatorio de municipios ....... 5
  - comparar los tres algoritmos para un par aleatorio ........... 1
  - listar los despachos ......................................... 2

Entre tareas cada usuario espera entre 0,1 y 0,5 s.
Uso: ver ejecutar-carga.sh (Linux/macOS) o ejecutar-carga.ps1 (Windows).
"""
import random

import requests
from locust import HttpUser, between, events, task

MUNICIPIOS = [
    "BUCARAMANGA", "FLORIDABLANCA", "PIEDECUESTA", "GIRON", "LEBRIJA", "LOS_SANTOS",
    "PESCADERO", "PARQUE_CHICAMOCHA", "ARATOCA", "CEPITA", "CURITI", "VILLANUEVA",
    "BARICHARA", "ZAPATOCA", "SAN_GIL", "PINCHOTE", "SOCORRO", "VALLE_SAN_JOSE",
    "PARAMO", "MOGOTES", "CHARALA", "OIBA",
]
ALGORITMOS = ["DIJKSTRA", "A_ESTRELLA", "MENOS_TRAMOS"]
DESPACHOS_INICIALES = [
    ("SAN_GIL", "BUCARAMANGA"),
    ("SOCORRO", "BUCARAMANGA"),
    ("CHARALA", "SAN_GIL"),
]


def par_aleatorio():
    origen, destino = random.sample(MUNICIPIOS, 2)
    return origen, destino


@events.test_start.add_listener
def crear_despachos_iniciales(environment, **_kwargs):
    """Crea unos despachos al inicio para que el listado devuelva datos."""
    for origen, destino in DESPACHOS_INICIALES:
        requests.post(f"{environment.host}/api/despachos",
                      json={"origen": origen, "destino": destino}, timeout=10)


class OperadorDespacho(HttpUser):
    wait_time = between(0.1, 0.5)

    @task(5)
    def calcular_ruta(self):
        origen, destino = par_aleatorio()
        self.client.get("/api/rutas", name="GET /api/rutas", params={
            "origen": origen, "destino": destino, "algoritmo": random.choice(ALGORITMOS)})

    @task(1)
    def comparar_algoritmos(self):
        origen, destino = par_aleatorio()
        self.client.get("/api/rutas/comparar", name="GET /api/rutas/comparar",
                        params={"origen": origen, "destino": destino})

    @task(2)
    def listar_despachos(self):
        self.client.get("/api/despachos", name="GET /api/despachos")
