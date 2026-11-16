package com.unsa.publicidad.prototipo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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

private enum class Confirmar { ELIMINAR, INACTIVAR }
private class PorConfirmar(val tipo: Confirmar, val codigo: String, val nombre: String, val usos: Int)
private class Consulta(val tabla: Tabla, val actual: String, val cargar: (String) -> Unit)

/**
 * Mantenimiento de una tabla, todo en una sola pantalla:
 *  - arriba: buscador (← Salir), orden (Clientes abre por Nombre; el resto por Código) y leyenda A / I / *;
 *  - al centro: tarjetas con sus botones dentro (Modificar, Inactivar / Reactivar, Eliminar);
 *    en el Maestro de Publicidad se puede cambiar de vista a Mapa;
 *  - abajo: Adicionar y Modificar abren el formulario en la mitad inferior, sin cambiar de pantalla.
 */
@Composable
fun PantallaLista(
    db: BaseDatos, version: Int, tabla: Tabla,
    alCambiarDatos: () -> Unit, avisar: (String) -> Unit, alSalir: () -> Unit,
) {
    val esMaestra = tabla == Tabla.PUBLICIDAD
    var busqueda by rememberSaveable(tabla) { mutableStateOf("") }
    var orden by rememberSaveable(tabla) { mutableStateOf(tabla.ordenDefecto) }
    var enMapa by rememberSaveable(tabla) { mutableStateOf(false) }
    // formulario inferior: null = cerrado, "" = Adición, otro = código que se modifica
    var edicion by rememberSaveable(tabla) { mutableStateOf<String?>(null) }
    var vecesAbierto by rememberSaveable(tabla) { mutableIntStateOf(0) }
    var pinElegido by rememberSaveable(tabla) { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf<PorConfirmar?>(null) }
    var consulta by remember { mutableStateOf<Consulta?>(null) }

    val departamentos = remember(version) { db.listar(Tabla.DEPARTAMENTO) }
    val clientes = remember(version) { db.listar(Tabla.CLIENTE) }
    val zonas = remember(version) { db.listar(Tabla.ZONA) }
    val publicidades = remember(version, orden) { db.publicidades(orden) }
    val registros = remember(version, orden, tabla) { if (esMaestra) emptyList() else db.listar(tabla, orden) }
    fun nombre(lista: List<Registro>, c: String) = lista.firstOrNull { it.codigo == c }?.nombre ?: "—"

    val q = busqueda.trim()
    fun coincide(vararg campos: String) = q.isEmpty() || campos.any { it.contains(q, ignoreCase = true) }
    val pubsFiltradas = publicidades.filter { coincide(it.codigo, it.nombre, it.calle, nombre(clientes, it.cliente)) }
    val regsFiltrados = registros.filter { coincide(it.nombre, it.codigo) }
    val total = if (esMaestra) publicidades.size else registros.size
    val mostrados = if (esMaestra) pubsFiltradas.size else regsFiltrados.size
    val codigos = if (esMaestra) pubsFiltradas.map { it.codigo } else regsFiltrados.map { it.codigo }

    // ---- acciones ----
    fun abrirFormulario(codigo: String?) { edicion = codigo ?: ""; vecesAbierto++; pinElegido = null }
    fun cerrarFormulario() { edicion = null }

    fun usosDe(codigo: String) = if (esMaestra) 0 else publicidades.count {
        it.estado != Estado.ELIMINADO && BaseDatos.campo(it, tabla) == codigo
    }

    fun cambiarEstado(codigo: String, nuevo: Char) {
        db.cambiarEstado(tabla, codigo, nuevo)
        alCambiarDatos()
        if (nuevo == Estado.ELIMINADO && edicion == codigo) cerrarFormulario()
        avisar(
            when (nuevo) {
                Estado.ACTIVO -> "$codigo reactivado (A)"
                Estado.INACTIVO -> "$codigo inactivado (I)"
                else -> "$codigo eliminado (*)"
            }
        )
    }

    fun pedirEliminar(codigo: String, nombre: String) {
        val usos = db.publicidadesQueUsan(tabla, codigo)
        if (usos.isNotEmpty()) avisar("No se puede eliminar: lo usa ${usos.joinToString { it.codigo }}")
        else confirmar = PorConfirmar(Confirmar.ELIMINAR, codigo, nombre, 0)
    }

    BackHandler(enabled = consulta != null || edicion != null || pinElegido != null) {
        when {
            consulta != null -> consulta = null
            edicion != null -> cerrarFormulario()
            else -> pinElegido = null
        }
    }

    // al modificar, la tarjeta del registro se lleva a la vista (encima del formulario);
    // al guardar, se muestra el registro guardado (uno nuevo puede quedar arriba, fuera de vista)
    val estadoLista = rememberLazyListState()
    var guardado by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(vecesAbierto) {
        val i = codigos.indexOf(edicion)
        if (i >= 0) estadoLista.animateScrollToItem(i)
    }
    LaunchedEffect(guardado, codigos) {
        val i = codigos.indexOf(guardado ?: return@LaunchedEffect)
        if (i >= 0) { estadoLista.animateScrollToItem(i); guardado = null }
    }

    @Composable
    fun Tarjeta(p: Publicidad, modifier: Modifier = Modifier) = TarjetaRegistro(
        p.codigo, p.estado, p.nombre,
        listOf(
            "Cliente: ${nombre(clientes, p.cliente)}",
            "${nombre(departamentos, p.departamento)} · zona ${nombre(zonas, p.zona).lowercase()}",
            p.calle,
        ),
        resaltada = edicion == p.codigo, modifier = modifier,
        alModificar = { abrirFormulario(p.codigo) },
        alInactivar = { confirmar = PorConfirmar(Confirmar.INACTIVAR, p.codigo, p.nombre, 0) },
        alReactivar = { cambiarEstado(p.codigo, Estado.ACTIVO) },
        alEliminar = { pedirEliminar(p.codigo, p.nombre) },
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        val altoPanel = maxHeight * .5f   // el formulario ocupa la mitad de abajo
        Column(Modifier.fillMaxSize().imePadding()) {
            BuscadorFlotante(
                busqueda, { busqueda = it },
                when (tabla) {
                    Tabla.PUBLICIDAD -> "Buscar publicidad, calle o cliente"
                    Tabla.CLIENTE -> "Buscar cliente por nombre"
                    else -> "Buscar ${tabla.singular} por nombre o código"
                },
                alSalir,
            )

            // ---- cabecera ----
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(if (esMaestra) "Maestro de Publicidad" else tabla.pluralTitulo, style = sans(22, 700))
                Text(
                    (if (q.isEmpty()) "$total ${if (total == 1) tabla.singular else tabla.plural}"
                    else "$mostrados de $total ${tabla.plural}") +
                        if (esMaestra) " · tabla maestra" else " · tabla referencial",
                    style = sans(13, 400, Gris),
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!enMapa) SelectorOrden(orden) { orden = it }
                    Spacer(Modifier.weight(1f))
                    if (esMaestra) SelectorVista(enMapa) { enMapa = it; pinElegido = null }
                }
                if (edicion == null) Box(Modifier.padding(top = 8.dp)) { Leyenda() }
                Spacer(Modifier.height(6.dp))
            }

            // ---- contenido: tarjetas o mapa ----
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (esMaestra && enMapa) {
                    val elegida = pubsFiltradas.firstOrNull { it.codigo == pinElegido }
                    Mapa(
                        Modifier.fillMaxSize(), departamentos, pubsFiltradas,
                        margenSuperior = 0f, todas = publicidades,
                        margenInferior = when {
                            elegida != null -> 200.dp
                            edicion == null -> WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            else -> 0.dp
                        },
                        alTocarPin = { p -> if (edicion == null) pinElegido = p.codigo },
                    )
                    if (elegida != null && edicion == null) {
                        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Tarjeta(elegida, Modifier.shadow(8.dp, RoundedCornerShape(16.dp)))
                            IconButton(
                                { pinElegido = null },
                                Modifier.align(Alignment.TopEnd).offset(8.dp, (-8).dp).size(30.dp)
                                    .shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White),
                            ) { Icon(Icons.Default.Close, "Cerrar", tint = Tinta, modifier = Modifier.size(16.dp)) }
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(), state = estadoLista,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (edicion == null) 96.dp else 12.dp),
                    ) {
                        if (mostrados == 0) item {
                            Text(
                                if (q.isEmpty()) "Todavía no hay ${tabla.plural}. Use Adicionar."
                                else "Ningún registro coincide con «$q».",
                                style = sans(15, 400, Gris), modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
                            )
                        }
                        if (esMaestra) {
                            items(pubsFiltradas, key = { it.codigo }) { p -> Tarjeta(p) }
                        } else {
                            items(regsFiltrados, key = { it.codigo }) { r ->
                                val usos = usosDe(r.codigo)
                                TarjetaRegistro(
                                    r.codigo, r.estado, r.nombre,
                                    listOf(
                                        when (usos) {
                                            0 -> "Ninguna publicidad lo usa"
                                            1 -> "Lo usa 1 publicidad"
                                            else -> "Lo usan $usos publicidades"
                                        }
                                    ),
                                    serif = tabla != Tabla.CLIENTE, resaltada = edicion == r.codigo,
                                    alModificar = { abrirFormulario(r.codigo) },
                                    alInactivar = { confirmar = PorConfirmar(Confirmar.INACTIVAR, r.codigo, r.nombre, usos) },
                                    alReactivar = { cambiarEstado(r.codigo, Estado.ACTIVO) },
                                    alEliminar = { pedirEliminar(r.codigo, r.nombre) },
                                )
                            }
                        }
                    }
                }

                if (edicion == null && pinElegido == null) ExtendedFloatingActionButton(
                    onClick = { abrirFormulario(null) }, containerColor = Tinta, contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Adicionar", style = sans(16, 700, Color.White)) },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
                )
            }

            // ---- formulario en la mitad inferior (misma pantalla) ----
            edicion?.let { e ->
                key(vecesAbierto) {
                    PanelFormulario(
                        db, version, tabla, e.ifEmpty { null },
                        modifier = Modifier.height(altoPanel),
                        alConsultar = { t, actual, cargar -> consulta = Consulta(t, actual, cargar) },
                        alGuardar = { c -> cerrarFormulario(); guardado = c; alCambiarDatos() },
                        avisar = avisar,
                        alCancelar = ::cerrarFormulario,
                    )
                }
            }
        }

        // ---- consulta de selección de la tabla referencial (solo activos, orden alfabético) ----
        consulta?.let { c ->
            val activos = remember(version, c.tabla) { db.activos(c.tabla) }
            HojaSeleccion(
                c.tabla, activos, c.actual,
                alCargar = { r -> c.cargar(r.codigo); consulta = null },
                alCancelar = { consulta = null },
            )
        }
    }

    confirmar?.let { c ->
        val eliminar = c.tipo == Confirmar.ELIMINAR
        AlertDialog(
            onDismissRequest = { confirmar = null },
            containerColor = Color.White,
            title = { Text(if (eliminar) "¿Eliminar ${c.codigo}?" else "¿Inactivar ${c.codigo}?", style = sans(20, 700)) },
            text = {
                Text(
                    if (eliminar) "El registro «${c.nombre}» quedará con estado * (eliminado) y ya no se podrá modificar."
                    else "El registro «${c.nombre}» quedará con estado I (inactivo)." +
                        if (c.usos > 0) " Las ${c.usos} publicidades que lo usan lo conservan, pero no se podrá elegir en nuevas." else "",
                    style = sans(15, 400),
                )
            },
            confirmButton = {
                TextButton({
                    confirmar = null
                    cambiarEstado(c.codigo, if (eliminar) Estado.ELIMINADO else Estado.INACTIVO)
                }) { Text(if (eliminar) "Sí, eliminar" else "Sí, inactivar", style = sans(15, 700, Pin)) }
            },
            dismissButton = { TextButton({ confirmar = null }) { Text("Cancelar", style = sans(15, 600)) } },
        )
    }
}
