package com.unsa.publicidad.prototipo.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Mapa a pantalla completa con una hoja blanca encima que se arrastra desde su cabecera
 * (asa, título...). Se queda en una de tres posiciones: abajo (casi todo el mapa a la vista),
 * en medio (la posición inicial) o arriba (casi toda la lista). Tocar el asa alterna entre
 * abajo y en medio.
 *
 * @param clave         la posición de la hoja se recuerda por clave (p. ej. por tabla)
 * @param topeInicial   distancia desde arriba hasta el borde de la hoja en la posición del medio
 * @param reservaSuperior alto de lo que flota sobre el mapa (buscador o rótulo), bajo la barra de estado
 * @param extraVisible  cuánto de la hoja queda a la vista, además de la cabecera, cuando está abajo
 * @param mapa          recibe la fracción de alto tapada arriba y el alto tapado abajo por la hoja.
 *                      Si es null no hay mapa: la barra superior, la cabecera y la lista ocupan la
 *                      pantalla completa (Inicio y tablas referenciales; el mapa es solo de la maestra).
 * @param sobreMapa     lo que flota sobre el mapa y bajo la hoja (buscador o rótulo)
 * @param alFrente      lo que flota sobre todo (botón Adicionar)
 */
@Composable
fun MapaConHoja(
    clave: Any,
    topeInicial: Dp,
    reservaSuperior: Dp,
    extraVisible: Dp = 16.dp,
    mapa: (@Composable (margenSuperior: Float, margenInferior: Dp) -> Unit)?,
    sobreMapa: @Composable BoxScope.() -> Unit,
    alFrente: @Composable BoxScope.() -> Unit = {},
    cabecera: @Composable ColumnScope.() -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    if (mapa == null) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth()) { sobreMapa() }
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)) { cabecera() }
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) { contenido() }
            }
            alFrente()
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Papel)) {
        val densidad = LocalDensity.current
        val alto = maxHeight
        val barraEstado = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val barraNavegacion = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        var altoCabecera by remember { mutableStateOf(110.dp) }

        // las tres posiciones, en dp desde arriba
        val arriba = (barraEstado + reservaSuperior).value
        val abajo = max(alto - altoCabecera - extraVisible - barraNavegacion, topeInicial).value
        val medio = topeInicial.value.coerceIn(arriba, abajo)
        val anclas = listOf(arriba, medio, abajo)

        var tope by rememberSaveable(clave) { mutableFloatStateOf(medio) }
        val topeVisible = tope.coerceIn(arriba, abajo)
        val alcance = rememberCoroutineScope()
        val arrastre = rememberDraggableState { delta -> tope = (tope + delta / densidad.density).coerceIn(arriba, abajo) }

        suspend fun irA(destino: Float, velocidad: Float = 0f) =
            animate(tope.coerceIn(arriba, abajo), destino, velocidad) { x, _ -> tope = x }

        mapa(arriba / alto.value, alto - topeVisible.dp)
        sobreMapa()

        Column(
            Modifier.fillMaxSize().padding(top = topeVisible.dp)
                .shadow(14.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White),
        ) {
            // cabecera: de aquí se arrastra la hoja
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { altoCabecera = with(densidad) { it.height.toDp() } }
                    .draggable(
                        arrastre, Orientation.Vertical,
                        onDragStopped = { v ->
                            val vDp = v / densidad.density
                            val destino = when {
                                vDp > 600 -> anclas.filter { it > tope + 1 }.minOrNull() ?: abajo
                                vDp < -600 -> anclas.filter { it < tope - 1 }.maxOrNull() ?: arriba
                                else -> anclas.minBy { abs(it - tope) }
                            }
                            irA(destino, vDp)
                        },
                    )
                    .padding(horizontal = 20.dp),
            ) {
                // asa más grande que la rayita para que sea fácil de tocar
                Box(
                    Modifier.fillMaxWidth().height(22.dp)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            alcance.launch { irA(if (topeVisible < abajo - 1) abajo else medio) }
                        }
                        .semantics { contentDescription = "Mostrar u ocultar la lista" },
                ) { Column(Modifier.fillMaxWidth()) { Asa() } }
                cabecera()
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) { contenido() }
        }
        alFrente()
    }
}
