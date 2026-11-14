package com.unsa.publicidad.prototipo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.*

private enum class Confirmar { ELIMINAR, INACTIVAR }

/**
 * Detalle de un registro de cualquiera de las 4 tablas.
 *  - Modo ver: Salir (←), Modificar, Inactivar / Reactivar, Eliminar.
 *  - Modo edición (Adición o Modificación): Cancelar (✕) y Guardar.
 * En la maestra, Cliente / Departamento / Zona se cargan con la consulta de selección.
 */
@Composable
fun PantallaDetalle(
    db: BaseDatos, version: Int, tabla: Tabla, codigoInicial: String?,
    alCambiarDatos: () -> Unit, avisar: (String) -> Unit, alSalir: () -> Unit,
) {
    val esMaestra = tabla == Tabla.PUBLICIDAD
    var codigo by remember { mutableStateOf(codigoInicial ?: db.siguienteCodigo(tabla)) }
    var esNuevo by remember { mutableStateOf(codigoInicial == null) }
    var editando by remember { mutableStateOf(codigoInicial == null) }

    // registro guardado (null mientras es nuevo)
    val pub = remember(version, codigo, esNuevo) { if (esMaestra && !esNuevo) db.publicidad(codigo) else null }
    val reg = remember(version, codigo, esNuevo) { if (!esMaestra && !esNuevo) db.buscar(tabla, codigo) else null }
    val estado = pub?.estado ?: reg?.estado ?: Estado.ACTIVO

    // campos del formulario
    var nombre by remember { mutableStateOf(pub?.nombre ?: reg?.nombre ?: "") }
    var cliente by remember { mutableStateOf(pub?.cliente ?: "") }
    var departamento by remember { mutableStateOf(pub?.departamento ?: "") }
    var zona by remember { mutableStateOf(pub?.zona ?: "") }
    var calle by remember { mutableStateOf(pub?.calle ?: "") }
    fun cargarCampos() {
        nombre = pub?.nombre ?: reg?.nombre ?: ""
        cliente = pub?.cliente ?: ""; departamento = pub?.departamento ?: ""
        zona = pub?.zona ?: ""; calle = pub?.calle ?: ""
    }
    LaunchedEffect(pub, reg) { if (!editando) cargarCampos() }

    var intentoGuardar by remember { mutableStateOf(false) }
    var seleccion by remember { mutableStateOf<Tabla?>(null) }
    var confirmar by remember { mutableStateOf<Confirmar?>(null) }

    val departamentos = remember(version) { db.listar(Tabla.DEPARTAMENTO) }
    val usos = remember(version, codigo) { db.publicidadesQueUsan(tabla, codigo) }
    val todas = remember(version) { db.publicidades() }

    // ---- validación ----
    val errNombre = when {
        nombre.isBlank() -> "Ingrese el nombre"
        !esMaestra && db.listar(tabla).any {
            it.codigo != codigo && it.estado != Estado.ELIMINADO && alfabetico.compare(it.nombre.trim(), nombre.trim()) == 0
        } -> "Ya existe ${tabla.articuloUn} ${tabla.singular} con ese nombre"
        else -> null
    }
    val errCliente = if (esMaestra && cliente.isEmpty()) "Elija el cliente" else null
    val errDep = if (esMaestra && departamento.isEmpty()) "Elija el departamento" else null
    val errZona = if (esMaestra && zona.isEmpty()) "Elija la zona" else null
    val errCalle = if (esMaestra && calle.isBlank()) "Ingrese la calle de ubicación" else null
    val hayErrores = listOf(errNombre, errCliente, errDep, errZona, errCalle).any { it != null }
    fun mostrar(e: String?) = if (intentoGuardar) e else null

    // ---- acciones ----
    fun cancelar() {
        if (esNuevo) alSalir()
        else { editando = false; intentoGuardar = false; cargarCampos() }
    }

    fun guardar() {
        intentoGuardar = true
        if (hayErrores) { avisar("Revise los datos marcados en rojo"); return }
        try {
            if (esMaestra) {
                val p = Publicidad(codigo, nombre, cliente, departamento, zona, calle, estado)
                if (esNuevo) db.insertar(p) else db.modificar(p)
            } else {
                if (esNuevo) db.insertar(tabla, codigo, nombre) else db.modificar(tabla, codigo, nombre)
            }
        } catch (e: Exception) {
            avisar("No se pudo guardar: ${e.message}"); return
        }
        avisar(if (esNuevo) "Adicionado $codigo con estado A (activo)" else "Modificado $codigo")
        esNuevo = false; editando = false; intentoGuardar = false
        alCambiarDatos()
    }

    fun cambiarEstado(nuevo: Char) {
        db.cambiarEstado(tabla, codigo, nuevo)
        alCambiarDatos()
        avisar(
            when (nuevo) {
                Estado.ACTIVO -> "$codigo reactivado (A)"
                Estado.INACTIVO -> "$codigo inactivado (I)"
                else -> "$codigo eliminado (*)"
            }
        )
    }

    BackHandler {
        when {
            seleccion != null -> seleccion = null
            confirmar != null -> confirmar = null
            editando -> cancelar()
            else -> alSalir()
        }
    }

    val pinesMapa = listOf(
        Publicidad(codigo, nombre, cliente, departamento.ifEmpty { "D01" }, zona, calle, estado)
    ).filter { departamento.isNotEmpty() }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            // ---- botones superiores; en la maestra van flotando sobre el mini mapa ----
            Box {
                if (esMaestra) Mapa(Modifier.fillMaxWidth().height(220.dp), departamentos, pinesMapa, margenSuperior = .30f, todas = todas, margenInferior = 0.dp)
                Row(Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (editando) {
                        IconButton(::cancelar, Modifier.clip(CircleShape).background(Color.White)) {
                            Icon(Icons.Default.Close, "Cancelar", tint = Tinta)
                        }
                    } else {
                        IconButton(alSalir, Modifier.clip(CircleShape).background(Color.White)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir", tint = Tinta)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (editando) {
                        Button(::guardar, colors = ButtonDefaults.buttonColors(containerColor = Tinta)) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text("Guardar", style = sans(15, 700, Color.White))
                        }
                    } else if (estado != Estado.ELIMINADO) {
                        Button(
                            { editando = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Tinta),
                            elevation = ButtonDefaults.buttonElevation(2.dp),
                        ) {
                            Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text("Modificar", style = sans(15, 700))
                        }
                    }
                }
            }

            // ---- formulario ----
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        codigo + if (esNuevo) " · ${tabla.nuevoTitulo}" else " · ${if (esMaestra) "Maestro de Publicidad" else tabla.singularTitulo}",
                        style = sans(13, 700, Pin), modifier = Modifier.weight(1f),
                    )
                    ChipEstado(estado)
                }
                Text(
                    when {
                        nombre.isNotBlank() -> nombre
                        esNuevo -> tabla.nuevoTitulo
                        else -> "—"
                    },
                    style = if (!esMaestra && tabla != Tabla.CLIENTE) lugar(28, Tinta, 600) else sans(26, 700),
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                )
                if (editando) Text(
                    if (esNuevo) "Adición: el código se asigna solo y el estado queda en A (activo)."
                    else "Modificación: cambie los datos y toque Guardar, o ✕ para cancelar.",
                    style = sans(13, 400, Gris),
                )
                if (estado == Estado.ELIMINADO) Aviso("Registro eliminado (*). Ya no se puede modificar ni reactivar.")
                else if (estado == Estado.INACTIVO && !editando) Aviso(
                    "Registro inactivo (I). " + if (esMaestra) "Reactívelo para volver a usarlo."
                    else "No aparece en la consulta de selección hasta reactivarlo."
                )

                CampoTexto("Código", codigo, {}, editable = false, estilo = sans(17, 600, Gris))
                CampoTexto(
                    if (esMaestra) "Nombre de la publicidad" else "Nombre", nombre, { nombre = it }, editando, mostrar(errNombre),
                    estilo = if (!esMaestra && tabla != Tabla.CLIENTE) lugar(20) else sans(18, 600),
                )
                if (esMaestra) {
                    CampoReferencia("Cliente", cliente, db.nombreDe(Tabla.CLIENTE, cliente), editando, false, mostrar(errCliente)) { seleccion = Tabla.CLIENTE }
                    CampoReferencia("Departamento", departamento, db.nombreDe(Tabla.DEPARTAMENTO, departamento), editando, true, mostrar(errDep)) { seleccion = Tabla.DEPARTAMENTO }
                    CampoReferencia("Zona", zona, db.nombreDe(Tabla.ZONA, zona), editando, true, mostrar(errZona)) { seleccion = Tabla.ZONA }
                    CampoTexto("Calle de ubicación", calle, { calle = it }, editando, mostrar(errCalle))
                } else if (!esNuevo) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (usos.size) {
                            0 -> "Ninguna publicidad usa este registro."
                            1 -> "Lo usa 1 publicidad:"
                            else -> "Lo usan ${usos.size} publicidades:"
                        },
                        style = sans(14, 600, Gris),
                    )
                    usos.forEach { p ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            PinMapa(numeroDe(p.codigo), p.estado, tam = 24.dp); Spacer(Modifier.width(10.dp))
                            Text("${p.codigo} · ${p.nombre}", style = sans(15, 400))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ---- barra de acciones (modo ver) ----
            if (!editando && estado != Estado.ELIMINADO) {
                HorizontalDivider(color = Linea)
                Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (estado == Estado.ACTIVO) BotonAccion("Inactivar", Icons.Default.Lock) { confirmar = Confirmar.INACTIVAR }
                    else BotonAccion("Reactivar", Icons.Default.Refresh) { cambiarEstado(Estado.ACTIVO) }
                    BotonAccion("Eliminar", Icons.Default.Delete, Pin) {
                        if (usos.isNotEmpty()) avisar("No se puede eliminar: lo usa ${usos.joinToString { it.codigo }}")
                        else confirmar = Confirmar.ELIMINAR
                    }
                }
            } else Spacer(Modifier.navigationBarsPadding())
        }

        // ---- consulta de selección de la tabla referencial ----
        seleccion?.let { t ->
            val activos = remember(version, t) { db.activos(t) }
            HojaSeleccion(
                t, activos,
                actual = when (t) { Tabla.CLIENTE -> cliente; Tabla.DEPARTAMENTO -> departamento; else -> zona },
                alCargar = { r ->
                    when (t) { Tabla.CLIENTE -> cliente = r.codigo; Tabla.DEPARTAMENTO -> departamento = r.codigo; else -> zona = r.codigo }
                    seleccion = null
                },
                alCancelar = { seleccion = null },
            )
        }
    }

    confirmar?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            containerColor = Color.White,
            title = { Text(if (c == Confirmar.ELIMINAR) "¿Eliminar $codigo?" else "¿Inactivar $codigo?", style = sans(20, 700)) },
            text = {
                Text(
                    if (c == Confirmar.ELIMINAR) "El registro «$nombre» quedará con estado * (eliminado) y ya no se podrá modificar."
                    else "El registro «$nombre» quedará con estado I (inactivo)." +
                        if (!esMaestra && usos.isNotEmpty()) " Las ${usos.size} publicidades que lo usan lo conservan, pero no se podrá elegir en nuevas." else "",
                    style = sans(15, 400),
                )
            },
            confirmButton = {
                TextButton({
                    confirmar = null
                    cambiarEstado(if (c == Confirmar.ELIMINAR) Estado.ELIMINADO else Estado.INACTIVO)
                }) { Text(if (c == Confirmar.ELIMINAR) "Sí, eliminar" else "Sí, inactivar", style = sans(15, 700, Pin)) }
            },
            dismissButton = { TextButton({ confirmar = null }) { Text("Cancelar", style = sans(15, 600)) } },
        )
    }
}

@Composable
private fun Aviso(texto: String) {
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Papel).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Info, null, tint = Gris, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = sans(14, 400, Tinta))
    }
}
