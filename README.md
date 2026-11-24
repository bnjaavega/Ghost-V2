# Ghost

Editor de código para Android especializado en **Python**, con intérprete real (CPython vía Chaquopy), consola interactiva con `input()`, editor Sora, modo oscuro único y funcionamiento 100 % local (sin cuentas, sin nube).

> **Estado de verificación (léelo primero).** Este proyecto se generó sin Android SDK ni acceso de red para Gradle, así que **no se pudo compilar el APK durante su creación**. Sí se comprobó: la lógica Python del ejecutor con CPython real (entrada interactiva, salida, errores, interrupción), la salida esperada de los 12 ejemplos de Práctica, la validez de los JSON/XML y el equilibrio sintáctico del Kotlin. La **primera compilación real será la de GitHub Actions**; si falla, copia el error del log y se corrige (lo más probable son pequeños desajustes de la API de Sora Editor; ver «Puntos a vigilar»).

## Novedades de la V2

- **Botón «+» contextual**: en Inicio y Archivos ofrece *Nuevo proyecto* y *Abrir .py*; dentro de un proyecto, *Nueva carpeta* y *Nuevo archivo*; en Práctica no aparece. Se quitaron los botones duplicados de la parte superior de esas pantallas.
- **Barra inferior**: Inicio · Archivos · Práctica, visible también dentro de un proyecto y en Práctica.
- **Archivos**: todos los archivos de todos los proyectos agrupados por proyecto; al tocar uno se abre en el editor.
- **Menú ⋮**: *Eliminar proyectos* (selección múltiple con confirmación) y *Papelera*. Dentro de un proyecto, el mismo menú permite eliminar archivos y carpetas.
- **Papelera**: los elementos eliminados se conservan (carpeta oculta `.papelera`) con su ruta original; se pueden restaurar (recreando carpetas/proyecto; si ya existe algo igual, las carpetas se fusionan y los archivos en conflicto se restauran como «nombre (restaurado)»), eliminar definitivamente o vaciar la papelera.
- **Editor con pestañas**: abre varios archivos a la vez (máx. 10), cada pestaña conserva texto, deshacer/rehacer y posición; indicador ● de cambios sin guardar y botón × para cerrar. El «+» de la fila de pestañas abre otro archivo de cualquier proyecto.
- **Guardado**: con el guardado automático desactivado aparece un botón **Guardar** siempre visible; se avisa al cerrar una pestaña o salir con cambios pendientes, y si la app pasa a segundo plano se crea un *borrador de seguridad* que se recupera al reabrir el archivo (el archivo original no se toca hasta que guardas).
- **Práctica**: niveles Básico / Intermedio / Avanzado (48 ejercicios en `assets/practice/practice.json`, con la salida generada ejecutando cada ejemplo), solo botón *Copiar código*, carga en caché y sin recomposiciones innecesarias.
- **Rendimiento**: el autoguardado ya no recarga las listas en cada guardado, debounce adaptativo en archivos grandes, consola con salida agrupada y límite de memoria menor, ajustes del editor solo cuando cambian, un solo ejercicio abierto a la vez en Práctica.

## Funciones (base V1)

- Proyectos y archivos `.py`: crear, renombrar, eliminar (con confirmación), carpetas, organización por proyecto.
- Editor real (Sora Editor): números de línea, resaltado de sintaxis Python (palabras reservadas, variables, funciones, cadenas, comentarios, números), fuente monoespaciada, tamaño ajustable, scroll vertical y horizontal, autocompletado, indentación automática, copiar/cortar/pegar/seleccionar, deshacer/rehacer, indicador de archivo y estado de guardado, fila de símbolos sobre el teclado.
- Guardado automático con *debounce* (800 ms), guardado manual, guardado al pasar la app a segundo plano y aviso de cambios sin guardar.
- Ejecución real de Python con consola integrada (mostrar/ocultar), `print()`, errores con traceback, `input()` interactivo y botón de detener.
- Archivos externos con el selector nativo (SAF), empezando en Descargas; se guardan en el archivo original cuando el proveedor concede escritura.
- Práctica (pestaña inferior): `print()`, `input()`, `int()`, `float()`, `str()`, variables, `if/else`, `for`, `while`, `def`, listas y operadores, con ejemplo, resultado esperado, botón *Copiar* y *Probar en el editor*.
- Ajustes persistentes (DataStore): tamaño de fuente, guardado automático, indentación automática, ajuste de línea, autocompletado, información y versión.
- Navegación inferior flotante: barra (Inicio · Proyectos · Archivos) + botón circular **+** separado. *Archivos* se puede seleccionar pero es un espacio reservado: no abre pantalla ni hace nada más (intencional).

## Decisiones técnicas

| Tema | Decisión | Motivo |
|---|---|---|
| Android Gradle Plugin | 8.13.0 | Chaquopy 17.0.0 soporta AGP 8.9–8.13 (changelog oficial). |
| Gradle | 8.14.3 (wrapper incluido) | Requisito de AGP 8.13 (≥ 8.13). |
| Kotlin | 2.2.20 + plugin Compose Compiler | Compose ya no usa `composeOptions`; el plugin del compilador va con la versión de Kotlin. |
| Compose BOM | 2025.10.00 | Versión estable que funciona con compileSdk 36 y AGP 8.x. Las versiones más nuevas (p. ej. Compose 1.12) exigen AGP 9 y compileSdk 37, incompatibles con Chaquopy 17. |
| Chaquopy | 17.0.0 | Última versión confirmada; soporta dispositivos con páginas de 16 KB. Python 3.x que incluya esa versión. |
| Sora Editor | 0.23.6 (`editor` + `language-textmate`) | Última versión confirmada en Maven Central. |
| minSdk / target / compile | 26 / 36 / 36 | minSdk 26 permite icono adaptativo sin PNG heredados; Chaquopy 17 requiere ≥ 24. |
| Java | 17 | Requerido por AGP 8.13. |
| Resaltado Python | Gramática TextMate escrita a mano (`assets/textmate/python/…`) + tema `ghost-dark.json` | Sora no incluye Python; así no se depende de descargar gramáticas externas. |
| Fuente de código | Monoespaciada del sistema | No se pudo descargar una fuente externa; se puede añadir una en `res/font` y asignarla en `GhostCodeEditor.kt`. |

### Ubicación de los proyectos (desviación documentada)

Pediste `Documentos/Ghost Proyectos/`. En Android moderno, escribir en el `Documentos` público con la API de archivos (necesaria para que Python importe módulos hermanos) exige el permiso `MANAGE_EXTERNAL_STORAGE`, que preferiste evitar. Por eso Ghost usa el directorio de documentos **propio de la app**:

```
Android/data/com.ghost.editor/files/Documents/Ghost Proyectos/
```

Es accesible desde el explorador de archivos del sistema en la mayoría de dispositivos y desde un PC por USB; **se borra si desinstalas la app**. Para conservar o compartir código usa un archivo externo (SAF) o cópialo fuera. Ajustes muestra la ruta exacta.

### Cómo funciona `input()` (stdin/stdout reales)

`ghost_runner.py` (en `app/src/main/python`) corre dentro del CPython de Chaquopy y recibe un objeto *bridge* de Kotlin:

- `sys.stdout` / `sys.stderr` se sustituyen por objetos que llaman a `bridge.write(texto, esError)`.
- `input()` y `sys.stdin` se sustituyen por funciones que llaman a `bridge.readLine(prompt)`, que **bloquea el hilo de Python** en una `LinkedBlockingQueue` hasta que la consola entrega una línea (`PythonRunner.sendInput`).
- El programa corre en su propio hilo (pila de 16 MB), por lo que la interfaz nunca se bloquea; la salida se agrupa cada ~35 ms antes de pintarse.
- **Detener**: se desbloquea la cola (EOF) y se lanza `KeyboardInterrupt` en el hilo con `PyThreadState_SetAsyncExc`.
- Al terminar se restauran `sys.*`, `cwd`, `sys.path` y se olvidan los módulos del proyecto para que la próxima ejecución use los cambios guardados.

## Abrir en Android Studio

1. Android Studio **Narwhal (2025.1) o más reciente**, con JDK 17 (incluido).
2. *File → Open* y elige la carpeta `Ghost`.
3. Deja que sincronice Gradle (descarga AGP, Chaquopy, Sora…). Necesitas Internet la primera vez.
4. Si pide el SDK, instala *Android SDK Platform 36*.

## Compilar localmente

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requiere JDK 17 y Android SDK 36 (`ANDROID_HOME` configurado). No hace falta Python en tu PC porque no se instalan paquetes pip.

## Subir a GitHub y generar el APK

1. Crea un repositorio vacío en GitHub.
2. Descomprime el ZIP y, dentro de la carpeta, ejecuta:
   ```bash
   git init
   git add .
   git commit -m "Ghost"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```
   (O usa «Upload files» en la web, incluyendo la carpeta oculta `.github`.)
3. En la pestaña **Actions** se ejecuta *Build Ghost APK* automáticamente con cada push (o pulsa *Run workflow*).
4. Al terminar (unos 5–10 min la primera vez), abre la ejecución y descarga el artifact **Ghost-debug-apk** (sección *Artifacts*, abajo). Es un ZIP que contiene `app-debug.apk`.

El workflow usa JDK 17 (Temurin), Android SDK 36, Gradle por wrapper y no necesita secretos.

## Instalar en el teléfono

1. Pasa `app-debug.apk` al teléfono.
2. Ábrelo y permite «instalar apps de origen desconocido» para la app desde la que lo abras.
3. Es un APK *debug* firmado con la clave de depuración: sirve para uso personal.

## Limitaciones de Python en Android

- **Sin `pip install` en el teléfono.** Solo está la biblioteca estándar incluida por Chaquopy. Paquetes de terceros hay que declararlos en `app/build.gradle.kts` (`chaquopy { defaultConfig { pip { install("...") } } }`, requiere un Python de compilación en la máquina que compila) y solo funcionan los puros en Python o los que Chaquopy publica compilados para Android.
- **Sin interfaces gráficas**: `tkinter`, `pygame`, `turtle` (ventana), etc. no funcionan.
- **Sin `matplotlib`, `numpy`, `pandas` por defecto** (existen para Chaquopy, pero hay que añadirlos al build y aumentan el APK).
- **`subprocess`, `os.system`, `multiprocessing` con procesos y `fork`** están muy limitados o no funcionan.
- **`sys.stdin.read()` sin argumentos** espera hasta que pulses Detener (no hay forma de enviar EOF); usa `input()` o `readline()`.
- Detener un programa funciona para código Python normal; una llamada nativa larga (p. ej. un `time.sleep` enorme) termina cuando vuelve al intérprete.
- Los archivos externos se ejecutan desde una **copia temporal** en caché: los `import` de módulos hermanos del archivo original no se resuelven (los proyectos de Ghost Proyectos sí).
- Rutas: el directorio de trabajo es la carpeta del archivo; el almacenamiento accesible es el de la app.
- Python 3.x de Chaquopy: algunas funciones dependientes de la plataforma (`platform`, `os.uname`) devuelven valores de Android/Linux.

## Estructura del código

```
app/src/main/
├── python/ghost_runner.py            # ejecutor: redirección de stdout/stderr/stdin, errores, interrupción
├── assets/textmate/                  # gramática Python, configuración y tema oscuro de Ghost
├── res/                              # icono (vector adaptativo), tema
└── kotlin/com/ghost/editor/
    ├── GhostApp.kt, MainActivity.kt
    ├── data/                         # ProjectRepository, DataStore, modelos, contenido de Práctica
    ├── python/PythonRunner.kt        # hilo de ejecución + puente Kotlin↔Python
    ├── editor/                       # Sora: setup, lenguaje Python, controlador, composable
    ├── viewmodel/                    # EditorViewModel (guardado, ejecución, consola), listas, ajustes
    └── ui/                           # tema, componentes (barra flotante), pantallas, navegación
```

## Puntos a vigilar en la primera compilación

Sora Editor cambia pequeñas firmas entre versiones. Si Gradle marca error, es casi seguro en uno de estos archivos y se arregla con una línea:

- `editor/GhostPythonLanguage.kt`: firmas de `requireAutoComplete` y de `NewlineHandler` (`Content` vs `ContentReference`).
- `editor/EditorSetup.kt`: constantes de `EditorColorScheme`.
- `editor/GhostCodeEditor.kt`: nombres de métodos del `CodeEditor` (`setWordwrap`, `setTextSize`…).

## Licencias

Sora Editor: LGPL 2.1. Chaquopy: MIT (desde 12.0.1). El código de Ghost no incluye claves ni secretos.
