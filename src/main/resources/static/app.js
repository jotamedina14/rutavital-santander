'use strict';

/* RutaVital Santander: cliente web (JavaScript sin frameworks). */

const API = '/api';
const INTERVALO_REFRESCO_MS = 2000;

const COLOR_ESTADO = { ABIERTO: '#2f8f46', RESTRINGIDO: '#d9730d', CERRADO: '#c8322b' };
const NOMBRE_ESTADO = { ABIERTO: 'Abierto', RESTRINGIDO: 'Restringido', CERRADO: 'Cerrado' };
const NOMBRE_ALGORITMO = { DIJKSTRA: 'Dijkstra', A_ESTRELLA: 'A*', MENOS_TRAMOS: 'Menos tramos' };
const COLOR_RUTA_ACTUAL = '#1f5fa8';
const COLOR_RUTA_ANTERIOR = '#3d4750';
const COLOR_RESALTADO = '#f5c518';

const formatoNumero = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });
const formatoMs = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
const formatoPorcentaje = new Intl.NumberFormat('es-CO', { style: 'percent', maximumFractionDigits: 1 });
const formatoHora = new Intl.DateTimeFormat('es-CO', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

const estado = {
    municipios: new Map(),
    tramos: new Map(),
    lineasTramo: new Map(),
    firmaTramos: '',
    firmaDespachos: '',
    firmaEventos: '',
    firmaHistorial: null,
    recalculosVistos: new Map(),
    consultaMostrada: null,
    comparacionMostrada: null,
    despachos: [],
};

let mapa;
let capaRutaCalculada;
let capaDespachos;
let temporizadorAviso;

/* ---------- Utilidades ---------- */

function $(id) {
    return document.getElementById(id);
}

function elemento(etiqueta, clase, texto) {
    const el = document.createElement(etiqueta);
    if (clase) {
        el.className = clase;
    }
    if (texto !== undefined && texto !== null) {
        el.textContent = texto;
    }
    return el;
}

function nombreMunicipio(id) {
    return estado.municipios.get(id)?.nombre ?? id;
}

function minutos(valor) {
    return `${formatoNumero.format(valor)} min`;
}

function kilometros(valor) {
    return `${formatoNumero.format(valor)} km`;
}

function recalculadaTexto(veces) {
    return `Recalculada ${veces} ${veces === 1 ? 'vez' : 'veces'}`;
}

async function api(ruta, opciones = {}) {
    const respuesta = await fetch(API + ruta, {
        headers: { 'Content-Type': 'application/json' },
        ...opciones,
    });
    const cuerpo = await respuesta.json().catch(() => null);
    if (!respuesta.ok) {
        throw new Error(cuerpo?.mensaje ?? `La petición falló con estado ${respuesta.status}`);
    }
    return cuerpo;
}

function avisar(mensaje, esError = false) {
    const aviso = $('aviso');
    aviso.textContent = mensaje;
    aviso.classList.toggle('error', esError);
    aviso.hidden = false;
    clearTimeout(temporizadorAviso);
    temporizadorAviso = setTimeout(() => {
        aviso.hidden = true;
    }, 4500);
}

function puntosTramo(tramoId) {
    const tramo = estado.tramos.get(tramoId);
    const a = estado.municipios.get(tramo.origenId);
    const b = estado.municipios.get(tramo.destinoId);
    return [[a.latitud, a.longitud], [b.latitud, b.longitud]];
}

/** Coordenadas de una ruta completa, en el orden de sus municipios. */
function puntosRuta(ruta) {
    return ruta.municipios.map((id) => {
        const m = estado.municipios.get(id);
        return [m.latitud, m.longitud];
    });
}

/* ---------- Mapa ---------- */

function iniciarMapa() {
    mapa = L.map('mapa', { zoomSnap: 0.5 });
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; colaboradores de <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(mapa);

    // Orden de capas: resaltado de la ruta calculada, tramos, rutas de despachos y zona de clic
    mapa.createPane('resaltado').style.zIndex = 405;
    mapa.createPane('tramos').style.zIndex = 410;
    mapa.createPane('despachos').style.zIndex = 420;
    mapa.createPane('zonasClic').style.zIndex = 430;

    capaRutaCalculada = L.layerGroup().addTo(mapa);
    capaDespachos = L.layerGroup().addTo(mapa);
    agregarLeyenda();
}

function agregarLeyenda() {
    const leyenda = L.control({ position: 'bottomright' });
    leyenda.onAdd = () => {
        const div = L.DomUtil.create('div', 'leyenda');
        const linea = (color, ancho, guiones, opacidad = 1) =>
            `<svg width="30" height="10" aria-hidden="true"><line x1="1" y1="5" x2="29" y2="5" stroke="${color}"
              stroke-width="${ancho}" stroke-opacity="${opacidad}" ${guiones ? `stroke-dasharray="${guiones}"` : ''}
              stroke-linecap="${guiones ? 'butt' : 'round'}"/></svg>`;
        div.innerHTML = `
            <ul>
                <li>${linea(COLOR_ESTADO.ABIERTO, 5)} Tramo abierto</li>
                <li>${linea(COLOR_ESTADO.RESTRINGIDO, 5)} Restringido (paso a un carril)</li>
                <li>${linea(COLOR_ESTADO.CERRADO, 5, '6 5')} Cerrado</li>
                <li>${linea(COLOR_RESALTADO, 10, null, 0.7)} Ruta calculada</li>
                <li>${linea(COLOR_RUTA_ACTUAL, 3)} Despacho: ruta actual</li>
                <li>${linea(COLOR_RUTA_ANTERIOR, 4, '4 6')} Despacho: ruta anterior</li>
                <li><span class="icono-hospital" style="width:16px;height:16px;font-size:11px">H</span> Hospital (tamaño según nivel)</li>
            </ul>`;
        L.DomEvent.disableClickPropagation(div);
        return div;
    };
    leyenda.addTo(mapa);
}

function dibujarMunicipios(municipios) {
    for (const m of municipios) {
        let marcador;
        let desplazamiento;
        if (m.tieneHospital) {
            const lado = 14 + m.nivelHospital * 3;
            desplazamiento = lado / 2 + 3;
            marcador = L.marker([m.latitud, m.longitud], {
                icon: L.divIcon({
                    className: '',
                    html: `<span class="icono-hospital" style="font-size:${Math.round(lado * 0.62)}px">H</span>`,
                    iconSize: [lado, lado],
                    iconAnchor: [lado / 2, lado / 2],
                }),
                title: m.nombre,
                keyboard: true,
            });
        } else {
            desplazamiento = 7;
            marcador = L.circleMarker([m.latitud, m.longitud], {
                radius: 5,
                color: '#ffffff',
                weight: 2,
                fillColor: '#52606c',
                fillOpacity: 1,
            });
        }
        const detalle = m.tieneHospital ? `Hospital de nivel ${m.nivelHospital}` : 'Sin hospital';
        marcador.bindPopup(`<strong>${m.nombre}</strong><br>${detalle}`);
        marcador.bindTooltip(m.nombre, {
            permanent: true,
            direction: 'right',
            offset: [desplazamiento, 0],
            className: 'etiqueta-municipio',
        });
        marcador.addTo(mapa);
    }
    const limites = L.latLngBounds(municipios.map((m) => [m.latitud, m.longitud]));
    mapa.fitBounds(limites, { padding: [30, 30] });
}

function estiloTramo(estadoTramo) {
    const cerrado = estadoTramo === 'CERRADO';
    return {
        color: COLOR_ESTADO[estadoTramo],
        opacity: 0.95,
        dashArray: cerrado ? '8 8' : null,
        lineCap: cerrado ? 'butt' : 'round',
    };
}

function dibujarTramos(tramos) {
    for (const t of tramos) {
        const puntos = puntosTramo(t.id);
        const linea = L.polyline(puntos, {
            pane: 'tramos',
            weight: 6,
            interactive: false,
            ...estiloTramo(t.estado),
        }).addTo(mapa);
        // Línea invisible y ancha para que sea fácil hacer clic sobre el tramo
        const zona = L.polyline(puntos, { pane: 'zonasClic', weight: 20, opacity: 0, color: '#000' }).addTo(mapa);
        zona.bindPopup(() => contenidoPopupTramo(t.id), { minWidth: 250 });
        zona.bindTooltip(() => {
            const actual = estado.tramos.get(t.id);
            return `${actual.id}: ${nombreMunicipio(actual.origenId)} – ${nombreMunicipio(actual.destinoId)} (${NOMBRE_ESTADO[actual.estado].toLowerCase()})`;
        }, { sticky: true });
        estado.lineasTramo.set(t.id, linea);
    }
}

function contenidoPopupTramo(tramoId) {
    const t = estado.tramos.get(tramoId);
    const contenedor = elemento('div', 'popup-tramo');
    contenedor.append(elemento('h3', null, `${t.id}: ${nombreMunicipio(t.origenId)} – ${nombreMunicipio(t.destinoId)}`));
    contenedor.append(elemento('p', null,
        `${kilometros(t.distanciaKm)} por vía ${t.tipoVia.toLowerCase()} (${t.velocidadKmH} km/h)`));

    const lineaEstado = elemento('p');
    const punto = elemento('span', `punto-estado ${t.estado}`);
    lineaEstado.append(punto, `${NOMBRE_ESTADO[t.estado]}. `,
        t.tiempoEstimadoMin === null ? 'No transitable.' : `Tiempo estimado: ${minutos(t.tiempoEstimadoMin)}.`);
    contenedor.append(lineaEstado);

    const etiqueta = elemento('label', null, 'Motivo');
    const motivo = elemento('input');
    motivo.type = 'text';
    motivo.placeholder = 'Ej.: Derrumbe sector Pescadero';
    motivo.maxLength = 120;
    etiqueta.append(motivo);
    contenedor.append(etiqueta);

    const acciones = elemento('div', 'acciones');
    const opciones = [
        ['CERRADO', 'Cerrar', 'boton-peligro'],
        ['RESTRINGIDO', 'Restringir', 'boton-secundario'],
        ['ABIERTO', 'Reabrir', 'boton-secundario'],
    ];
    for (const [nuevoEstado, texto, clase] of opciones) {
        const boton = elemento('button', `boton boton-pequeno ${clase}`, texto);
        boton.type = 'button';
        boton.disabled = t.estado === nuevoEstado;
        boton.addEventListener('click', async () => {
            boton.disabled = true;
            await cambiarEstadoTramo(t.id, nuevoEstado, motivo.value);
            mapa.closePopup();
        });
        acciones.append(boton);
    }
    contenedor.append(acciones);
    return contenedor;
}

function dibujarRutaCalculada(ruta, ajustarVista) {
    capaRutaCalculada.clearLayers();
    if (!ruta.encontrada || ruta.municipios.length < 2) {
        return;
    }
    const linea = L.polyline(puntosRuta(ruta), {
        pane: 'resaltado',
        color: COLOR_RESALTADO,
        weight: 16,
        opacity: 0.7,
        lineCap: 'round',
        lineJoin: 'round',
        interactive: false,
    }).addTo(capaRutaCalculada);
    if (ajustarVista) {
        mapa.fitBounds(linea.getBounds(), { padding: [40, 40] });
    }
}

function dibujarRutasDespachos(despachos) {
    capaDespachos.clearLayers();
    for (const d of despachos) {
        const anterior = d.historialRutas.at(-1);
        if (anterior && anterior.encontrada) {
            L.polyline(puntosRuta(anterior), {
                pane: 'despachos',
                color: COLOR_RUTA_ANTERIOR,
                weight: 4,
                dashArray: '4 9',
                interactive: false,
            }).addTo(capaDespachos);
        }
        if (d.rutaActual.encontrada) {
            L.polyline(puntosRuta(d.rutaActual), {
                pane: 'despachos',
                color: COLOR_RUTA_ACTUAL,
                weight: 3,
                lineJoin: 'round',
                interactive: false,
            }).addTo(capaDespachos);
        }
    }
}

/* ---------- Rutas ---------- */

function consultaActual() {
    return {
        origen: $('sel-origen').value,
        destino: $('sel-destino').value,
        algoritmo: $('sel-algoritmo').value,
    };
}

async function calcularRuta(consulta, ajustarVista = true) {
    const parametros = new URLSearchParams(consulta);
    const ruta = await api(`/rutas?${parametros}`);
    estado.consultaMostrada = consulta;
    mostrarRuta(ruta, consulta);
    dibujarRutaCalculada(ruta, ajustarVista);
}

function mostrarRuta(ruta, consulta) {
    const contenedor = $('resultado-ruta');
    contenedor.replaceChildren();
    contenedor.hidden = false;

    if (!ruta.encontrada) {
        const bloque = elemento('div', 'sin-ruta');
        bloque.append(elemento('strong', null,
            `Sin ruta entre ${nombreMunicipio(consulta.origen)} y ${nombreMunicipio(consulta.destino)}`));
        bloque.append(elemento('span', null, `${ruta.mensaje} Reabre un tramo desde el mapa para habilitar un camino.`));
        contenedor.append(bloque);
        return;
    }

    const tiempo = elemento('div', 'ruta-tiempo');
    tiempo.append(elemento('span', 'cifra', formatoNumero.format(ruta.tiempoTotalMin)), elemento('span', 'unidad', 'minutos'));
    contenedor.append(tiempo);
    contenedor.append(elemento('p', 'ruta-algoritmo',
        `${nombreMunicipio(consulta.origen)} a ${nombreMunicipio(consulta.destino)} con ${NOMBRE_ALGORITMO[ruta.algoritmo] ?? ruta.algoritmo}`));

    const metricas = elemento('dl', 'metricas');
    const desdeCache = elemento('span', ruta.metricas.desdeCache ? 'etiqueta-cache' : null,
        ruta.metricas.desdeCache ? 'Sí' : 'No');
    const datos = [
        ['Distancia', kilometros(ruta.distanciaTotalKm)],
        ['Tramos', String(ruta.tramos.length)],
        ['Nodos explorados', String(ruta.metricas.nodosExplorados)],
        ['Tiempo de cálculo', `${formatoMs.format(ruta.metricas.tiempoCalculoMs)} ms`],
        ['Desde caché', desdeCache],
    ];
    for (const [titulo, valor] of datos) {
        const grupo = elemento('div');
        const dd = elemento('dd');
        dd.append(valor);
        grupo.append(elemento('dt', null, titulo), dd);
        metricas.append(grupo);
    }
    contenedor.append(metricas);
    contenedor.append(crearItinerario(ruta));
}

/** Itinerario como diagrama de línea: paradas unidas por los tramos recorridos. */
function crearItinerario(ruta) {
    const lista = elemento('ol', 'itinerario');
    lista.setAttribute('aria-label', 'Itinerario de la ruta');
    ruta.municipios.forEach((municipioId, i) => {
        const m = estado.municipios.get(municipioId);
        const parada = elemento('li', 'parada', m.nombre);
        if (m.tieneHospital) {
            parada.append(elemento('span', 'hospital', `Hospital nivel ${m.nivelHospital}`));
        }
        lista.append(parada);
        if (i < ruta.tramos.length) {
            const t = estado.tramos.get(ruta.tramos[i]);
            const tramo = elemento('li', 'tramo',
                `${t.id}, ${kilometros(t.distanciaKm)}, vía ${t.tipoVia.toLowerCase()}, ${minutos(t.tiempoEstimadoMin ?? 0)}`);
            if (t.estado === 'RESTRINGIDO') {
                tramo.append(' ', elemento('span', 'estado-restringido', 'restringido'));
            }
            lista.append(tramo);
        }
    });
    return lista;
}

/**
 * @param mostrarTabla true cuando lo pide el usuario: desplaza el panel hasta la tabla.
 *                     En los refrescos automáticos la vista no se mueve.
 */
async function compararAlgoritmos(origen, destino, mostrarTabla = false) {
    const comparacion = await api(`/rutas/comparar?${new URLSearchParams({ origen, destino })}`);
    estado.comparacionMostrada = { origen, destino };
    const contenedor = $('resultado-comparacion');
    contenedor.replaceChildren();
    contenedor.hidden = false;
    contenedor.append(elemento('h3', null,
        `Comparación ${nombreMunicipio(origen)} a ${nombreMunicipio(destino)}`));

    const tabla = elemento('table', 'tabla-comparacion');
    tabla.innerHTML = `<thead><tr><th>Algoritmo</th><th>Min</th><th>Km</th><th>Tramos</th>
        <th>Nodos</th><th>Cálculo (ms)</th></tr></thead>`;
    const cuerpo = elemento('tbody');
    const encontradas = comparacion.rutas.filter((r) => r.encontrada);
    const mejorTiempo = Math.min(...encontradas.map((r) => r.tiempoTotalMin));
    for (const ruta of comparacion.rutas) {
        const fila = elemento('tr');
        if (ruta.encontrada && Math.abs(ruta.tiempoTotalMin - mejorTiempo) < 1e-9) {
            fila.classList.add('mejor');
        }
        const celdas = ruta.encontrada
            ? [NOMBRE_ALGORITMO[ruta.algoritmo], formatoNumero.format(ruta.tiempoTotalMin),
                formatoNumero.format(ruta.distanciaTotalKm), ruta.tramos.length, ruta.metricas.nodosExplorados,
                formatoMs.format(ruta.metricas.tiempoCalculoMs)]
            : [NOMBRE_ALGORITMO[ruta.algoritmo], 'Sin ruta', '–', '–', ruta.metricas.nodosExplorados,
                formatoMs.format(ruta.metricas.tiempoCalculoMs)];
        for (const valor of celdas) {
            fila.append(elemento('td', null, String(valor)));
        }
        fila.tabIndex = 0;
        fila.title = 'Mostrar esta ruta en el mapa';
        const mostrar = () => {
            mostrarRuta(ruta, { origen, destino, algoritmo: ruta.algoritmo });
            dibujarRutaCalculada(ruta, true);
        };
        fila.addEventListener('click', mostrar);
        fila.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                mostrar();
            }
        });
        cuerpo.append(fila);
    }
    tabla.append(cuerpo);
    contenedor.append(tabla);
    contenedor.append(elemento('p', 'nota',
        'Las tres estrategias se ejecutan sin caché. Haz clic en una fila para verla en el mapa.'));
    if (mostrarTabla) {
        const suave = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        contenedor.scrollIntoView({ behavior: suave ? 'smooth' : 'auto', block: 'nearest' });
    }
}

/* ---------- Acciones (Command) ---------- */

async function cambiarEstadoTramo(tramoId, nuevoEstado, motivo) {
    try {
        const respuesta = await api(`/tramos/${encodeURIComponent(tramoId)}/estado`, {
            method: 'POST',
            body: JSON.stringify({ estado: nuevoEstado, motivo }),
        });
        avisar(respuesta.descripcion);
        await refrescarTodo();
    } catch (error) {
        avisar(error.message, true);
    }
}

async function deshacerUltimaAccion() {
    try {
        const respuesta = await api('/comandos/deshacer', { method: 'POST' });
        avisar(respuesta.mensaje, !respuesta.deshecho);
        await refrescarTodo();
    } catch (error) {
        avisar(error.message, true);
    }
}

async function crearDespacho(origen, destino) {
    const despacho = await api('/despachos', {
        method: 'POST',
        body: JSON.stringify({ origen, destino }),
    });
    avisar(`Despacho ${despacho.id} creado: ${nombreMunicipio(origen)} a ${nombreMunicipio(destino)}`);
    await refrescarTodo();
}

async function finalizarDespacho(id) {
    try {
        await api(`/despachos/${encodeURIComponent(id)}/finalizar`, { method: 'POST' });
        avisar(`Despacho ${id} finalizado`);
        await refrescarTodo();
    } catch (error) {
        avisar(error.message, true);
    }
}

/* ---------- Refresco periódico ---------- */

async function refrescarTramos() {
    const tramos = await api('/tramos');
    const firma = tramos.map((t) => `${t.id}:${t.estado}`).join('|');
    if (firma === estado.firmaTramos) {
        return;
    }
    const huboCambio = estado.firmaTramos !== '';
    estado.firmaTramos = firma;
    for (const t of tramos) {
        estado.tramos.set(t.id, t);
        estado.lineasTramo.get(t.id)?.setStyle(estiloTramo(t.estado));
    }
    // La red cambió: la ruta y la comparación que se están mostrando se vuelven a calcular
    if (huboCambio && estado.consultaMostrada) {
        await calcularRuta(estado.consultaMostrada, false);
    }
    if (huboCambio && estado.comparacionMostrada) {
        await compararAlgoritmos(estado.comparacionMostrada.origen, estado.comparacionMostrada.destino);
    }
}

async function refrescarDespachos() {
    const despachos = await api('/despachos?soloActivos=true');
    const firma = JSON.stringify(despachos.map((d) => [d.id, d.vecesRecalculada, d.rutaActual.tramos]));
    if (firma === estado.firmaDespachos) {
        return;
    }
    estado.firmaDespachos = firma;
    estado.despachos = despachos;
    mostrarDespachos(despachos);
    dibujarRutasDespachos(despachos);
}

function describirRuta(ruta) {
    if (!ruta.encontrada) {
        return 'sin ruta posible';
    }
    const intermedios = ruta.municipios.slice(1, -1).map(nombreMunicipio);
    const via = intermedios.length ? ` por ${intermedios.join(', ')}` : ' directa';
    return `${minutos(ruta.tiempoTotalMin)}, ${kilometros(ruta.distanciaTotalKm)}${via}`;
}

function mostrarDespachos(despachos) {
    const lista = $('lista-despachos');
    lista.replaceChildren();
    if (despachos.length === 0) {
        lista.append(elemento('li', 'vacio', 'No hay despachos activos. Crea uno para seguir su ruta en el mapa.'));
        return;
    }
    for (const d of despachos) {
        const tarjeta = elemento('li', 'despacho');
        const vistos = estado.recalculosVistos.get(d.id);
        if (vistos !== undefined && d.vecesRecalculada > vistos) {
            tarjeta.classList.add('recien-recalculado');
        }
        estado.recalculosVistos.set(d.id, d.vecesRecalculada);

        const cabecera = elemento('div', 'despacho-cabecera');
        const titulo = elemento('span', 'despacho-titulo');
        titulo.append(elemento('span', 'despacho-id', d.id),
            `${nombreMunicipio(d.origenId)} → ${nombreMunicipio(d.destinoId)}`);
        const insignia = elemento('span', `insignia${d.vecesRecalculada > 0 ? ' activa' : ''}`,
            recalculadaTexto(d.vecesRecalculada));
        cabecera.append(titulo, insignia);
        tarjeta.append(cabecera);

        const actual = elemento('p', 'despacho-ruta');
        actual.append('Ruta actual: ', elemento('span', 'tiempo', describirRuta(d.rutaActual)));
        tarjeta.append(actual);

        const anterior = d.historialRutas.at(-1);
        if (anterior) {
            tarjeta.append(elemento('p', 'despacho-anterior', `Ruta anterior: ${describirRuta(anterior)}`));
        }

        const acciones = elemento('div', 'despacho-acciones');
        const ver = elemento('button', 'boton boton-secundario boton-pequeno', 'Ver en el mapa');
        ver.type = 'button';
        ver.disabled = !d.rutaActual.encontrada;
        ver.addEventListener('click', () => mapa.fitBounds(L.latLngBounds(puntosRuta(d.rutaActual)), { padding: [40, 40] }));
        const finalizar = elemento('button', 'boton boton-peligro boton-pequeno', 'Finalizar');
        finalizar.type = 'button';
        finalizar.addEventListener('click', () => finalizarDespacho(d.id));
        acciones.append(ver, finalizar);
        tarjeta.append(acciones);
        lista.append(tarjeta);
    }
}

async function refrescarEventos() {
    const eventos = await api('/eventos');
    const firma = eventos.length ? `${eventos.length}:${eventos[0].fechaHora}` : '0';
    if (firma === estado.firmaEventos) {
        return;
    }
    estado.firmaEventos = firma;
    const lista = $('lista-eventos');
    lista.replaceChildren();
    if (eventos.length === 0) {
        lista.append(elemento('li', 'vacio', 'Sin eventos. Haz clic en un tramo del mapa para cerrarlo o restringirlo.'));
        return;
    }
    for (const e of eventos) {
        const item = elemento('li');
        const cabecera = elemento('div', 'evento-cabecera');
        cabecera.append(
            elemento('span', 'evento-hora', formatoHora.format(new Date(e.fechaHora))),
            elemento('strong', null, e.tramoId));
        const cambio = elemento('span');
        cambio.append(`${NOMBRE_ESTADO[e.estadoAnterior]} a `, elemento('span', `punto-estado ${e.estadoNuevo}`),
            NOMBRE_ESTADO[e.estadoNuevo]);
        cabecera.append(cambio);
        item.append(cabecera, elemento('div', 'evento-motivo', e.motivo));
        lista.append(item);
    }
}

async function refrescarHistorial() {
    const historial = await api('/comandos/historial');
    const firma = historial.length ? historial.map((h) => h.descripcion).join('|') : 'vacio';
    if (firma === estado.firmaHistorial) {
        return;
    }
    estado.firmaHistorial = firma;
    const boton = $('btn-deshacer');
    boton.disabled = historial.length === 0;
    boton.title = historial.length ? `Deshacer: ${historial[0].descripcion}` : 'No hay acciones para deshacer';

    const lista = $('lista-historial');
    lista.replaceChildren();
    if (historial.length === 0) {
        lista.append(elemento('li', 'vacio', 'Todavía no hay acciones. Los cierres y despachos que hagas aparecerán aquí.'));
        return;
    }
    for (const entrada of historial) {
        lista.append(elemento('li', null, entrada.descripcion));
    }
}

async function refrescarCache() {
    const metricas = await api('/metricas/cache');
    const indicador = $('indicador-cache');
    indicador.replaceChildren();
    indicador.classList.toggle('desactivada', !metricas.habilitada);
    if (!metricas.habilitada) {
        indicador.append(elemento('span', 'cache-titulo', 'Caché desactivada'));
        return;
    }
    const dato = (titulo, valor) => {
        const span = elemento('span', null, `${titulo} `);
        span.append(elemento('strong', null, valor));
        return span;
    };
    indicador.append(
        elemento('span', 'cache-titulo', 'Caché activa'),
        dato('Aciertos', String(metricas.hits)),
        dato('Fallos', String(metricas.misses)),
        dato('Tasa', formatoPorcentaje.format(metricas.tasaAcierto)));
}

async function refrescarTodo() {
    // Los tramos primero: el resto de vistas usa su estado actualizado
    await refrescarTramos();
    await Promise.all([refrescarDespachos(), refrescarEventos(), refrescarHistorial(), refrescarCache()]);
}

function programarRefresco() {
    setTimeout(async () => {
        try {
            await refrescarTodo();
        } catch (error) {
            avisar(`No se pudo actualizar: ${error.message}`, true);
        }
        programarRefresco();
    }, INTERVALO_REFRESCO_MS);
}

/* ---------- Inicio ---------- */

function llenarSelect(select, municipios, seleccionado) {
    for (const m of municipios) {
        const opcion = elemento('option', null, m.nombre);
        opcion.value = m.id;
        opcion.selected = m.id === seleccionado;
        select.append(opcion);
    }
}

function conectarEventos() {
    $('form-ruta').addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await calcularRuta(consultaActual());
        } catch (error) {
            avisar(error.message, true);
        }
    });
    $('btn-comparar').addEventListener('click', async () => {
        try {
            const { origen, destino } = consultaActual();
            await compararAlgoritmos(origen, destino, true);
        } catch (error) {
            avisar(error.message, true);
        }
    });
    $('form-despacho').addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await crearDespacho($('sel-despacho-origen').value, $('sel-despacho-destino').value);
        } catch (error) {
            avisar(error.message, true);
        }
    });
    $('btn-deshacer').addEventListener('click', deshacerUltimaAccion);
}

async function iniciar() {
    iniciarMapa();
    conectarEventos();
    try {
        const [municipios, tramos] = await Promise.all([api('/municipios'), api('/tramos')]);
        municipios.forEach((m) => estado.municipios.set(m.id, m));
        tramos.forEach((t) => estado.tramos.set(t.id, t));

        const ordenados = [...municipios].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        llenarSelect($('sel-origen'), ordenados, 'SAN_GIL');
        llenarSelect($('sel-destino'), ordenados, 'BUCARAMANGA');
        llenarSelect($('sel-despacho-origen'), ordenados, 'SAN_GIL');
        llenarSelect($('sel-despacho-destino'), ordenados, 'BUCARAMANGA');

        dibujarMunicipios(municipios);
        dibujarTramos(tramos);
        await refrescarTodo();
    } catch (error) {
        avisar(`No se pudo cargar la red vial: ${error.message}`, true);
    }
    programarRefresco();
}

document.addEventListener('DOMContentLoaded', iniciar);
