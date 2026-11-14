package com.unsa.publicidad.prototipo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.*

/**
 * Lista de una tabla: la maestra va bajo el mapa; las referenciales, a pantalla completa.
 * Orden por defecto: código. Se puede cambiar a nombre y buscar por nombre o código
 * (en la maestra también por calle o cliente).
 */
@Composable
fun PantallaLista(
    db: BaseDatos, version: Int, tabla: Tabla,
    alAbrir: (String) -> Unit, alAdicionar: () -> Unit, alSalir: () -> Unit,
) {
    var busqueda by rememberSaveable(tabla) { mutableStateOf("") }
    var orden by rememberSaveable(tabla) { mutableStateOf(Orden.CODIGO) }
    val departamentos = remember(version) { db.listar(Tabla.DEPARTAMENTO) }
    val clientes = remember(version) { db.listar(Tabla.CLIENTE) }
    val zonas = remember(version) { db.listar(Tabla.ZONA) }
    val publicidades = remember(version, orden) { db.publicidades(orden) }
    fun nombre(lista: List<Registro>, c: String) = lista.firstOrNull { it.codigo == c }?.nombre ?: "—"
    val q = busqueda.trim()
    fun coincide(vararg campos: String) = q.isEmpty() || campos.any { it.contains(q, ignoreCase = true) }

    val esMaestra = tabla == Tabla.PUBLICIDAD
    val pubsFiltradas = publicidades.filter {
        coincide(it.codigo, it.nombre, it.calle, nombre(clientes, it.cliente))
    }
    val registros = remember(version, orden, tabla) { if (esMaestra) emptyList() else db.listar(tabla, orden) }
        .filter { coincide(it.codigo, it.nombre) }
    val total = if (esMaestra) publicidades.size else db.listar(tabla).size
    val mostrados = if (esMaestra) pubsFiltradas.size else registros.size
    val alturaMapa = if (esMaestra) 330.dp else 250.dp

    MapaConHoja(
        clave = tabla, topeInicial = alturaMapa - 30.dp, reservaSuperior = 72.dp, extraVisible = 84.dp,
        // el mapa solo tiene sentido en la maestra: las referenciales se listan a pantalla completa
        mapa = if (!esMaestra) null else { margenSuperior, margenInferior ->
            Mapa(
                Modifier.fillMaxSize(), departamentos, pubsFiltradas,
                margenSuperior = margenSuperior, todas = publicidades, margenInferior = margenInferior,
                alTocarPin = { p -> alAbrir(p.codigo) },
            )
        },
        sobreMapa = {
            BuscadorFlotante(
                busqueda, { busqueda = it },
                if (esMaestra) "Buscar publicidad, calle o cliente" else "Buscar ${tabla.singular} por nombre o código",
                alSalir,
            )
        },
        alFrente = {
            ExtendedFloatingActionButton(
                onClick = alAdicionar, containerColor = Tinta, contentColor = Color.White,
                icon = { Icon(if (esMaestra) Icons.Default.Place else Icons.Default.Add, null) },
                text = { Text("Adicionar", style = sans(16, 700, Color.White)) },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().imePadding().padding(20.dp),
            )
        },
        cabecera = {
            Row(Modifier.padding(top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (q.isEmpty()) "$total ${if (total == 1) tabla.singular else tabla.plural}"
                        else "$mostrados de $total ${tabla.plural}",
                        style = sans(20, 700),
                    )
                    if (!esMaestra) Text("Tabla referencial", style = sans(12, 400, Gris))
                }
                SelectorOrden(orden) { orden = it }
            }
            Leyenda()
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            if (mostrados == 0) item {
                Text(
                    if (q.isEmpty()) "Todavía no hay ${tabla.plural}. Use Adicionar."
                    else "Ningún registro coincide con «$q».",
                    style = sans(15, 400, Gris), modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            if (esMaestra) {
                // agrupadas por departamento, como lugares en un mapa
                pubsFiltradas.groupBy { it.departamento }.toSortedMap().forEach { (dep, lista) ->
                    item(key = "dep-$dep") {
                        Text(nombre(departamentos, dep), style = lugar(17, Gris), modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                    }
                    items(lista, key = { it.codigo }) { p ->
                        FilaLista(
                            p.codigo, p.estado, p.nombre,
                            "${p.codigo} · ${p.calle}, zona ${nombre(zonas, p.zona).lowercase()}",
                        ) { alAbrir(p.codigo) }
                    }
                }
            } else {
                items(registros, key = { it.codigo }) { r ->
                    val usos = publicidades.count { it.estado != Estado.ELIMINADO && BaseDatos.campo(it, tabla) == r.codigo }
                    FilaLista(
                        r.codigo, r.estado, r.nombre,
                        "${r.codigo} · ${Estado.texto(r.estado)} · " +
                            if (usos == 1) "1 publicidad" else "$usos publicidades",
                        serif = tabla != Tabla.CLIENTE,
                    ) { alAbrir(r.codigo) }
                }
            }
        }
    }
}
