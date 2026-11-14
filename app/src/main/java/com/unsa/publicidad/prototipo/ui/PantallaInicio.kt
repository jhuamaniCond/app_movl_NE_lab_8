package com.unsa.publicidad.prototipo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.BaseDatos
import com.unsa.publicidad.prototipo.datos.Estado
import com.unsa.publicidad.prototipo.datos.Tabla

/** Pantalla inicial: acceso al mantenimiento de las 4 tablas (el mapa está dentro del Maestro de Publicidad). */
@Composable
fun PantallaInicio(db: BaseDatos, version: Int, alAbrir: (Tabla) -> Unit, alSalir: () -> Unit) {
    val publicidades = remember(version) { db.publicidades() }
    val conteos = remember(version) {
        Tabla.entries.associateWith { t ->
            val estados = if (t == Tabla.PUBLICIDAD) publicidades.map { it.estado } else db.listar(t).map { it.estado }
            estados.size to estados.count { it == Estado.ACTIVO }
        }
    }

    MapaConHoja(
        clave = "inicio", topeInicial = 330.dp, reservaSuperior = 80.dp,
        mapa = null,   // el mapa se ve solo dentro del Maestro de Publicidad
        sobreMapa = {
            // rótulo superior (en lugar del buscador)
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Color.White)
                    .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Publicidad exterior", style = sans(18, 700))
                    Text("Soluciones Móviles S.A.C. · Prototipo M-Business", style = sans(12, 400, Gris))
                }
                IconButton(alSalir) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Salir de la aplicación", tint = Tinta) }
            }
        },
        cabecera = {
            Text("Mantenimiento", style = sans(22, 700), modifier = Modifier.padding(top = 2.dp))
            Text("Cuatro tablas relacionadas. Elija una para darle mantenimiento.", style = sans(14, 400, Gris))
            Spacer(Modifier.height(8.dp))
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Tabla.entries.forEach { t ->
                val (total, activos) = conteos.getValue(t)
                FilaTabla(t, total, activos) { alAbrir(t) }
                if (t == Tabla.PUBLICIDAD) Text(
                    "Tablas referenciales", style = lugar(16, Gris),
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Leyenda()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FilaTabla(t: Tabla, total: Int, activos: Int, alTocar: () -> Unit) {
    val maestra = t == Tabla.PUBLICIDAD
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp))
            .background(if (maestra) Papel else Color.Transparent)
            .clickable(onClick = alTocar).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (maestra) Pin else Tinta), contentAlignment = Alignment.Center) {
            Text(t.prefijo, style = sans(17, 700, Color.White))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (maestra) "Maestro de Publicidad" else t.pluralTitulo, style = sans(17, 700))
            Text("$total registros · $activos activos", style = sans(13, 400, Gris))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Gris)
    }
}
