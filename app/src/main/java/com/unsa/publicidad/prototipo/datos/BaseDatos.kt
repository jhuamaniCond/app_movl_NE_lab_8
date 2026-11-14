package com.unsa.publicidad.prototipo.datos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local SQLite con las 4 tablas relacionadas del módulo:
 *
 *   cliente(codigo, nombre, estado)
 *   departamento(codigo, nombre, estado)
 *   zona(codigo, nombre, estado)
 *   publicidad(codigo, nombre, cliente→cliente, departamento→departamento, zona→zona, calle, estado)
 *
 * Todas las tablas tienen el dato Estado de Registro (A, I, *). La eliminación es lógica:
 * el registro queda con '*' y ya no se puede modificar.
 */
class BaseDatos(context: Context) : SQLiteOpenHelper(context, "publicidad.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Tabla.referenciales.forEach { t ->
            db.execSQL(
                """CREATE TABLE ${t.sql} (
                    codigo TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'A' CHECK (estado IN ('A','I','*'))
                )"""
            )
        }
        db.execSQL(
            """CREATE TABLE publicidad (
                codigo       TEXT PRIMARY KEY,
                nombre       TEXT NOT NULL,
                cliente      TEXT NOT NULL REFERENCES cliente(codigo),
                departamento TEXT NOT NULL REFERENCES departamento(codigo),
                zona         TEXT NOT NULL REFERENCES zona(codigo),
                calle        TEXT NOT NULL,
                estado       TEXT NOT NULL DEFAULT 'A' CHECK (estado IN ('A','I','*'))
            )"""
        )
        sembrar(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) = Unit

    /** Datos iniciales para que el prototipo no arranque vacío. */
    private fun sembrar(db: SQLiteDatabase) {
        fun ref(t: Tabla, c: String, n: String, e: Char = 'A') =
            db.insert(t.sql, null, ContentValues().apply { put("codigo", c); put("nombre", n); put("estado", e.toString()) })

        ref(Tabla.CLIENTE, "C001", "Lácteos del Sur S.A.")
        ref(Tabla.CLIENTE, "C002", "Ferretería El Misti")
        ref(Tabla.CLIENTE, "C003", "Inversiones Chachani")
        ref(Tabla.CLIENTE, "C004", "Textil Colca E.I.R.L.", 'I')
        ref(Tabla.CLIENTE, "C005", "Panificadora Yanahuara")

        ref(Tabla.DEPARTAMENTO, "D01", "Arequipa")
        ref(Tabla.DEPARTAMENTO, "D02", "Tacna")
        ref(Tabla.DEPARTAMENTO, "D03", "Moquegua")
        ref(Tabla.DEPARTAMENTO, "D04", "Puno")
        ref(Tabla.DEPARTAMENTO, "D05", "Cusco")
        ref(Tabla.DEPARTAMENTO, "D06", "Apurímac", 'I')
        ref(Tabla.DEPARTAMENTO, "D07", "Ica")

        ref(Tabla.ZONA, "Z01", "Centro")
        ref(Tabla.ZONA, "Z02", "Rural")
        ref(Tabla.ZONA, "Z03", "Carretera")
        ref(Tabla.ZONA, "Z04", "Residencial")
        ref(Tabla.ZONA, "Z05", "Industrial", 'I')

        listOf(
            Publicidad("P001", "Panel Av. Ejército", "C001", "D01", "Z01", "Av. Ejército 710", 'A'),
            Publicidad("P002", "Valla Variante de Uchumayo", "C003", "D01", "Z03", "Km 8 Variante", 'A'),
            Publicidad("P003", "Paleta Plaza de Armas", "C002", "D02", "Z01", "Calle San Martín 120", 'I'),
            Publicidad("P004", "Banner Feria Puno", "C005", "D04", "Z01", "Jr. Lima 455", 'A'),
            Publicidad("P005", "Valla Panamericana Sur", "C001", "D03", "Z03", "Km 1145 Panam. Sur", 'A'),
            Publicidad("P006", "Mural Mercado Rural", "C004", "D02", "Z02", "Av. Collpa s/n", '*'),
        ).forEach { db.insert("publicidad", null, valores(it)) }
    }

    // ---------- Tablas referenciales ----------

    /** Todos los registros de la tabla (activos, inactivos y eliminados) en el orden pedido. */
    fun listar(t: Tabla, orden: Orden = Orden.CODIGO): List<Registro> {
        val filas = readableDatabase.rawQuery("SELECT codigo, nombre, estado FROM ${t.sql}", null).use { c ->
            buildList { while (c.moveToNext()) add(Registro(c.getString(0), c.getString(1), c.getString(2)[0])) }
        }
        return when (orden) {
            Orden.CODIGO -> filas.sortedBy { it.codigo }
            Orden.NOMBRE -> filas.sortedWith { a, b -> alfabetico.compare(a.nombre, b.nombre) }
        }
    }

    /** Lo que muestra la consulta de selección: solo activos, en orden alfabético del nombre. */
    fun activos(t: Tabla) = listar(t, Orden.NOMBRE).filter { it.estado == Estado.ACTIVO }

    fun buscar(t: Tabla, codigo: String): Registro? =
        readableDatabase.rawQuery("SELECT codigo, nombre, estado FROM ${t.sql} WHERE codigo = ?", arrayOf(codigo)).use { c ->
            if (c.moveToFirst()) Registro(c.getString(0), c.getString(1), c.getString(2)[0]) else null
        }

    fun nombreDe(t: Tabla, codigo: String) = buscar(t, codigo)?.nombre ?: "—"

    /** Adición: el estado se coloca por defecto en 'A'. */
    fun insertar(t: Tabla, codigo: String, nombre: String) {
        writableDatabase.insertOrThrow(t.sql, null, ContentValues().apply {
            put("codigo", codigo); put("nombre", nombre.trim()); put("estado", Estado.ACTIVO.toString())
        })
    }

    fun modificar(t: Tabla, codigo: String, nombre: String) {
        writableDatabase.update(t.sql, ContentValues().apply { put("nombre", nombre.trim()) }, "codigo = ?", arrayOf(codigo))
    }

    // ---------- Tabla maestra ----------

    fun publicidades(orden: Orden = Orden.CODIGO): List<Publicidad> {
        val filas = readableDatabase.rawQuery(
            "SELECT codigo, nombre, cliente, departamento, zona, calle, estado FROM publicidad", null
        ).use { c -> buildList { while (c.moveToNext()) add(leer(c)) } }
        return when (orden) {
            Orden.CODIGO -> filas.sortedBy { it.codigo }
            Orden.NOMBRE -> filas.sortedWith { a, b -> alfabetico.compare(a.nombre, b.nombre) }
        }
    }

    fun publicidad(codigo: String): Publicidad? = readableDatabase.rawQuery(
        "SELECT codigo, nombre, cliente, departamento, zona, calle, estado FROM publicidad WHERE codigo = ?", arrayOf(codigo)
    ).use { c -> if (c.moveToFirst()) leer(c) else null }

    fun insertar(p: Publicidad) {
        writableDatabase.insertOrThrow("publicidad", null, valores(p.copy(estado = Estado.ACTIVO)))
    }

    fun modificar(p: Publicidad) {
        writableDatabase.update("publicidad", valores(p).apply { remove("estado") }, "codigo = ?", arrayOf(p.codigo))
    }

    /** Publicidades (no eliminadas) que usan un registro referencial. */
    fun publicidadesQueUsan(t: Tabla, codigo: String): List<Publicidad> =
        if (t == Tabla.PUBLICIDAD) emptyList()
        else publicidades().filter { it.estado != Estado.ELIMINADO && campo(it, t) == codigo }

    // ---------- Comunes ----------

    /** Inactivar (I), Reactivar (A) o Eliminar (*). */
    fun cambiarEstado(t: Tabla, codigo: String, estado: Char) {
        writableDatabase.update(t.sql, ContentValues().apply { put("estado", estado.toString()) }, "codigo = ?", arrayOf(codigo))
    }

    /** Siguiente código libre: P007, C006, D08, Z06... */
    fun siguienteCodigo(t: Tabla): String {
        val max = readableDatabase.rawQuery("SELECT codigo FROM ${t.sql}", null).use { c ->
            var m = 0
            while (c.moveToNext()) m = maxOf(m, c.getString(0).filter { it.isDigit() }.toIntOrNull() ?: 0)
            m
        }
        return t.prefijo + (max + 1).toString().padStart(t.digitos, '0')
    }

    private fun leer(c: Cursor) = Publicidad(
        c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6)[0]
    )

    private fun valores(p: Publicidad) = ContentValues().apply {
        put("codigo", p.codigo); put("nombre", p.nombre.trim()); put("cliente", p.cliente)
        put("departamento", p.departamento); put("zona", p.zona); put("calle", p.calle.trim()); put("estado", p.estado.toString())
    }

    companion object {
        fun campo(p: Publicidad, t: Tabla) = when (t) {
            Tabla.CLIENTE -> p.cliente
            Tabla.DEPARTAMENTO -> p.departamento
            Tabla.ZONA -> p.zona
            Tabla.PUBLICIDAD -> p.codigo
        }
    }
}
