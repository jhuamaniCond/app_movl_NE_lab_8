# Prototipo – Módulo Publicidad (Alternativa 2 "Cartografía")

App Android nativa (Kotlin + Jetpack Compose + SQLite) para el Lab 08 de Negocios Electrónicos – M-Business.
Implementa el layout ganador (alternativa 2): en el Maestro de Publicidad la lista vive bajo un mapa real del Perú y
pines numerados; Inicio y las tablas referenciales se muestran sin mapa, a pantalla completa.
El estado se lee en el pin (lleno = activo, hueco = inactivo, tachado = eliminado).

## Mapa

- Los 25 departamentos con sus límites oficiales, los países vecinos y los lagos Titicaca y Junín,
  dibujados con Canvas desde `res/raw/mapa_peru.txt`: funciona sin internet y sin API de pago.
- Se desplaza con un dedo y se acerca con dos dedos o con doble toque. Los botones **+ / −**
  acercan y alejan, **Perú** muestra todo el país y el botón del pin vuelve a encuadrar las publicidades.
- La hoja blanca de abajo (lista del Maestro de Publicidad) se arrastra desde su cabecera: abajo deja ver casi todo
  el mapa, arriba deja ver casi toda la lista; tocar el asa alterna entre abajo y la posición inicial.
- Cada departamento de la tabla se ubica por su nombre ("Apurímac", "Lima", "Cusco"...).
  Si el nombre no es de un departamento del Perú, aparece en el mar como "(sin ubicar)".
- Fuentes de datos: límites departamentales del INEI vía
  [juaneladio/peru-geojson](https://github.com/juaneladio/peru-geojson) (MPL-2.0); países vecinos
  y lago Titicaca de [Natural Earth](https://www.naturalearthdata.com/) 1:50m (dominio público).

## Tablas (SQLite, `datos/BaseDatos.kt`)

| Tabla | Campos | Tipo |
|---|---|---|
| `publicidad` | codigo, nombre, cliente → cliente, departamento → departamento, zona → zona, calle, estado | maestra |
| `cliente` | codigo, nombre, estado | referencial |
| `departamento` | codigo, nombre, estado | referencial |
| `zona` | codigo, nombre, estado | referencial |

Todas tienen **Estado de Registro** (`A` activo, `I` inactivo, `*` eliminado) con `CHECK`,
y la maestra tiene claves foráneas reales (`PRAGMA foreign_keys = ON`).

## Requisitos del enunciado y dónde se cumplen

| Requisito | Implementación |
|---|---|
| Adición | botón **Adicionar**; el código se genera solo (P007, C006, D08, Z06) y el estado queda en `A` |
| Modificación | botón **Modificar** en el detalle → modo edición |
| Eliminación | botón **Eliminar** (con confirmación) → estado `*`; queda de solo lectura. Si un referencial está en uso por alguna publicidad, no se deja eliminar |
| Inactivar / Reactivar | botones **Inactivar** (con confirmación) → `I` y **Reactivar** → `A` |
| Buscar | buscador en la parte superior de cada lista (por nombre o código; en la maestra también calle o cliente) |
| Cancelar | ✕ en modo edición (descarta cambios) y "Cancelar" en la consulta de selección |
| Guardar | botón **Guardar** con validación (campos obligatorios, nombres repetidos) |
| Salir | flecha ← en listas y detalle; botón salir en el inicio |
| Orden por defecto | por **código**; interruptor Código / Nombre (opcional) |
| Consulta de tabla referencial | en la maestra, al tocar Cliente / Departamento / Zona se abre una hoja con **solo activos en orden alfabético**; "Cargar …" carga el código, "Cancelar" vuelve sin cambios |
| Opcional: otro orden | interruptor Código / Nombre en todas las listas |
| Opcional: búsqueda y selección | buscar → tocar el registro → modificar / inactivar / reactivar / eliminar |

## Estructura

```
app/src/main/java/com/unsa/publicidad/prototipo/
  MainActivity.kt          navegación (Inicio → Lista → Detalle) y avisos
  datos/Modelos.kt         Tabla, Estado, Registro, Publicidad, Orden
  datos/BaseDatos.kt       SQLiteOpenHelper: esquema, datos iniciales y operaciones
  ui/Tema.kt               colores y tipografías (Source Serif cursiva para lugares, Source Sans)
  ui/Mapa.kt               mapa del Perú con desplazamiento y zoom, nombres de departamento y pines
res/raw/mapa_peru.txt      coordenadas de departamentos, países vecinos y lagos (lon,lat)
  ui/HojaMapa.kt           mapa a pantalla completa con la hoja inferior que se arrastra
  ui/Componentes.kt        buscador, selector de orden, leyenda, campos, hoja de selección
  ui/PantallaInicio.kt     acceso a las 4 tablas (sin mapa)
  ui/PantallaLista.kt      lista de una tabla (la maestra bajo el mapa; las referenciales sin mapa)
  ui/PantallaDetalle.kt    detalle / adición / modificación y acciones de estado (mini mapa solo en la maestra)
```

## Compilar y ejecutar

- Abrir la carpeta `prototipo_code` en Android Studio (2026.1 o superior) y ejecutar en un
  emulador o celular con Android 7.0 (API 24) o más.
- Por consola: `gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
  (usar el JDK de Android Studio: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`).
- La base se crea en el primer arranque con datos de ejemplo. Para volver a empezar:
  `adb shell pm clear com.unsa.publicidad.prototipo`.
