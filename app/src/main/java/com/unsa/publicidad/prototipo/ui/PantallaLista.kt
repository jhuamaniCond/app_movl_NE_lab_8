package com.unsa.publicidad.prototipo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.publicidad.prototipo.datos.*

/** Modo de trabajo de la ficha. */
private enum class Modo { CONSULTA, ADICION, MODIFICACION }
private enum class Confirmar { ELIMINAR, INACTIVAR }
private class Consulta(val tabla: Tabla, val actual: String, val cargar: (String) -> Unit)

/** Lo que se escribe en la ficha durante una Adición o una Modificación. */
private class Formulario(val codigo: String, p: Publicidad?, r: Registro?) {
    var nombre by mutableStateOf(p?.nombre ?: r?.nombre ?: "")
    var cliente by mutableStateOf(p?.cliente ?: "")
    var departamento by mutableStateOf(p?.departamento ?: "")
    var zona by mutableStateOf(p?.zona ?: "")
    var calle by mutableStateOf(p?.calle ?: "")
    var intentoGuardar by mutableStateOf(false)
}

/**
 * Mantenimiento de una tabla, todo en una sola pantalla:
 *  - arriba: buscador (← Salir; en la maestra busca también por cliente, calle, departamento y zona), orden (Clientes abre por Nombre; el resto por Código) y leyenda A / I / *;
 *  - al centro: las tarjetas de la tabla (en el Maestro de Publicidad también se puede ver el Mapa);
 *  - abajo: la FICHA, una hoja deslizable con el modo de trabajo (consulta, adición o modificación),
 *    los datos del registro elegido y los botones de las funciones (Modificar, Inactivar, Reactivar,
 *    Eliminar / Adicionar, Cancelar, Guardar), que se encienden o se apagan según el modo y el estado
 *    del registro (A, I, *). Buscar y Salir están arriba: el buscador y la flecha ←.
 * Se toca una tarjeta (o un pin) para elegir el registro y luego se usa la función deseada.
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
    var modo by remember(tabla) { mutableStateOf(Modo.CONSULTA) }
    var elegido by remember(tabla) { mutableStateOf<String?>(null) }
    var form by remember(tabla) { mutableStateOf<Formulario?>(null) }
    var posicion by remember(tabla) { mutableStateOf(PosicionHoja.ABAJO) }
    var asomada by remember { mutableStateOf(280.dp) }
    var confirmar by remember { mutableStateOf<Confirmar?>(null) }
    var consulta by remember { mutableStateOf<Consulta?>(null) }
    var guardado by remember { mutableStateOf<String?>(null) }
    val teclado = LocalSoftwareKeyboardController.current
    val foco = LocalFocusManager.current

    val departamentos = remember(version) { db.listar(Tabla.DEPARTAMENTO) }
    val clientes = remember(version) { db.listar(Tabla.CLIENTE) }
    val zonas = remember(version) { db.listar(Tabla.ZONA) }
    val publicidades = remember(version, orden) { db.publicidades(orden) }
    val registros = remember(version, orden, tabla) { if (esMaestra) emptyList() else db.listar(tabla, orden) }
    fun nombre(lista: List<Registro>, c: String) = lista.firstOrNull { it.codigo == c }?.nombre ?: "—"

    val q = busqueda.trim()
    fun coincide(vararg campos: String) = q.isEmpty() || campos.any { it.contains(q, ignoreCase = true) }
    val pubsFiltradas = publicidades.filter {
        coincide(it.codigo, it.nombre, it.calle, nombre(clientes, it.cliente), nombre(departamentos, it.departamento), nombre(zonas, it.zona))
    }
    val regsFiltrados = registros.filter { coincide(it.nombre, it.codigo) }
    val total = if (esMaestra) publicidades.size else registros.size
    val mostrados = if (esMaestra) pubsFiltradas.size else regsFiltrados.size
    val codigos = if (esMaestra) pubsFiltradas.map { it.codigo } else regsFiltrados.map { it.codigo }

    // registro elegido (el que se ve en la ficha en modo consulta)
    val pubElegida = if (esMaestra) publicidades.firstOrNull { it.codigo == elegido } else null
    val regElegido = if (!esMaestra) registros.firstOrNull { it.codigo == elegido } else null
    val estadoElegido = pubElegida?.estado ?: regElegido?.estado
    val hayElegido = estadoElegido != null
    fun usosDe(codigo: String) = if (esMaestra) 0 else publicidades.count {
        it.estado != Estado.ELIMINADO && BaseDatos.campo(it, tabla) == codigo
    }

    // ---- validación (solo en Adición / Modificación) ----
    val f = form
    val errNombre = f?.let {
        when {
            it.nombre.isBlank() -> "Ingrese el nombre"
            !esMaestra && registros.any { r ->
                r.codigo != it.codigo && r.estado != Estado.ELIMINADO && alfabetico.compare(r.nombre.trim(), it.nombre.trim()) == 0
            } -> "Ya existe ${tabla.articuloUn} ${tabla.singular} con ese nombre"
            else -> null
        }
    }
    val errCliente = if (f != null && esMaestra && f.cliente.isEmpty()) "Elija el cliente" else null
    val errDep = if (f != null && esMaestra && f.departamento.isEmpty()) "Elija el departamento" else null
    val errZona = if (f != null && esMaestra && f.zona.isEmpty()) "Elija la zona" else null
    val errCalle = if (f != null && esMaestra && f.calle.isBlank()) "Ingrese la calle de ubicación" else null
    val hayErrores = listOf(errNombre, errCliente, errDep, errZona, errCalle).any { it != null }
    fun mostrar(e: String?) = if (f?.intentoGuardar == true) e else null

    // ---- las 9 funciones ----
    fun cerrarTeclado() { foco.clearFocus(); teclado?.hide() }

    fun adicionar() {
        cerrarTeclado()
        form = Formulario(db.siguienteCodigo(tabla), null, null)
        modo = Modo.ADICION
        posicion = PosicionHoja.MEDIO
    }

    fun modificar() {
        val c = elegido ?: return
        cerrarTeclado()
        form = Formulario(c, pubElegida, regElegido)
        modo = Modo.MODIFICACION
        posicion = PosicionHoja.MEDIO
    }

    fun cambiarEstado(nuevo: Char) {
        val c = elegido ?: return
        db.cambiarEstado(tabla, c, nuevo)
        alCambiarDatos()
        avisar(
            when (nuevo) {
                Estado.ACTIVO -> "${tabla.singularTitulo} $c reactivado (A)"
                Estado.INACTIVO -> "${tabla.singularTitulo} $c inactivado (I)"
                else -> "${tabla.singularTitulo} $c eliminado (*)"
            }
        )
    }

    fun eliminar() {
        val c = elegido ?: return
        val usos = db.publicidadesQueUsan(tabla, c)
        if (usos.isNotEmpty()) avisar("No se puede eliminar: lo usa${if (usos.size == 1) " la publicidad" else "n las publicidades"} ${usos.joinToString { it.codigo }}")
        else confirmar = Confirmar.ELIMINAR
    }


    fun cancelar() {
        if (modo == Modo.CONSULTA) elegido = null   // en consulta, Cancelar suelta el registro elegido
        else { modo = Modo.CONSULTA; form = null; cerrarTeclado(); posicion = PosicionHoja.ABAJO }
    }

    fun guardar() {
        val datos = form ?: return
        datos.intentoGuardar = true
        if (hayErrores) { avisar("Revise los datos marcados en rojo"); posicion = PosicionHoja.MEDIO; return }
        val nuevo = modo == Modo.ADICION
        try {
            if (esMaestra) {
                val p = Publicidad(datos.codigo, datos.nombre, datos.cliente, datos.departamento, datos.zona, datos.calle,
                    estadoElegido ?: Estado.ACTIVO)
                if (nuevo) db.insertar(p) else db.modificar(p)
            } else {
                if (nuevo) db.insertar(tabla, datos.codigo, datos.nombre) else db.modificar(tabla, datos.codigo, datos.nombre)
            }
        } catch (e: Exception) {
            avisar("No se pudo guardar: ${e.message}"); return
        }
        avisar("${tabla.singularTitulo} ${datos.codigo} " + if (nuevo) "adicionado con estado A (activo)" else "modificado")
        elegido = datos.codigo
        guardado = datos.codigo
        modo = Modo.CONSULTA; form = null
        cerrarTeclado()
        posicion = PosicionHoja.ABAJO
        alCambiarDatos()
    }

    fun elegir(codigo: String) {
        if (modo != Modo.CONSULTA) {
            avisar("Primero guarde o cancele la ${if (modo == Modo.ADICION) "adición" else "modificación"}")
            return
        }
        cerrarTeclado()
        elegido = codigo   // para soltarlo está Cancelar
    }

    BackHandler(enabled = consulta != null || modo != Modo.CONSULTA || posicion != PosicionHoja.ABAJO || elegido != null) {
        when {
            consulta != null -> consulta = null
            modo != Modo.CONSULTA -> cancelar()
            posicion != PosicionHoja.ABAJO -> posicion = PosicionHoja.ABAJO
            else -> elegido = null
        }
    }

    // al guardar, la lista se desplaza hasta el registro (uno nuevo puede quedar fuera de vista)
    val estadoLista = rememberLazyListState()
    // al pulsar Modificar, la tarjeta del registro sube a la vista (queda encima de la ficha)
    LaunchedEffect(modo) {
        val i = codigos.indexOf(elegido)
        if (modo == Modo.MODIFICACION && i >= 0) estadoLista.animateScrollToItem(i)
    }
    LaunchedEffect(guardado, codigos) {
        val i = codigos.indexOf(guardado ?: return@LaunchedEffect)
        if (i >= 0) { estadoLista.animateScrollToItem(i); guardado = null }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            BuscadorFlotante(
                busqueda, { busqueda = it },
                when (tabla) {
                    Tabla.PUBLICIDAD -> "Buscar publicidad, cliente o lugar"
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
                    if (esMaestra) SelectorVista(enMapa) { enMapa = it }
                }
                Box(Modifier.padding(top = 8.dp, bottom = 6.dp)) { Leyenda() }
            }

            // ---- tarjetas o mapa ----
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (esMaestra && enMapa) {
                    Mapa(
                        Modifier.fillMaxSize(), departamentos, pubsFiltradas,
                        margenSuperior = 0f, todas = publicidades, margenInferior = asomada,
                        alTocarPin = { p -> elegir(p.codigo) }, elegido = elegido,
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(), state = estadoLista,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = asomada + 12.dp),
                    ) {
                        if (mostrados == 0) item {
                            Text(
                                if (q.isEmpty()) "Todavía no hay ${tabla.plural}. Use Adicionar en la ficha de abajo."
                                else "Ningún registro coincide con «$q».",
                                style = sans(15, 400, Gris), modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
                            )
                        }
                        if (esMaestra) {
                            items(pubsFiltradas, key = { it.codigo }) { p ->
                                TarjetaRegistro(
                                    p.codigo, p.estado, p.nombre,
                                    listOf(
                                        "Cliente: ${nombre(clientes, p.cliente)}",
                                        "${nombre(departamentos, p.departamento)} · zona ${nombre(zonas, p.zona).lowercase()} · ${p.calle}",
                                    ),
                                    elegida = elegido == p.codigo,
                                    editando = modo == Modo.MODIFICACION && elegido == p.codigo,
                                ) { elegir(p.codigo) }
                            }
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
                                    serif = tabla != Tabla.CLIENTE,
                                    elegida = elegido == r.codigo,
                                    editando = modo == Modo.MODIFICACION && elegido == r.codigo,
                                ) { elegir(r.codigo) }
                            }
                        }
                    }
                }
            }
        }

        // ---- ficha: hoja deslizable con el modo, los datos y las 9 funciones ----
        val consultando = modo == Modo.CONSULTA
        val estadoFicha = if (modo == Modo.ADICION) Estado.ACTIVO else estadoElegido
        val codigoFicha = f?.codigo ?: elegido
        HojaDeslizable(
            posicion = posicion, alCambiarPosicion = { posicion = it },
            reservaSuperior = 72.dp, subirConTeclado = !consultando,
            alMedirAsomada = { asomada = it },
            cabecera = {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (modo) {
                                Modo.CONSULTA -> "Modo consulta"
                                Modo.ADICION -> "Modo adición · $codigoFicha"
                                Modo.MODIFICACION -> "Modo modificación · $codigoFicha"
                            }.uppercase(),
                            style = sans(12, 700, when (modo) {
                                Modo.CONSULTA -> Pin
                                Modo.ADICION -> Verde
                                Modo.MODIFICACION -> Ambar
                            }).copy(letterSpacing = 1.4.sp),
                        )
                        val titulo = when (modo) {
                            Modo.CONSULTA -> pubElegida?.nombre ?: regElegido?.nombre
                            else -> f?.nombre?.ifBlank { null } ?: if (modo == Modo.ADICION) tabla.nuevoTitulo else "—"
                        }
                        Text(
                            titulo ?: "Ningún registro elegido",
                            style = if (titulo == null) sans(18, 600, Gris)
                            else if (!esMaestra && tabla != Tabla.CLIENTE) lugar(21, Tinta, 600) else sans(19, 700),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            when {
                                modo == Modo.ADICION -> "Complete los datos y pulse Guardar. Entra con estado A."
                                modo == Modo.MODIFICACION -> "Cambie los datos y pulse Guardar, o Cancelar."
                                !hayElegido -> (if (esMaestra && enMapa) "Toque un pin del mapa" else "Toque una tarjeta") + " para elegirla, o pulse Adicionar."
                                estadoElegido == Estado.ELIMINADO -> "$elegido · eliminado (*): solo lectura"
                                else -> "$elegido · ${Estado.texto(estadoElegido!!).lowercase()}" +
                                    if (posicion == PosicionHoja.ABAJO) " · deslice hacia arriba para ver la ficha" else ""
                            },
                            style = sans(13, 400, Gris), maxLines = 2,
                        )
                    }
                    if (estadoFicha != null) { Spacer(Modifier.width(10.dp)); LetraEstado(estadoFicha, tam = 34.dp) }
                }
            },
            pie = {
                // Buscar y Salir no van aquí: ya están arriba (buscador y flecha ←)
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val vivo = hayElegido && estadoElegido != Estado.ELIMINADO
                    // acciones sobre el registro elegido
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonFuncion("Modificar", Icons.Default.Edit, consultando && vivo, compacto = true, alTocar = ::modificar)
                        BotonFuncion("Inactivar", Icons.Default.Lock, consultando && estadoElegido == Estado.ACTIVO, Ambar, compacto = true) {
                            confirmar = Confirmar.INACTIVAR
                        }
                        BotonFuncion("Reactivar", Icons.Default.Refresh, consultando && estadoElegido == Estado.INACTIVO, Verde, compacto = true) {
                            cambiarEstado(Estado.ACTIVO)
                        }
                        BotonFuncion("Eliminar", Icons.Default.Delete, consultando && vivo, Pin, compacto = true, alTocar = ::eliminar)
                    }
                    // registro nuevo y edición
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonFuncion("Adicionar", Icons.Default.Add, consultando, alTocar = ::adicionar)
                        BotonFuncion("Cancelar", Icons.Default.Close, !consultando || hayElegido, alTocar = ::cancelar)
                        BotonFuncion("Guardar", Icons.Default.Check, !consultando, relleno = true, peso = 1.25f, alTocar = ::guardar)
                    }
                }
            },
        ) {
            // ---- datos del registro ----
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                HorizontalDivider(color = Linea)
                if (consultando && !hayElegido) {
                    Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Gris, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("La ficha muestra aquí los datos del registro elegido.", style = sans(14, 400, Gris))
                    }
                    return@Column
                }
                val editable = !consultando
                Row {
                    CampoTexto(
                        "Código", codigoFicha.orEmpty() + if (modo == Modo.ADICION) " (nuevo)" else "", {}, false,
                        estilo = sans(17, 600, Gris), modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(16.dp))
                    CampoTexto(
                        "Estado de registro", estadoFicha?.let { "$it · ${Estado.texto(it)}" }.orEmpty(), {}, false,
                        estilo = sans(17, 700, colorEstado(estadoFicha ?: Estado.ACTIVO)), modifier = Modifier.weight(1f),
                    )
                }
                val serifNombre = !esMaestra && tabla != Tabla.CLIENTE
                CampoTexto(
                    if (esMaestra) "Nombre de la publicidad" else "Nombre",
                    if (editable) f!!.nombre else pubElegida?.nombre ?: regElegido?.nombre.orEmpty(),
                    { form?.nombre = it }, editable, mostrar(errNombre),
                    estilo = if (serifNombre) lugar(20) else sans(18, 600),
                )
                if (esMaestra) {
                    val cli = if (editable) f!!.cliente else pubElegida?.cliente.orEmpty()
                    val dep = if (editable) f!!.departamento else pubElegida?.departamento.orEmpty()
                    val zon = if (editable) f!!.zona else pubElegida?.zona.orEmpty()
                    val cal = if (editable) f!!.calle else pubElegida?.calle.orEmpty()
                    CampoReferencia("Cliente", cli, db.nombreDe(Tabla.CLIENTE, cli), editable, false, mostrar(errCliente)) {
                        consulta = Consulta(Tabla.CLIENTE, cli) { form?.cliente = it }
                    }
                    CampoReferencia("Departamento", dep, db.nombreDe(Tabla.DEPARTAMENTO, dep), editable, true, mostrar(errDep)) {
                        consulta = Consulta(Tabla.DEPARTAMENTO, dep) { form?.departamento = it }
                    }
                    CampoReferencia("Zona", zon, db.nombreDe(Tabla.ZONA, zon), editable, true, mostrar(errZona)) {
                        consulta = Consulta(Tabla.ZONA, zon) { form?.zona = it }
                    }
                    CampoTexto("Calle de ubicación", cal, { form?.calle = it }, editable, mostrar(errCalle))
                } else if (consultando) {
                    val usos = publicidades.filter { it.estado != Estado.ELIMINADO && BaseDatos.campo(it, tabla) == elegido }
                    Text(
                        when (usos.size) {
                            0 -> "Ninguna publicidad usa este registro."
                            1 -> "Lo usa 1 publicidad:"
                            else -> "Lo usan ${usos.size} publicidades:"
                        },
                        style = sans(14, 600, Gris), modifier = Modifier.padding(top = 10.dp),
                    )
                    usos.forEach { p ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            LetraEstado(p.estado, tam = 22.dp); Spacer(Modifier.width(10.dp))
                            Text("${p.codigo} · ${p.nombre}", style = sans(15, 400))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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
        val eliminando = c == Confirmar.ELIMINAR
        val nombreElegido = pubElegida?.nombre ?: regElegido?.nombre ?: ""
        val usos = elegido?.let { usosDe(it) } ?: 0
        AlertDialog(
            onDismissRequest = { confirmar = null },
            containerColor = Color.White,
            title = { Text((if (eliminando) "¿Eliminar " else "¿Inactivar ") + "${tabla.articuloEl} ${tabla.singular} $elegido?", style = sans(20, 700)) },
            text = {
                Text(
                    if (eliminando) "El registro «$nombreElegido» quedará con estado * (eliminado) y ya no se podrá modificar."
                    else "El registro «$nombreElegido» quedará con estado I (inactivo)." +
                        if (usos > 0) " Las $usos publicidades que lo usan lo conservan, pero no se podrá elegir en nuevas." else "",
                    style = sans(15, 400),
                )
            },
            confirmButton = {
                TextButton({
                    confirmar = null
                    cambiarEstado(if (eliminando) Estado.ELIMINADO else Estado.INACTIVO)
                }) { Text(if (eliminando) "Sí, eliminar" else "Sí, inactivar", style = sans(15, 700, Pin)) }
            },
            dismissButton = { TextButton({ confirmar = null }) { Text("Cancelar", style = sans(15, 600)) } },
        )
    }
}
