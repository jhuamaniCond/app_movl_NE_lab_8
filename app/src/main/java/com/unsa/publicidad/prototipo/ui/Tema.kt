package com.unsa.publicidad.prototipo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.unsa.publicidad.prototipo.R

/*
 * Alternativa 2 — "Cartografía".
 * Cada publicidad es un lugar: el Maestro de Publicidad se puede ver como lista o como mapa del Perú.
 * Los nombres de lugares van en serif cursiva (como en los mapas) y la interfaz en sans.
 * El estado se indica con su letra: A = activo, I = inactivo, * = eliminado.
 */

val Papel = Color(0xFFEDF1EC)
val Curva = Color(0xFFA9BCAE)
val Tinta = Color(0xFF22313F)
val Agua = Color(0xFF8FB8CF)
val Pin = Color(0xFFB3261E)
val Gris = Color(0xFF66737D)
val Linea = Color(0xFFCAD3CC)
val Asa = Color(0xFFD5DBD6)
val Verde = Color(0xFF2E7D4F)   // estado A
val Ambar = Color(0xFFB26A00)   // estado I

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, peso: Int) = Font(
    res, FontWeight(peso), FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

val SourceSerif = FontFamily(variable(R.font.source_serif, 400), variable(R.font.source_serif, 600))
val SourceSans = FontFamily(variable(R.font.source_sans, 400), variable(R.font.source_sans, 600), variable(R.font.source_sans, 700))

/** Texto de interfaz. */
fun sans(tam: Int, peso: Int = 400, color: Color = Tinta) =
    TextStyle(fontFamily = SourceSans, fontSize = tam.sp, fontWeight = FontWeight(peso), color = color)

/** Nombre de lugar: serif cursiva. */
fun lugar(tam: Int, color: Color = Tinta, peso: Int = 400) =
    TextStyle(fontFamily = SourceSerif, fontSize = tam.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight(peso), color = color)

@Composable
fun TemaCartografia(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Tinta, onPrimary = Color.White,
            secondary = Pin, error = Pin,
            background = Color.White, surface = Color.White, onSurface = Tinta,
            outline = Linea,
        ),
        content = contenido,
    )
}
