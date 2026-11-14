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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.Estado
import com.unsa.publicidad.prototipo.datos.Orden
import com.unsa.publicidad.prototipo.datos.Registro
import com.unsa.publicidad.prototipo.datos.Tabla
import com.unsa.publicidad.prototipo.datos.numeroDe

/** Buscador flotante sobre el mapa. La flecha es "Salir". */
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

/** Leyenda: el estado se lee en el pin. */
@Composable
fun Leyenda() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Estado.ACTIVO to "activo", Estado.INACTIVO to "inactivo", Estado.ELIMINADO to "eliminado").forEach { (e, s) ->
            PinMapa("", e, tam = 18.dp)
            Text(s, style = sans(13, 400, Gris)); Spacer(Modifier.width(6.dp))
        }
    }
}

/** Fila de la lista: pin + nombre + subtítulo. */
@Composable
fun FilaLista(codigo: String, estado: Char, titulo: String, subtitulo: String, serif: Boolean = false, alTocar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = alTocar).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PinMapa(numeroDe(codigo), estado)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = (if (serif) lugar(18) else sans(16, 600)).let {
                    if (estado == Estado.ELIMINADO) it.copy(color = Gris) else it
                },
            )
            Text(subtitulo, style = sans(13, 400, Gris), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Etiqueta con el estado del registro. */
@Composable
fun ChipEstado(estado: Char) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Papel).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PinMapa("", estado, tam = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text("$estado · ${Estado.texto(estado)}", style = sans(13, 700, if (estado == Estado.ELIMINADO) Gris else Tinta))
    }
}

/** Campo con línea inferior. Editable o de solo lectura. */
@Composable
fun CampoTexto(
    etiqueta: String, valor: String, alCambiar: (String) -> Unit, editable: Boolean,
    error: String? = null, estilo: TextStyle = sans(18, 600),
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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

/** Botón de acción de la barra inferior del detalle. */
@Composable
fun RowScope.BotonAccion(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Tinta, alTocar: () -> Unit) {
    OutlinedButton(
        alTocar, Modifier.weight(1f).height(48.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .5f)),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = sans(15, 700, color), maxLines = 1)
    }
}
