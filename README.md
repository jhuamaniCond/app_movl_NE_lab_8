# Prototipo – Módulo Publicidad (Alternativa 2 "Cartografía")

App Android nativa (Kotlin + Jetpack Compose + SQLite) para el Lab 08 de Negocios Electrónicos – M-Business.
Implementa el layout ganador (alternativa 2), ajustado con la retroalimentación del docente:

- Todo el mantenimiento de cada tabla se hace en **una sola pantalla**: Adicionar y Modificar abren el formulario
  en la **mitad inferior**, con la lista a la vista arriba (ya no hay pantalla de detalle aparte).
- Cada registro es una **tarjeta con sus botones dentro**: Modificar, Inactivar / Reactivar y Eliminar.
- El estado se indica con su letra: **A** (activo, verde), **I** (inactivo, ámbar), **\*** (eliminado, gris).
- **Clientes** abre ordenado por **Nombre** (es el dato con el que más se trabaja); las demás tablas, por código.
- El **Maestro de Publicidad** se muestra como las otras tablas; el mapa del Perú es un botón de
  **cambio de vista** (Lista | Mapa).

## Mapa

- Los 25 departamentos con sus límites oficiales, los países vecinos y los lagos Titicaca y Junín,
  dibujados con Canvas desde `res/raw/mapa_peru.txt`: funciona sin internet y sin API de pago.
- Se desplaza con un dedo y se acerca con dos dedos o con doble toque. Los botones **+ / −**
  acercan y alejan, **Perú** muestra todo el país y el botón del pin vuelve a encuadrar las publicidades.
- Se abre con el botón **Mapa** del Maestro de Publicidad. Cada pin lleva el número de la publicidad y la letra
  de su estado; al tocarlo aparece su tarjeta con los botones Modificar / Inactivar / Eliminar.
- La búsqueda también filtra los pines.
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
| Adición | botón **Adicionar** → formulario en la mitad inferior de la misma pantalla; el código se genera solo (P007, C006, D08, Z06) y el estado queda en `A` |
| Modificación | botón **Modificar** dentro de la tarjeta → mismo formulario inferior, con la tarjeta resaltada |
| Eliminación | botón **Eliminar** de la tarjeta (con confirmación) → estado `*`; queda de solo lectura. Si un referencial está en uso por alguna publicidad, no se deja eliminar |
| Inactivar / Reactivar | botones **Inactivar** (con confirmación) → `I` y **Reactivar** → `A`, dentro de la tarjeta |
| Buscar | buscador en la parte superior de cada lista (Clientes por nombre; en la maestra también calle o cliente) |
| Cancelar | **Cancelar** / ✕ en el formulario inferior (descarta cambios) y "Cancelar" en la consulta de selección |
| Guardar | botón **Guardar** con validación (campos obligatorios, nombres repetidos) |
| Salir | flecha ← en las listas; botón salir en el inicio |
| Orden por defecto | Clientes por **nombre**; Publicidad, Departamento y Zona por **código**; interruptor Código / Nombre (opcional) |
| Consulta de tabla referencial | en la maestra, al tocar Cliente / Departamento / Zona se abre una hoja con **solo activos en orden alfabético**; "Cargar …" carga el código, "Cancelar" vuelve sin cambios |
| Opcional: otro orden | interruptor Código / Nombre en todas las listas |
| Opcional: búsqueda y selección | buscar → tocar el registro → modificar / inactivar / reactivar / eliminar |

## Estructura

```
app/src/main/java/com/unsa/publicidad/prototipo/
  MainActivity.kt          navegación (Inicio → Lista) y avisos
  datos/Modelos.kt         Tabla, Estado, Registro, Publicidad, Orden
  datos/BaseDatos.kt       SQLiteOpenHelper: esquema, datos iniciales y operaciones
  ui/Tema.kt               colores y tipografías (Source Serif cursiva para lugares, Source Sans)
  ui/Mapa.kt               mapa del Perú con desplazamiento y zoom, nombres de departamento y pines
  ui/Componentes.kt        buscador, selectores de orden y de vista, letra de estado A/I/*, tarjeta con botones,
                           campos y hoja de selección
  ui/PantallaInicio.kt     acceso a las 4 tablas
  ui/PantallaLista.kt      mantenimiento de una tabla: tarjetas (o mapa en la maestra) + formulario inferior
  ui/PanelFormulario.kt    formulario de Adición / Modificación en la mitad inferior de la pantalla
res/raw/mapa_peru.txt      coordenadas de departamentos, países vecinos y lagos (lon,lat)
```

## Compilar y ejecutar

- Abrir la carpeta `prototipo_code` en Android Studio (2026.1 o superior) y ejecutar en un
  emulador o celular con Android 7.0 (API 24) o más.
- Por consola: `gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
  (usar el JDK de Android Studio: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`).
- La base se crea en el primer arranque con datos de ejemplo. Para volver a empezar:
  `adb shell pm clear com.unsa.publicidad.prototipo`.
