package com.unsa.publicidad.prototipo.datos

import java.text.Collator
import java.util.Locale

/** Las cuatro tablas del módulo Publicidad. */
enum class Tabla(
    val sql: String,          // nombre de la tabla en SQLite
    val prefijo: String,      // prefijo del código (P001, C001, D01, Z01)
    val digitos: Int,
    val singular: String,
    val plural: String,
    val pregunta: String,     // título de la consulta de selección
    val femenino: Boolean,
) {
    PUBLICIDAD("publicidad", "P", 3, "publicidad", "publicidades", "", true),
    CLIENTE("cliente", "C", 3, "cliente", "clientes", "¿Para qué cliente es?", false),
    DEPARTAMENTO("departamento", "D", 2, "departamento", "departamentos", "¿En qué departamento está?", false),
    ZONA("zona", "Z", 2, "zona", "zonas", "¿En qué zona está?", true);

    val pluralTitulo get() = plural.replaceFirstChar { it.uppercase() }
    val singularTitulo get() = singular.replaceFirstChar { it.uppercase() }
    /** "un cliente" / "una zona" */
    val articuloUn get() = if (femenino) "una" else "un"
    /** "Nuevo cliente" / "Nueva zona" */
    val nuevoTitulo get() = (if (femenino) "Nueva " else "Nuevo ") + singular

    companion object {
        val referenciales = listOf(CLIENTE, DEPARTAMENTO, ZONA)
    }
}

/** Estado de registro: A = activo, I = inactivo, * = eliminado. */
object Estado {
    const val ACTIVO = 'A'
    const val INACTIVO = 'I'
    const val ELIMINADO = '*'

    fun texto(e: Char) = when (e) {
        ACTIVO -> "Activo"
        INACTIVO -> "Inactivo"
        else -> "Eliminado"
    }
}

/** Fila de una tabla referencial (Clientes, Departamento, Zonas). */
data class Registro(val codigo: String, val nombre: String, val estado: Char)

/** Fila de la tabla maestra. Cliente, departamento y zona guardan el CÓDIGO del referencial. */
data class Publicidad(
    val codigo: String,
    val nombre: String,
    val cliente: String,
    val departamento: String,
    val zona: String,
    val calle: String,
    val estado: Char,
)

enum class Orden(val texto: String) { CODIGO("Código"), NOMBRE("Nombre") }

/** Comparación alfabética en español (Á, É, Ñ ordenan bien; sin distinguir mayúsculas). */
val alfabetico: Collator = Collator.getInstance(Locale.forLanguageTag("es-PE")).apply { strength = Collator.PRIMARY }

/** Número que va dentro del pin: P007 -> "7". */
fun numeroDe(codigo: String) = codigo.filter { it.isDigit() }.toIntOrNull()?.toString() ?: codigo
