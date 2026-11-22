# Prototipo – Módulo Publicidad (Alternativa 2 "Cartografía")

App Android nativa (Kotlin + Jetpack Compose + SQLite) para el Lab 08 de Negocios Electrónicos – M-Business.
Implementa el layout ganador (alternativa 2), ajustado con la retroalimentación del docente:

- Todo el mantenimiento de cada tabla se hace en **una sola pantalla**: arriba las **tarjetas** de la tabla
  (no una tabla de filas y columnas) y abajo la **ficha**, una hoja que se desliza con el dedo.
- La ficha muestra el **modo de trabajo** (consulta, adición o modificación), los datos del registro elegido
  y los botones de las funciones en dos filas: **Modificar · Inactivar · Reactivar · Eliminar** (sobre el
  registro elegido) y **Adicionar · Cancelar · Guardar**. Siempre están a la vista y se encienden o se apagan
  según el modo y el estado del registro (por ejemplo, con un registro `I` se enciende Reactivar y se apaga
  Inactivar). **Buscar** y **Salir** no se repiten en la ficha: son el buscador y la flecha ← de arriba.
- Se toca una tarjeta (o un pin del mapa) para elegir el registro; Adicionar y Modificar suben la ficha hasta
  más o menos la mitad de la pantalla para escribir los datos, y con el teclado abierto sube lo necesario.
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
  de su estado; al tocarlo se elige esa publicidad (queda resaltada) y se carga en la ficha de abajo.
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

Los **códigos son solo números**, sin letras: `001`, `002`… en publicidad y clientes, y `01`, `02`… en
departamentos y zonas. Una base creada por una versión anterior (con códigos como `C001` o `Z06`) se
convierte sola al abrir la app (versión 2 de la base): se quita la letra en las 4 tablas y en las
referencias de la maestra, sin perder datos.

## Requisitos del enunciado y dónde se cumplen

| Requisito | Implementación |
|---|---|
| Adición | **Adicionar** → la ficha pasa a *modo adición* y sube; el código se genera solo y es **solo numérico** (007 en publicidad y clientes, 06 en departamentos y zonas) y el estado queda en `A` |
| Modificación | elegir la tarjeta → **Modificar** → *modo modificación* en la ficha, con la tarjeta marcada "modificando" |
| Eliminación | **Eliminar** (con confirmación) → estado `*`; la tarjeta queda tachada y de solo lectura. Si un referencial está en uso por alguna publicidad, no se deja eliminar |
| Inactivar / Reactivar | **Inactivar** (con confirmación) → `I` y **Reactivar** → `A`; cada uno solo se enciende cuando corresponde |
| Buscar | buscador en la parte superior (Clientes por nombre; en la maestra también cliente, calle, departamento o zona) |
| Cancelar | en adición / modificación descarta los cambios; en consulta suelta el registro elegido. "Cancelar" también en la consulta de selección |
| Guardar | **Guardar** con validación (campos obligatorios, nombres repetidos); solo se enciende en adición / modificación |
| Salir | flecha ← del buscador (vuelve al inicio); botón salir en el inicio |
| Orden por defecto | Clientes por **nombre**; Publicidad, Departamento y Zona por **código**; interruptor Código / Nombre (opcional) |
| Consulta de tabla referencial | en la maestra, al tocar Cliente / Departamento / Zona se abre una hoja con **solo activos en orden alfabético**; "Cargar …" carga el código, "Cancelar" vuelve sin cambios |
| Opcional: otro orden | interruptor Código / Nombre en todas las listas |
| Opcional: búsqueda y selección | buscar → tocar la tarjeta → Modificar / Inactivar / Reactivar / Eliminar en la ficha |

## Estructura

```
app/src/main/java/com/unsa/publicidad/prototipo/
  MainActivity.kt          navegación (Inicio → Lista) y avisos
  datos/Modelos.kt         Tabla, Estado, Registro, Publicidad, Orden
  datos/BaseDatos.kt       SQLiteOpenHelper: esquema, datos iniciales y operaciones
  ui/Tema.kt               colores y tipografías (Source Serif cursiva para lugares, Source Sans)
  ui/Mapa.kt               mapa del Perú con desplazamiento y zoom, nombres de departamento y pines
  ui/Componentes.kt        buscador, selectores de orden y de vista, letra de estado A/I/*, tarjeta,
                           botón de función, campos y hoja de selección
  ui/HojaDeslizable.kt     hoja inferior que se arrastra (abajo / mitad / arriba) y sube con el teclado
  ui/PantallaInicio.kt     acceso a las 4 tablas
  ui/PantallaLista.kt      mantenimiento de una tabla: tarjetas (o mapa en la maestra) + ficha con modo,
                           datos y botones de las funciones
res/raw/mapa_peru.txt      coordenadas de departamentos, países vecinos y lagos (lon,lat)
```

## Compilar y ejecutar

- Abrir la carpeta `prototipo_code` en Android Studio (2026.1 o superior) y ejecutar en un
  emulador o celular con Android 7.0 (API 24) o más.
- Por consola: `gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
  (usar el JDK de Android Studio: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`).
- La base se crea en el primer arranque con datos de ejemplo. Para volver a empezar:
  `adb shell pm clear com.unsa.publicidad.prototipo`.
