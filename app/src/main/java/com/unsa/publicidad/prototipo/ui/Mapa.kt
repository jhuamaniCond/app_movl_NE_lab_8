package com.unsa.publicidad.prototipo.ui

import android.content.Context
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.publicidad.prototipo.R
import com.unsa.publicidad.prototipo.datos.Estado
import com.unsa.publicidad.prototipo.datos.Publicidad
import com.unsa.publicidad.prototipo.datos.Registro
import com.unsa.publicidad.prototipo.datos.numeroDe
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Mapa real del Perú: los 25 departamentos con sus límites del INEI, los países vecinos y los
 * lagos Titicaca y Junín (res/raw/mapa_peru.txt). Se puede desplazar con un dedo, acercar con
 * dos dedos o doble toque, y usar los botones + / − / Perú / publicidades.
 */

// ---------------------------------------------------------------- colores del mapa
private val Oceano = Color(0xFFD5E4EA)
private val TierraVecina = Color(0xFFE4E8E3)
private val BordeVecino = Color(0xFFC5CDC7)
private val TierraPeru = Color(0xFFF9FAF7)
private val DepActivo = Color(0xFFDCE9DF)
private val DepInactivo = Color(0xFFEDF1EC)
private val Lago = Color(0xFFB2CDDC)
private val ContornoPeru = Color(0xFF7A8C81)
private val NombreSecundario = Color(0xFF8E9A92)
private val NombreAgua = Color(0xFF5F8BA3)

// ---------------------------------------------------------------- datos geográficos

/** cos(9.2°): corrige el ancho de un grado de longitud a la latitud media del Perú. */
private const val COS_LAT = 0.98714f

/** Coordenadas de "mundo": x = longitud · cos φ₀, y = −latitud (el norte queda arriba). */
private fun mundo(lon: Float, lat: Float) = Offset(lon * COS_LAT, -lat)

/** Una figura del mapa: D = departamento, C = contorno del país, P = país vecino, L = lago. */
private class Figura(val tipo: Char, val nombre: String, val etiqueta: Offset, val path: Path) {
    val clave = claveLugar(nombre)
    val caja: Rect = path.getBounds()
}

/** "Apurímac " -> "APURIMAC": sin tildes, en mayúsculas y con espacios simples. */
private fun claveLugar(s: String): String =
    Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        .uppercase().replace(Regex("\\s+"), " ")

/** Lee una sola vez res/raw/mapa_peru.txt (formato: TIPO|NOMBRE|lon,lat|anillo;anillo...). */
private object GeoPeru {
    @Volatile private var figuras: List<Figura>? = null

    fun de(ctx: Context): List<Figura> = figuras ?: synchronized(this) {
        figuras ?: leer(ctx).also { figuras = it }
    }

    private fun leer(ctx: Context): List<Figura> =
        ctx.resources.openRawResource(R.raw.mapa_peru).bufferedReader(Charsets.UTF_8).useLines { lineas ->
            lineas.filter { it.isNotBlank() && !it.startsWith("#") }.map { linea ->
                val (tipo, nombre, etiqueta, anillos) = linea.split('|')
                val path = Path().apply { fillType = PathFillType.EvenOdd }
                for (anillo in anillos.split(';')) {
                    anillo.split(' ').forEachIndexed { i, par ->
                        val p = punto(par)
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    path.close()
                }
                Figura(tipo[0], nombre, punto(etiqueta), path)
            }.toList()
        }

    private fun punto(par: String): Offset {
        val coma = par.indexOf(',')
        return mundo(par.substring(0, coma).toFloat(), par.substring(coma + 1).toFloat())
    }
}

/** Dónde cae cada departamento de la tabla: su figura real o, si el nombre no es de un departamento del Perú, un punto en el mar. */
private class Lugar(val registro: Registro, val figura: Figura?, val ancla: Offset)

private fun ubicar(departamentos: List<Registro>, geo: List<Figura>): Map<String, Lugar> {
    val porClave = geo.filter { it.tipo == 'D' }.associateBy { it.clave }
    var sinUbicar = 0
    return departamentos.sortedBy { it.codigo }.associate { d ->
        val k = claveLugar(d.nombre).removePrefix("DEPARTAMENTO DE ").let { if (it == "CUZCO") "CUSCO" else it }
        // "Lima Metropolitana" o "Lima Provincias" caen en Lima
        val f = porClave[k] ?: porClave.values.firstOrNull { k.startsWith(it.clave + " ") }
        d.codigo to if (f != null) Lugar(d, f, f.etiqueta)
        else Lugar(d, null, mundo(-80.6f, -12.4f - 1.1f * sinUbicar++))
    }
}

private fun Rect.unir(o: Rect) = Rect(min(left, o.left), min(top, o.top), max(right, o.right), max(bottom, o.bottom))

// ---------------------------------------------------------------- cámara (desplazamiento y zoom)

/** pantalla = mundo · escala + desp. */
@Stable
private class Camara {
    var escala by mutableFloatStateOf(0f)
    var desp by mutableStateOf(Offset.Zero)
    var vista = Rect.Zero           // parte del mapa que no tapan el buscador ni la hoja inferior
    var limites = Rect.Zero         // el centro de la vista no sale de aquí
    var minEscala = 1f
    var maxEscala = 1f

    fun aPantalla(p: Offset) = p * escala + desp

    /** Encuadre que muestra [caja] completa dentro de la vista. */
    fun encuadre(caja: Rect, tope: Float = maxEscala): Pair<Float, Offset> {
        val m = caja.inflate(max(caja.width, caja.height) * .06f + .2f)
        val s = min(vista.width / m.width, vista.height / m.height).coerceIn(minEscala, tope)
        return s to (vista.center - m.center * s)
    }

    fun fijar(s: Float, d: Offset) {
        val centro = (vista.center - d) / s
        val c = Offset(centro.x.coerceIn(limites.left, limites.right), centro.y.coerceIn(limites.top, limites.bottom))
        escala = s
        desp = vista.center - c * s
    }

    fun transformar(centro: Offset, pan: Offset, zoom: Float) {
        val s = (escala * zoom).coerceIn(minEscala, maxEscala)
        fijar(s, centro - (centro - desp) * (s / escala) + pan)
    }

    /** Vuelo suave: la escala cambia en progresión geométrica y el centro en línea recta. */
    suspend fun animarA(objetivo: Pair<Float, Offset>) {
        val (s1, d1) = objetivo
        val s0 = escala
        val c0 = (vista.center - desp) / s0
        val c1 = (vista.center - d1) / s1
        animate(0f, 1f, animationSpec = tween(450)) { t, _ ->
            val s = s0 * (s1 / s0).pow(t)
            fijar(s, vista.center - (c0 + (c1 - c0) * t) * s)
        }
    }

    suspend fun acercar(punto: Offset, factor: Float) {
        val s = (escala * factor).coerceIn(minEscala, maxEscala)
        animarA(s to (punto - (punto - desp) * (s / escala)))
    }
}

// ---------------------------------------------------------------- pines

/**
 * Lugar del pin respecto al centro de su departamento, en dp. [indice] es el lugar de la
 * publicidad entre las de su departamento (0, 1, 2...) y reparte los pines en fila a los lados
 * del centro: 0, +1, −1, +2, −2; desde el sexto se abre otra fila encima.
 */
private fun desplazamientoPin(indice: Int): Pair<Int, Int> {
    val k = indice % 5
    val columna = if (k == 0) 0 else if (k % 2 == 1) (k + 1) / 2 else -(k / 2)
    return columna * 30 to -(indice / 5 * 32)
}

/** Un pin ya ubicado: punto del mundo donde se ancla y desplazamiento en dp. */
private class PinUbicado(val publicidad: Publicidad, val ancla: Offset, val dx: Int, val dy: Int)

/** Pin numerado: lleno = activo, hueco = inactivo, tachado = eliminado. */
@Composable
fun PinMapa(numero: String, estado: Char, modifier: Modifier = Modifier, tam: Dp = 30.dp) {
    Box(modifier.size(tam), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(tam - 2.dp).clip(CircleShape)
                .background(if (estado == Estado.ACTIVO) Pin else Color.White)
                .border(2.dp, if (estado == Estado.ELIMINADO) Gris else Pin, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                numero,
                style = sans(if (numero.length > 2) 10 else 12, 700,
                    when (estado) { Estado.ACTIVO -> Color.White; Estado.ELIMINADO -> Gris; else -> Pin }),
            )
        }
        if (estado == Estado.ELIMINADO) Canvas(Modifier.size(tam - 2.dp)) {
            drawLine(Gris, Offset(4f, size.height - 4f), Offset(size.width - 4f, 4f), strokeWidth = 4f)
        }
    }
}

// ---------------------------------------------------------------- dibujo

/**
 * Escribe un nombre en el primero de los [centros] candidatos que no choque con lo [ocupado]
 * (pines y nombres ya escritos). Si ninguno está libre no lo escribe: aparecerá al acercar.
 */
private fun DrawScope.nombre(
    medidor: TextMeasurer, texto: String, estilo: TextStyle, ocupado: MutableList<Rect>,
    anchoMax: Float = Float.MAX_VALUE, centros: (ancho: Float, alto: Float) -> List<Offset>,
) {
    val r = medidor.measure(texto, estilo)
    val w = r.size.width.toFloat()
    val h = r.size.height.toFloat()
    if (w > anchoMax) return
    val pantalla = Rect(Offset.Zero, size)
    for (c in centros(w, h)) {
        val caja = Rect(c.x - w / 2, c.y - h / 2, c.x + w / 2, c.y + h / 2)
        if (!caja.overlaps(pantalla)) continue
        val choque = caja.deflate(h * .15f)   // el renglón trae aire arriba y abajo
        if (ocupado.any { it.overlaps(choque) }) continue
        ocupado += caja
        drawText(r, topLeft = caja.topLeft)
        return
    }
}

/**
 * Mapa del Perú con un pin por publicidad. Se desplaza y se acerca con los dedos.
 * [margenSuperior] (fracción del alto) y [margenInferior] marcan lo que tapan el buscador y la
 * hoja inferior, para encuadrar las publicidades en la parte que sí se ve.
 */
@Composable
fun Mapa(
    modifier: Modifier,
    departamentos: List<Registro>,
    pines: List<Publicidad>,
    margenSuperior: Float = .30f,
    todas: List<Publicidad> = pines,   // para ubicar cada pin entre los de su departamento
    oscurecido: Boolean = false,
    margenInferior: Dp = 30.dp,
    alTocarPin: ((Publicidad) -> Unit)? = null,
) {
    val geo = GeoPeru.de(LocalContext.current)
    val lugares = remember(departamentos, geo) { ubicar(departamentos, geo) }
    val medidor = rememberTextMeasurer(cacheSize = 64)
    val alcance = rememberCoroutineScope()
    val densidad = LocalDensity.current

    // Lo que se encuadra: los departamentos con pines o, si no hay, los registrados.
    val depsObjetivo = pines.map { it.departamento }.distinct().sorted()
        .ifEmpty { lugares.values.filter { it.registro.estado != Estado.ELIMINADO }.map { it.registro.codigo } }
    val cajaPeru = geo.first { it.tipo == 'C' }.caja
    val cajaObjetivo = depsObjetivo.mapNotNull { lugares[it] }
        .map { it.figura?.caja ?: Rect(it.ancla, .6f) }
        .reduceOrNull { a, b -> a.unir(b) } ?: cajaPeru

    BoxWithConstraints(modifier.clipToBounds().background(Oceano)) {
        val ancho = constraints.maxWidth.toFloat()
        val alto = constraints.maxHeight.toFloat()
        val camara = remember { Camara() }
        val vista = Rect(0f, alto * margenSuperior, ancho, alto - with(densidad) { margenInferior.toPx() })
        // si la hoja inferior sube o baja, lo que estaba al centro de la parte visible sigue al centro
        if (camara.escala != 0f && vista != camara.vista) camara.desp += vista.center - camara.vista.center
        camara.vista = vista
        camara.limites = cajaPeru.inflate(1.5f)
        camara.minEscala = .85f * min(camara.vista.width / cajaPeru.width, camara.vista.height / cajaPeru.height)
        camara.maxEscala = camara.vista.width / .25f   // hasta ver Callao entero
        val topeEncuadre = camara.vista.width / 2.5f     // un solo departamento no se acerca de más
        if (camara.escala == 0f) camara.encuadre(cajaObjetivo, topeEncuadre).let { (s, d) -> camara.fijar(s, d) }

        // si cambian los pines (búsqueda, otro departamento elegido), el mapa vuela hacia ellos
        val clave = depsObjetivo.joinToString()
        var claveAnterior by remember { mutableStateOf(clave) }
        LaunchedEffect(clave) {
            if (clave != claveAnterior) {
                claveAnterior = clave
                camara.animarA(camara.encuadre(cajaObjetivo, topeEncuadre))
            }
        }

        val porDepartamento = todas.groupBy { it.departamento }.mapValues { (_, l) -> l.map { it.codigo }.sorted() }
        val ubicados = pines.map { p ->
            val lista = porDepartamento[p.departamento].orEmpty()
            val indice = lista.indexOf(p.codigo).let { if (it < 0) lista.size else it }
            val (dx, dy) = desplazamientoPin(indice)
            PinUbicado(p, lugares[p.departamento]?.ancla ?: mundo(-80.6f, -18f), dx, dy)
        }

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTransformGestures { c, pan, zoom, _ -> camara.transformar(c, pan, zoom) } }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { p -> alcance.launch { camara.acercar(p, 2f) } }) }
                .semantics { contentDescription = "Mapa del Perú" },
        ) {
            val s = camara.escala
            val px = 1.dp.toPx() / s   // 1 dp en unidades de mundo
            val registrados = lugares.values.filter { it.registro.estado != Estado.ELIMINADO && it.figura != null }
                .associateBy { it.figura!! }

            withTransform({ translate(camara.desp.x, camara.desp.y); scale(s, s, Offset.Zero) }) {
                geo.filter { it.tipo == 'P' }.forEach {
                    drawPath(it.path, TierraVecina)
                    drawPath(it.path, TierraVecina, style = Stroke(.08f))   // tapa rendijas con la frontera del INEI
                }
                geo.filter { it.tipo == 'P' }.forEach { drawPath(it.path, BordeVecino, style = Stroke(px)) }
                geo.filter { it.tipo == 'D' }.forEach { f ->
                    val fondo = when (registrados[f]?.registro?.estado) {
                        Estado.ACTIVO -> DepActivo
                        Estado.INACTIVO -> DepInactivo
                        else -> TierraPeru
                    }
                    drawPath(f.path, fondo)
                }
                geo.filter { it.tipo == 'L' }.forEach { drawPath(it.path, Lago) }
                geo.filter { it.tipo == 'D' }.forEach {
                    drawPath(it.path, Curva, style = Stroke(px, join = StrokeJoin.Round))
                }
                geo.filter { it.tipo == 'C' }.forEach {
                    drawPath(it.path, ContornoPeru, style = Stroke(1.6f * px, join = StrokeJoin.Round))
                }
            }
            if (oscurecido) drawRect(Color.Black.copy(alpha = .06f))

            // Nombres, de más a menos importante; cada uno busca un hueco libre.
            val dp = 1.dp.toPx()
            val ocupado = ubicados.map { Rect(camara.aPantalla(it.ancla) + Offset(it.dx * dp, it.dy * dp), 15 * dp) }.toMutableList()
            val pieBotones = size.height - (margenInferior.value + 8) * dp
            ocupado += Rect(10 * dp, pieBotones - 36 * dp, 172 * dp, pieBotones)   // los botones de zoom
            val grupos = ubicados.groupBy { it.publicidad.departamento }
            // 1) departamentos de la tabla (gris si están inactivos): debajo, encima o al lado de sus pines
            lugares.values.filter { it.registro.estado != Estado.ELIMINADO }
                .sortedBy { if (it.registro.codigo in grupos) 0 else 1 }
                .forEach { l ->
                    val a = camara.aPantalla(l.ancla)
                    val g = grupos[l.registro.codigo]
                    val izq = (g?.minOf { it.dx } ?: 0) * dp
                    val der = (g?.maxOf { it.dx } ?: 0) * dp
                    val arriba = (g?.minOf { it.dy } ?: 0) * dp
                    val sep = if (g == null) 0f else 18 * dp
                    val texto = if (l.figura == null) "${l.registro.nombre} (sin ubicar)" else l.registro.nombre
                    val estilo = lugar(14, if (l.registro.estado == Estado.ACTIVO) Tinta else Gris, 600)
                    nombre(medidor, texto, estilo, ocupado) { w, h ->
                        listOfNotNull(
                            if (g == null) a else null,
                            Offset(a.x, a.y + sep + h / 2),
                            Offset(a.x, a.y + arriba - sep - h / 2),
                            Offset(a.x + der + sep + w / 2, a.y),
                            Offset(a.x + izq - sep - w / 2, a.y),
                        )
                    }
                }
            // 2) países vecinos, mar, lagos y el resto de departamentos (estos solo si caben dentro)
            geo.filter { it.tipo == 'P' && it.nombre.isNotEmpty() }.forEach { f ->
                nombre(medidor, f.nombre.uppercase(), sans(11, 600, NombreSecundario).copy(letterSpacing = 2.sp), ocupado) { _, _ ->
                    listOf(camara.aPantalla(f.etiqueta))
                }
            }
            nombre(medidor, "Océano Pacífico", lugar(14, NombreAgua), ocupado) { _, _ ->
                listOf(camara.aPantalla(mundo(-77.0f, -16.3f)), camara.aPantalla(mundo(-84.5f, -9.0f)))
            }
            geo.filter { it.tipo == 'L' }.forEach { f ->
                nombre(medidor, f.nombre, lugar(11, NombreAgua), ocupado, f.caja.width * s) { _, _ -> listOf(camara.aPantalla(f.etiqueta)) }
            }
            geo.filter { it.tipo == 'D' && it !in registrados }.forEach { f ->
                nombre(medidor, f.nombre, lugar(11, NombreSecundario), ocupado, f.caja.width * s * .9f) { _, _ ->
                    listOf(camara.aPantalla(f.etiqueta))
                }
            }
        }

        // pines (componentes aparte para que se puedan tocar)
        ubicados.forEach { u ->
            val p = u.publicidad
            PinMapa(
                numeroDe(p.codigo), p.estado,
                Modifier
                    .offset {
                        val c = camara.aPantalla(u.ancla)
                        IntOffset((c.x + (u.dx - 15).dp.toPx()).roundToInt(), (c.y + (u.dy - 15).dp.toPx()).roundToInt())
                    }
                    .semantics { contentDescription = "Pin ${p.codigo}" }
                    .then(if (alTocarPin != null) Modifier.clip(CircleShape).clickable { alTocarPin(p) } else Modifier),
            )
        }

        // controles: acercar, alejar, ver todo el Perú y volver a las publicidades
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = margenInferior + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val centroVista = { camara.vista.center }
            BotonMapa("Acercar el mapa", { alcance.launch { camara.acercar(centroVista(), 2f) } }) { Text("+", style = sans(20, 700)) }
            BotonMapa("Alejar el mapa", { alcance.launch { camara.acercar(centroVista(), .5f) } }) { Text("−", style = sans(20, 700)) }
            BotonMapa("Ver todo el Perú", { alcance.launch { camara.animarA(camara.encuadre(cajaPeru)) } }) {
                Text("Perú", style = sans(11, 700))
            }
            BotonMapa("Encuadrar las publicidades", { alcance.launch { camara.animarA(camara.encuadre(cajaObjetivo, topeEncuadre)) } }) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = Pin)
            }
        }
    }
}

@Composable
private fun BotonMapa(descripcion: String, alTocar: () -> Unit, contenido: @Composable () -> Unit) {
    Box(
        Modifier.size(36.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White)
            .clickable(onClick = alTocar).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) { contenido() }
}
