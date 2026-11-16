package com.unsa.publicidad.prototipo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.*

/**
 * Panel de Adición / Modificación que se abre en la MISMA pantalla, en la mitad de abajo,
 * dejando la lista a la vista arriba. Botones: Cancelar (✕) y Guardar.
 * En la maestra, Cliente / Departamento / Zona se cargan con la consulta de selección
 * (solo activos, en orden alfabético).
 *
 * @param codigoInicial null = Adición de un registro nuevo; si no, el código que se modifica
 * @param alConsultar   abre la consulta de selección de esa tabla referencial; recibe el código
 *                      actual y una función para cargar el código elegido
 */
@Composable
fun PanelFormulario(
    db: BaseDatos, version: Int, tabla: Tabla, codigoInicial: String?,
    modifier: Modifier = Modifier,
    alConsultar: (Tabla, actual: String, cargar: (String) -> Unit) -> Unit,
    alGuardar: (codigo: String) -> Unit, avisar: (String) -> Unit, alCancelar: () -> Unit,
) {
    val esMaestra = tabla == Tabla.PUBLICIDAD
    val esNuevo = codigoInicial == null
    val codigo = remember { codigoInicial ?: db.siguienteCodigo(tabla) }

    val pub = remember(version) { if (esMaestra && !esNuevo) db.publicidad(codigo) else null }
    val reg = remember(version) { if (!esMaestra && !esNuevo) db.buscar(tabla, codigo) else null }
    val estado = pub?.estado ?: reg?.estado ?: Estado.ACTIVO

    // campos del formulario (se cargan una sola vez: lo que se escribe no se pierde si cambian los datos)
    var nombre by remember { mutableStateOf(pub?.nombre ?: reg?.nombre ?: "") }
    var cliente by remember { mutableStateOf(pub?.cliente ?: "") }
    var departamento by remember { mutableStateOf(pub?.departamento ?: "") }
    var zona by remember { mutableStateOf(pub?.zona ?: "") }
    var calle by remember { mutableStateOf(pub?.calle ?: "") }
    var intentoGuardar by remember { mutableStateOf(false) }

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
        alGuardar(codigo)
    }

    Column(
        modifier.fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White)
            .navigationBarsPadding(),
    ) {
        Asa()
        // ---- cabecera: título, estado y cancelar ----
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    (if (esNuevo) "Adicionar " else "Modificar ") +
                        (if (esMaestra) "publicidad" else tabla.singular) + " · $codigo",
                    style = sans(19, 700),
                )
                Text(
                    if (esNuevo) "El código se asigna solo y el estado queda en A."
                    else "Cambie los datos y toque Guardar.",
                    style = sans(13, 400, Gris),
                )
            }
            LetraEstado(estado, tam = 26.dp)
            IconButton(alCancelar) { Icon(Icons.Default.Close, "Cancelar", tint = Tinta) }
        }

        // ---- campos ----
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            CampoTexto(
                if (esMaestra) "Nombre de la publicidad" else "Nombre", nombre, { nombre = it }, true, mostrar(errNombre),
                estilo = if (!esMaestra && tabla != Tabla.CLIENTE) lugar(20) else sans(18, 600),
            )
            if (esMaestra) {
                CampoReferencia("Cliente", cliente, db.nombreDe(Tabla.CLIENTE, cliente), true, false, mostrar(errCliente)) {
                    alConsultar(Tabla.CLIENTE, cliente) { cliente = it }
                }
                CampoReferencia("Departamento", departamento, db.nombreDe(Tabla.DEPARTAMENTO, departamento), true, true, mostrar(errDep)) {
                    alConsultar(Tabla.DEPARTAMENTO, departamento) { departamento = it }
                }
                CampoReferencia("Zona", zona, db.nombreDe(Tabla.ZONA, zona), true, true, mostrar(errZona)) {
                    alConsultar(Tabla.ZONA, zona) { zona = it }
                }
                CampoTexto("Calle de ubicación", calle, { calle = it }, true, mostrar(errCalle))
            }
            Spacer(Modifier.height(8.dp))
        }

        // ---- Cancelar / Guardar ----
        Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(alCancelar, Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Default.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Cancelar", style = sans(16, 600))
            }
            Button(
                ::guardar, Modifier.weight(1.3f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tinta),
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Guardar", style = sans(16, 700, Color.White))
            }
        }
    }
}
