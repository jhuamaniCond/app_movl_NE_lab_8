package com.unsa.publicidad.prototipo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.Estado
import com.unsa.publicidad.prototipo.datos.Orden
import com.unsa.publicidad.prototipo.datos.Registro
import com.unsa.publicidad.prototipo.datos.Tabla

/** Buscador superior (función Buscar). La flecha es "Salir". */
@Composable
fun BuscadorFlotante(texto: String, alCambiar: (String) -> Unit, pista: String, alSalir: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(52.dp)
            .shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Color.White)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(alSalir) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir", tint = Tinta) }
        Box(Modifier.weight(1f)) {
            if (texto.isEmpty()) Text(pista, style = sans(16, 400, Gris), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                texto, alCambiar, singleLine = true, textStyle = sans(16, 400),
                cursorBrush = SolidColor(Tinta),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar" },
            )
        }
        if (texto.isNotEmpty()) IconButton({ alCambiar("") }) { Icon(Icons.Default.Clear, "Limpiar búsqueda", tint = Gris) }
        else { Icon(Icons.Default.Search, null, tint = Tinta); Spacer(Modifier.width(12.dp)) }
    }
}

/** Asa de la hoja deslizable. */
@Composable
fun ColumnScope.Asa() {
    Box(Modifier.padding(top = 10.dp).size(40.dp, 4.dp).clip(CircleShape).background(Asa).align(Alignment.CenterHorizontally))
}

/** Interruptor de orden: Código | Nombre. */
@Composable
fun SelectorOrden(orden: Orden, alCambiar: (Orden) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Papel).padding(3.dp)) {
        Orden.entries.forEach { o ->
            val sel = o == orden
            Text(
                o.texto, style = sans(13, if (sel) 700 else 600, if (sel) Color.White else Tinta),
                modifier = Modifier.clip(RoundedCornerShape(15.dp)).background(if (sel) Tinta else Color.Transparent)
                    .clickable { alCambiar(o) }.padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Ordenar por ${o.texto}" },
            )
        }
    }
}

/** Color de cada estado: A verde, I ámbar, * gris. */
fun colorEstado(estado: Char) = when (estado) {
    Estado.ACTIVO -> Verde
    Estado.INACTIVO -> Ambar
    else -> Gris
}

/**
 * El estado del registro con su letra, tal como se guarda en la tabla:
 * A = activo (lleno), I = inactivo (borde), * = eliminado (gris).
 */
@Composable
fun LetraEstado(estado: Char, modifier: Modifier = Modifier, tam: Dp = 30.dp) {
    val color = colorEstado(estado)
    val lleno = estado == Estado.ACTIVO
    Box(
        modifier.size(tam).clip(RoundedCornerShape(tam / 4))
            .background(if (lleno) color else color.copy(alpha = .10f))
            .border(1.5.dp, color, RoundedCornerShape(tam / 4))
            .semantics { contentDescription = "Estado ${Estado.texto(estado)}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            estado.toString(),
            style = sans((tam.value * .5f).toInt().coerceAtLeast(9), 700, if (lleno) Color.White else color),
        )
    }
}

/** Leyenda de los estados: A activo · I inactivo · * eliminado. */
@Composable
fun Leyenda() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Estado.ACTIVO to "activo", Estado.INACTIVO to "inactivo", Estado.ELIMINADO to "eliminado").forEach { (e, s) ->
            LetraEstado(e, tam = 20.dp)
            Text(s, style = sans(13, 400, Gris)); Spacer(Modifier.width(6.dp))
        }
    }
}

/**
 * Tarjeta de un registro en la lista. Al tocarla se elige y se carga en la ficha de abajo,
 * donde están los botones de las funciones. Un registro eliminado (*) aparece tachado.
 *
 * @param lineas      datos que se muestran bajo el nombre (cliente, ubicación, uso...)
 * @param elegida     true si es el registro cargado en la ficha
 * @param editando    true mientras se modifica en la ficha
 */
@Composable
fun TarjetaRegistro(
    codigo: String, estado: Char, titulo: String, lineas: List<String>,
    serif: Boolean = false, elegida: Boolean = false, editando: Boolean = false,
    modifier: Modifier = Modifier, alTocar: () -> Unit,
) {
    val eliminado = estado == Estado.ELIMINADO
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp).clip(forma)
            .background(if (elegida) Papel else Color.White)
            .border(if (elegida) 2.dp else 1.dp, if (editando) Ambar else if (elegida) Tinta else Linea, forma)
            .clickable(onClick = alTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LetraEstado(estado, Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = (if (serif) lugar(19) else sans(17, 700)).let {
                    if (eliminado) it.copy(color = Gris, textDecoration = TextDecoration.LineThrough) else it
                },
            )
            lineas.forEach { Text(it, style = sans(13, 400, Gris), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(codigo, style = sans(13, 700, if (eliminado) Gris else Pin))
            when {
                editando -> Text("modificando", style = sans(11, 700, Ambar), modifier = Modifier.padding(top = 4.dp))
                elegida -> Icon(Icons.Default.CheckCircle, "Elegida", tint = Tinta, modifier = Modifier.padding(top = 4.dp).size(18.dp))
            }
        }
    }
}

/**
 * Botón de una función del mantenimiento (Adicionar, Modificar, Eliminar...).
 * Siempre está a la vista; si la función no se puede usar en ese momento se ve apagado.
 *
 * @param compacto  ícono sobre el texto (para la fila de 4 acciones sobre el registro elegido)
 * @param peso      ancho relativo dentro de la fila
 */
@Composable
fun RowScope.BotonFuncion(
    texto: String, icono: ImageVector, habilitado: Boolean,
    color: Color = Tinta, relleno: Boolean = false, compacto: Boolean = false, peso: Float = 1f,
    alTocar: () -> Unit,
) {
    val forma = RoundedCornerShape(14.dp)
    val fondo = when {
        !habilitado -> Papel.copy(alpha = .55f)
        relleno -> color
        else -> color.copy(alpha = .08f)
    }
    val tinta = when {
        !habilitado -> Gris.copy(alpha = .45f)
        relleno -> Color.White
        else -> color
    }
    val base = Modifier.weight(peso).clip(forma).background(fondo)
        .border(1.dp, if (habilitado && !relleno) color.copy(alpha = .35f) else Color.Transparent, forma)
        .clickable(enabled = habilitado, role = Role.Button, onClick = alTocar)
    if (compacto) {
        Column(
            base.height(58.dp).padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icono, null, tint = tinta, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(3.dp))
            Text(texto, style = sans(13, 700, tinta), maxLines = 1)
        }
    } else {
        Row(
            base.height(50.dp).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icono, null, tint = tinta, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = sans(15, 700, tinta), maxLines = 1)
        }
    }
}

/** Cambiar de vista en el Maestro de Publicidad: Lista | Mapa. */
@Composable
fun SelectorVista(enMapa: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Papel).padding(3.dp)) {
        listOf(false to "Lista", true to "Mapa").forEach { (mapa, texto) ->
            val sel = mapa == enMapa
            Row(
                Modifier.clip(RoundedCornerShape(15.dp)).background(if (sel) Tinta else Color.Transparent)
                    .clickable { alCambiar(mapa) }.padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Ver como $texto" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (mapa) Icons.Default.Place else Icons.AutoMirrored.Filled.List, null,
                    tint = if (sel) Color.White else Tinta, modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(texto, style = sans(13, if (sel) 700 else 600, if (sel) Color.White else Tinta))
            }
        }
    }
}

/** Etiqueta con el estado del registro: letra + texto. */
@Composable
fun ChipEstado(estado: Char) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Papel).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetraEstado(estado, tam = 18.dp)
        Spacer(Modifier.width(6.dp))
        Text(Estado.texto(estado), style = sans(13, 700, colorEstado(estado)))
    }
}

/** Campo con línea inferior. Editable o de solo lectura. */
@Composable
fun CampoTexto(
    etiqueta: String, valor: String, alCambiar: (String) -> Unit, editable: Boolean,
    error: String? = null, estilo: TextStyle = sans(18, 600), modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(etiqueta, style = sans(12, 600, if (error != null) Pin else Gris))
        if (editable) {
            BasicTextField(
                valor, alCambiar, singleLine = true, textStyle = estilo, cursorBrush = SolidColor(Tinta),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics { contentDescription = etiqueta },
            )
        } else {
            Text(valor.ifEmpty { "—" }, style = estilo, modifier = Modifier.padding(vertical = 6.dp))
        }
        HorizontalDivider(color = if (error != null) Pin else if (editable) Tinta else Linea, thickness = if (editable) 1.5.dp else 1.dp)
        if (error != null) Text(error, style = sans(12, 600, Pin), modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Campo que referencia a otra tabla: muestra nombre + código.
 * En edición, al tocarlo se abre la consulta de selección.
 */
@Composable
fun CampoReferencia(
    etiqueta: String, codigo: String, nombre: String, editable: Boolean, serif: Boolean,
    error: String? = null, alConsultar: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .then(if (editable) Modifier.clickable(onClick = alConsultar) else Modifier)
            .semantics { contentDescription = "Consultar $etiqueta" },
    ) {
        Text(etiqueta, style = sans(12, 600, if (error != null) Pin else Gris))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            if (codigo.isEmpty()) Text("Elegir ${etiqueta.lowercase()}…", style = sans(17, 400, Gris), modifier = Modifier.weight(1f))
            else Text(nombre, style = if (serif) lugar(19) else sans(17, 600), modifier = Modifier.weight(1f))
            if (codigo.isNotEmpty()) Text(codigo, style = sans(13, 600, Gris))
            if (editable) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.KeyboardArrowDown, null, tint = Tinta) }
        }
        HorizontalDivider(color = if (error != null) Pin else if (editable) Tinta else Linea, thickness = if (editable) 1.5.dp else 1.dp)
        if (error != null) Text(error, style = sans(12, 600, Pin), modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Consulta de selección de una tabla referencial, como hoja inferior sobre la pantalla.
 * Muestra solo los registros activos en orden alfabético; "Cargar" devuelve el código elegido
 * y "Cancelar" vuelve sin cambiar nada.
 */
@Composable
fun HojaSeleccion(
    tabla: Tabla,
    activos: List<Registro>,
    actual: String,
    alCargar: (Registro) -> Unit,
    alCancelar: () -> Unit,
) {
    var elegido by remember(actual) { mutableStateOf(activos.firstOrNull { it.codigo == actual }) }
    val serif = tabla != Tabla.CLIENTE
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Tinta.copy(alpha = .45f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = alCancelar),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.64f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .navigationBarsPadding().padding(horizontal = 22.dp),
        ) {
            Asa()
            Spacer(Modifier.height(16.dp))
            Text(tabla.pregunta, style = sans(21, 700))
            Text("Solo ${tabla.plural} activ${if (tabla.femenino) "as" else "os"}, en orden alfabético.", style = sans(14, 400, Gris))
            Spacer(Modifier.height(10.dp))
            if (activos.isEmpty()) {
                Text("No hay ${tabla.plural} activ${if (tabla.femenino) "as" else "os"}. Adicione o reactive uno primero.",
                    style = sans(15, 400, Gris), modifier = Modifier.padding(vertical = 20.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                items(activos, key = { it.codigo }) { r ->
                    val sel = r.codigo == elegido?.codigo
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Papel else Color.Transparent)
                            .clickable { elegido = r }.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            r.nombre, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = if (serif) lugar(21, Tinta, if (sel) 600 else 400) else sans(18, if (sel) 700 else 400),
                        )
                        Text(r.codigo, style = sans(13, 600, Gris))
                        if (sel) { Spacer(Modifier.width(10.dp)); Icon(Icons.Default.CheckCircle, "Elegido", tint = Pin) }
                    }
                }
            }
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(alCancelar, Modifier.weight(1f).height(50.dp)) { Text("Cancelar", style = sans(16, 600)) }
                Button(
                    { elegido?.let(alCargar) }, Modifier.weight(1.3f).height(50.dp), enabled = elegido != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Tinta),
                ) {
                    Text(elegido?.let { "Cargar ${it.nombre}" } ?: "Elija uno", style = sans(16, 700, Color.White),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
