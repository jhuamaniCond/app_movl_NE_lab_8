package com.unsa.publicidad.prototipo.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/** Posiciones en las que se queda la hoja inferior. */
enum class PosicionHoja { ABAJO, MEDIO, ARRIBA }

/**
 * Hoja blanca que se desliza desde abajo sobre la pantalla. Se arrastra desde la cabecera o el pie
 * y se queda en una de tres posiciones:
 *  - ABAJO: solo asoman la cabecera y el pie (el contenido queda oculto);
 *  - MEDIO: ocupa más o menos la mitad de abajo de la pantalla;
 *  - ARRIBA: casi toda la pantalla (queda a la vista solo el buscador).
 * Tocar el asa alterna entre ABAJO y MEDIO.
 *
 * @param reservaSuperior    lo que queda visible arriba con la hoja subida del todo (bajo la barra de estado)
 * @param subirConTeclado    si el teclado aparece, la hoja sube para que el contenido siga a la vista
 * @param alMedirAsomada     informa el alto de lo que asoma en ABAJO (para que las listas dejen ese espacio)
 * @param cabecera           siempre visible arriba de la hoja
 * @param pie                siempre visible abajo de la hoja
 * @param contenido          entre la cabecera y el pie; se ve al subir la hoja
 */
@Composable
fun HojaDeslizable(
    posicion: PosicionHoja,
    alCambiarPosicion: (PosicionHoja) -> Unit,
    reservaSuperior: Dp,
    subirConTeclado: Boolean,
    alMedirAsomada: (Dp) -> Unit,
    cabecera: @Composable ColumnScope.() -> Unit,
    pie: @Composable ColumnScope.() -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val densidad = LocalDensity.current
        val alto = maxHeight
        val barraEstado = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val teclado = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        var altoCabecera by remember { mutableStateOf(100.dp) }
        var altoPie by remember { mutableStateOf(190.dp) }
        val asomada = altoCabecera + altoPie
        LaunchedEffect(asomada) { alMedirAsomada(asomada) }

        // las tres posiciones, en dp desde arriba
        val arriba = (barraEstado + reservaSuperior).value
        val abajo = (alto - asomada).value.coerceAtLeast(arriba)
        val medio = (alto * .40f).value.coerceIn(arriba, abajo)
        fun anclaDe(p: PosicionHoja) = when (p) {
            PosicionHoja.ABAJO -> abajo
            PosicionHoja.MEDIO -> medio
            PosicionHoja.ARRIBA -> arriba
        }

        val tope = remember { Animatable(abajo) }
        val alcance = rememberCoroutineScope()
        LaunchedEffect(posicion, abajo, medio, arriba) { tope.animateTo(anclaDe(posicion)) }
        val arrastre = rememberDraggableState { delta ->
            alcance.launch { tope.snapTo((tope.value + delta / densidad.density).coerceIn(arriba, abajo)) }
        }
        val alSoltar: suspend kotlinx.coroutines.CoroutineScope.(Float) -> Unit = { v ->
            val vDp = v / densidad.density
            val anclas = PosicionHoja.entries.associateWith { anclaDe(it) }
            val destino = when {
                vDp > 600 -> anclas.filterValues { it > tope.value + 1 }.minByOrNull { it.value }?.key ?: PosicionHoja.ABAJO
                vDp < -600 -> anclas.filterValues { it < tope.value - 1 }.maxByOrNull { it.value }?.key ?: PosicionHoja.ARRIBA
                else -> anclas.minBy { abs(it.value - tope.value) }.key
            }
            if (destino == posicion) tope.animateTo(anclaDe(destino), initialVelocity = vDp) else alCambiarPosicion(destino)
        }

        // con el teclado abierto (Adición / Modificación) la hoja sube para no tapar los campos
        val topeConTeclado = if (subirConTeclado && teclado > 0.dp)
            (alto - teclado - asomada - 150.dp).value.coerceAtLeast(arriba) else Float.MAX_VALUE
        val topeVisible = min(tope.value.coerceIn(arriba, abajo), topeConTeclado)
        val forma = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

        Column(
            Modifier.fillMaxSize().padding(top = topeVisible.dp)
                .shadow(18.dp, forma).clip(forma).background(Color.White)
                .then(if (subirConTeclado) Modifier.imePadding() else Modifier),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { altoCabecera = with(densidad) { it.height.toDp() } }
                    .draggable(arrastre, Orientation.Vertical, onDragStopped = alSoltar),
            ) {
                // asa más grande que la rayita para que sea fácil de tocar
                Box(
                    Modifier.fillMaxWidth().height(24.dp)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            alCambiarPosicion(if (posicion == PosicionHoja.ABAJO) PosicionHoja.MEDIO else PosicionHoja.ABAJO)
                        }
                        .semantics { contentDescription = "Subir o bajar la ficha" },
                ) { Column(Modifier.fillMaxWidth()) { Asa() } }
                cabecera()
            }
            Column(Modifier.weight(1f).fillMaxWidth()) { contenido() }
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { altoPie = with(densidad) { it.height.toDp() } }
                    .draggable(arrastre, Orientation.Vertical, onDragStopped = alSoltar)
                    .navigationBarsPadding(),
            ) { pie() }
        }
    }
}
