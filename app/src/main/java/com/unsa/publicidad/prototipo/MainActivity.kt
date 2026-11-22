package com.unsa.publicidad.prototipo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unsa.publicidad.prototipo.datos.BaseDatos
import com.unsa.publicidad.prototipo.datos.Tabla
import com.unsa.publicidad.prototipo.ui.*
import kotlinx.coroutines.launch

/** Rutas de navegación de la app. */
sealed interface Ruta {
    data object Inicio : Ruta
    /** Mantenimiento completo de la tabla: la Adición y la Modificación se hacen en la misma pantalla. */
    data class Lista(val tabla: Tabla) : Ruta
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent { TemaCartografia { App(onSalir = { finish() }) } }
    }
}

@Composable
fun App(onSalir: () -> Unit) {
    val contexto = LocalContext.current
    val db = remember { BaseDatos(contexto.applicationContext) }
    // se incrementa cada vez que cambian los datos, para volver a leer las tablas
    var version by remember { mutableIntStateOf(0) }
    val pila = remember { mutableStateListOf<Ruta>(Ruta.Inicio) }
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    fun ir(r: Ruta) { pila.add(r) }
    fun volver() { if (pila.size > 1) pila.removeAt(pila.lastIndex) else onSalir() }
    fun avisar(texto: String) {
        alcance.launch { avisos.currentSnackbarData?.dismiss(); avisos.showSnackbar(texto) }
    }

    Box(Modifier.fillMaxSize()) {
        when (val r = pila.last()) {
            Ruta.Inicio -> PantallaInicio(
                db, version,
                alAbrir = { ir(Ruta.Lista(it)) },
                alSalir = onSalir,
            )
            is Ruta.Lista -> {
                BackHandler { volver() }
                PantallaLista(
                    db, version, r.tabla,
                    alCambiarDatos = { version++ },
                    avisar = ::avisar,
                    alSalir = ::volver,
                )
            }
        }
        SnackbarHost(
            // arriba, para no tapar la botonera de la ficha inferior
            avisos, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
        ) { datos ->
            Snackbar(datos, containerColor = Tinta, contentColor = Color.White)
        }
    }
}
