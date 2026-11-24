package com.ghost.editor.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghost.editor.container
import com.ghost.editor.data.EditorSettings
import com.ghost.editor.data.FileRef
import com.ghost.editor.data.ProjectGroup
import com.ghost.editor.editor.EditorController
import com.ghost.editor.python.PythonRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

enum class SaveStatus { SAVED, UNSAVED, SAVING, ERROR }

enum class ConsoleKind { OUT, ERR, INPUT, INFO }

data class ConsoleSegment(val kind: ConsoleKind, val text: String)

private sealed interface RunnerEvent {
    data class Output(val text: String, val isError: Boolean) : RunnerEvent
    data class InputRequested(val prompt: String) : RunnerEvent
    data class Finished(val exitCode: Int, val internalError: String?) : RunnerEvent
}

/** Una pestaña del editor: un archivo abierto con su propio estado de guardado y su editor. */
class EditorTab(val ref: FileRef, name: String) {
    val id: String = ref.encode()
    var fileName by mutableStateOf(name)
    var initialText by mutableStateOf<String?>(null)
    var loadError by mutableStateOf<String?>(null)
    var saveStatus by mutableStateOf(SaveStatus.SAVED)
    val controller = EditorController()
    var changeCounter = 0
    var autoSaveJob: Job? = null

    val isPython: Boolean get() = fileName.endsWith(".py", ignoreCase = true)
    val isDirty: Boolean get() = saveStatus == SaveStatus.UNSAVED || saveStatus == SaveStatus.ERROR
}

/**
 * Estado del editor: pestañas abiertas, guardado (automático con debounce o manual), borradores
 * de seguridad y ejecución de Python con consola compartida.
 */
class EditorViewModel(app: Application, initialRef: FileRef) : AndroidViewModel(app) {

    private val container = app.container
    private val runner = PythonRunner(app)
    private val saveMutex = Mutex()

    val settings: StateFlow<EditorSettings> =
        container.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, EditorSettings())

    val tabs = mutableStateListOf<EditorTab>()
    var activeId by mutableStateOf("")
        private set
    val activeTab: EditorTab? get() = tabs.firstOrNull { it.id == activeId }

    var message by mutableStateOf<String?>(null)

    val hasUnsavedChanges: Boolean get() = tabs.any { it.isDirty }

    var projectFiles by mutableStateOf<List<ProjectGroup>>(emptyList())
        private set

    // ---------------------------------------------------------- pestañas

    fun openTab(ref: FileRef) {
        val id = ref.encode()
        if (tabs.any { it.id == id }) {
            activeId = id
            return
        }
        if (tabs.size >= MAX_TABS) {
            message = "Máximo $MAX_TABS pestañas abiertas. Cierra alguna para abrir otra."
            return
        }
        val name = when (ref) {
            is FileRef.Internal -> ref.relPath.substringAfterLast('/')
            is FileRef.External -> "archivo.py"
        }
        val tab = EditorTab(ref, name)
        tabs.add(tab)
        activeId = id
        viewModelScope.launch { load(tab) }
    }

    fun selectTab(tab: EditorTab) {
        activeId = tab.id
    }

    /** Cierra una pestaña. Con [save] guarda antes; si no, descarta sus cambios. */
    fun closeTab(tab: EditorTab, save: Boolean) {
        viewModelScope.launch {
            tab.autoSaveJob?.cancel()
            if (save) {
                if (!saveTab(tab)) return@launch // no se pudo guardar: la pestaña sigue abierta
            } else {
                withContext(Dispatchers.IO) { container.drafts.delete(tab.id) }
            }
            val index = tabs.indexOf(tab)
            tabs.remove(tab)
            if (activeId == tab.id) {
                activeId = tabs.getOrNull(index.coerceAtMost(tabs.size - 1))?.id ?: ""
            }
        }
    }

    fun loadProjectFiles() {
        viewModelScope.launch { projectFiles = container.projects.listAllFiles() }
    }

    // ------------------------------------------------------------- carga

    private suspend fun load(tab: EditorTab) {
        try {
            var text = ""
            when (val ref = tab.ref) {
                is FileRef.Internal -> text = container.projects.read(ref.relPath)
                is FileRef.External -> {
                    val resolver = getApplication<Application>().contentResolver
                    val (content, name) = withContext(Dispatchers.IO) {
                        val c = resolver.openInputStream(ref.uri)?.use {
                            it.readBytes().toString(Charsets.UTF_8)
                        } ?: error("No se pudo abrir el archivo")
                        c to queryDisplayName(ref.uri)
                    }
                    text = content
                    tab.fileName = name
                    container.recents.add(ref.uri.toString(), name)
                }
            }
            // ¿Quedaron cambios sin guardar de una sesión anterior (guardado automático desactivado)?
            val draft = withContext(Dispatchers.IO) { container.drafts.load(tab.id) }
            if (draft != null && draft != text) {
                text = draft
                tab.changeCounter++
                tab.saveStatus = SaveStatus.UNSAVED
                message = "Se recuperaron cambios sin guardar de ${tab.fileName}"
            } else if (draft != null) {
                withContext(Dispatchers.IO) { container.drafts.delete(tab.id) }
            }
            tab.initialText = text
        } catch (t: Throwable) {
            tab.loadError = t.message ?: "No se pudo abrir el archivo"
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "archivo.py"
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/') ?: "archivo.py"
        }
    }

    // ---------------------------------------------------------- guardado

    /** Llamado por el editor de una pestaña tras cada cambio de contenido. */
    fun onContentChanged(tab: EditorTab) {
        tab.changeCounter++
        if (tab.saveStatus != SaveStatus.UNSAVED) tab.saveStatus = SaveStatus.UNSAVED
        tab.autoSaveJob?.cancel()
        if (settings.value.autoSave) {
            // Debounce: solo se escribe cuando el usuario deja de teclear brevemente
            // (más espera en archivos grandes para no ralentizar la escritura).
            val wait = if (tab.controller.length() > BIG_FILE_CHARS) 3000L else AUTOSAVE_DELAY_MS
            tab.autoSaveJob = viewModelScope.launch {
                delay(wait)
                saveTab(tab)
            }
        }
    }

    /** Guarda una pestaña. Devuelve false si falló. */
    suspend fun saveTab(tab: EditorTab, force: Boolean = false): Boolean {
        val text = tab.controller.text() ?: return true // aún sin cargar: nada que guardar
        if (!force && !tab.isDirty) return true
        val ticket = tab.changeCounter
        tab.saveStatus = SaveStatus.SAVING
        val ok = saveMutex.withLock {
            withContext(NonCancellable + Dispatchers.IO) { writeText(tab, text) }
        }
        tab.saveStatus = when {
            !ok -> SaveStatus.ERROR
            ticket == tab.changeCounter -> SaveStatus.SAVED
            else -> SaveStatus.UNSAVED
        }
        return ok
    }

    private suspend fun writeText(tab: EditorTab, text: String): Boolean = try {
        when (val ref = tab.ref) {
            is FileRef.Internal -> container.projects.write(ref.relPath, text, notify = false)
            is FileRef.External -> {
                val resolver = getApplication<Application>().contentResolver
                // "wt" = escribir y truncar para no dejar restos del contenido anterior.
                resolver.openOutputStream(ref.uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    ?: error("No se pudo abrir el archivo para escribir")
            }
        }
        container.drafts.delete(tab.id)
        true
    } catch (t: Throwable) {
        message = if (tab.ref is FileRef.External) {
            "No se pudo guardar en el archivo original (${t.message}). " +
                "Usa «Guardar copia en Ghost Proyectos» para no perder tus cambios."
        } else {
            "No se pudo guardar ${tab.fileName}: ${t.message}"
        }
        false
    }

    /** Guarda todas las pestañas con cambios. Devuelve true si todas quedaron guardadas. */
    suspend fun saveAll(): Boolean {
        var ok = true
        for (tab in tabs.toList()) {
            if (tab.isDirty && !saveTab(tab)) ok = false
        }
        return ok
    }

    /** Descarta los cambios pendientes (y sus borradores). */
    fun discardAll() {
        val ids = tabs.map { it.id }
        tabs.forEach { it.autoSaveJob?.cancel() }
        viewModelScope.launch(Dispatchers.IO) { ids.forEach { container.drafts.delete(it) } }
    }

    /**
     * La app pasa a segundo plano: con guardado automático se guarda todo; sin él, los cambios
     * pendientes se copian a un borrador de seguridad (no se toca el archivo original).
     */
    fun onAppStop() {
        if (!hasUnsavedChanges) return
        viewModelScope.launch {
            if (settings.value.autoSave) {
                saveAll()
            } else {
                val pending = tabs.filter { it.isDirty }.mapNotNull { t -> t.controller.text()?.let { t.id to it } }
                withContext(Dispatchers.IO) { pending.forEach { (id, text) -> container.drafts.save(id, text) } }
            }
        }
    }

    /** Copia el contenido actual a Ghost Proyectos/Importados (para archivos externos sin escritura). */
    fun saveCopyToProjects() {
        val tab = activeTab ?: return
        viewModelScope.launch {
            val text = tab.controller.text() ?: return@launch
            val name = tab.fileName.let { if (it.endsWith(".py", true)) it else "$it.py" }
            val result = container.projects.createPythonFile("Importados", name, text)
            message = result.fold(
                onSuccess = { "Copia guardada en Ghost Proyectos/$it" },
                onFailure = { "No se pudo guardar la copia: ${it.message}" }
            )
        }
    }

    // --------------------------------------------------------- ejecución

    var consoleVisible by mutableStateOf(false)
    var consoleSegments by mutableStateOf<List<ConsoleSegment>>(emptyList())
        private set
    var running by mutableStateOf(false)
        private set
    var stopping by mutableStateOf(false)
        private set
    var awaitingInput by mutableStateOf(false)
        private set

    private val events = ConcurrentLinkedQueue<RunnerEvent>()
    private val flushSignal = Channel<Unit>(Channel.CONFLATED)
    private var consoleChars = 0

    private val listener = object : PythonRunner.Listener {
        override fun onOutput(text: String, isError: Boolean) {
            events.add(RunnerEvent.Output(text, isError))
            flushSignal.trySend(Unit)
        }

        override fun onInputRequested(prompt: String) {
            events.add(RunnerEvent.InputRequested(prompt))
            flushSignal.trySend(Unit)
        }

        override fun onFinished(exitCode: Int, internalError: String?) {
            events.add(RunnerEvent.Finished(exitCode, internalError))
            flushSignal.trySend(Unit)
        }
    }

    init {
        // Agrupa la salida de Python para no recomponer la UI en cada print().
        viewModelScope.launch {
            for (signal in flushSignal) {
                drainEvents()
                delay(35)
            }
        }
        openTab(initialRef)
    }

    private fun drainEvents() {
        var segments = consoleSegments
        // Une salidas consecutivas del mismo tipo antes de añadirlas (evita copias repetidas).
        var runKind: ConsoleKind? = null
        val run = StringBuilder()

        fun flushRun() {
            val kind = runKind
            if (kind != null && run.isNotEmpty()) segments = append(segments, kind, run.toString())
            run.setLength(0)
            runKind = null
        }

        while (true) {
            val event = events.poll() ?: break
            when (event) {
                is RunnerEvent.Output -> {
                    val kind = if (event.isError) ConsoleKind.ERR else ConsoleKind.OUT
                    if (runKind != kind) flushRun()
                    runKind = kind
                    run.append(event.text)
                }

                is RunnerEvent.InputRequested -> {
                    flushRun()
                    awaitingInput = true
                }

                is RunnerEvent.Finished -> {
                    flushRun()
                    val note = when {
                        event.internalError != null ->
                            "\n[No se pudo ejecutar Python: ${event.internalError}]\n"
                        event.exitCode == 0 -> "\n[Proceso finalizado]\n"
                        event.exitCode == -2 -> "\n[Ejecución detenida]\n"
                        else -> "\n[Proceso finalizado con código ${event.exitCode}]\n"
                    }
                    segments = append(segments, ConsoleKind.INFO, note)
                    running = false
                    stopping = false
                    awaitingInput = false
                }
            }
        }
        flushRun()
        consoleSegments = segments
    }

    private fun append(list: List<ConsoleSegment>, kind: ConsoleKind, text: String): List<ConsoleSegment> {
        if (text.isEmpty()) return list
        val result = ArrayList(list)
        val last = result.lastOrNull()
        if (last != null && last.kind == kind) {
            result[result.size - 1] = ConsoleSegment(kind, last.text + text)
        } else {
            result.add(ConsoleSegment(kind, text))
        }
        consoleChars += text.length
        // Límite de memoria: se descarta el principio si la salida es enorme.
        while (consoleChars > MAX_CONSOLE_CHARS && result.size > 1) {
            consoleChars -= result.removeAt(0).text.length
        }
        if (consoleChars > MAX_CONSOLE_CHARS && result.size == 1) {
            val only = result[0]
            val trimmed = only.text.takeLast(MAX_CONSOLE_CHARS / 2)
            consoleChars = trimmed.length
            result[0] = only.copy(text = trimmed)
        }
        return result
    }

    fun toggleConsole() {
        consoleVisible = !consoleVisible
    }

    fun clearConsole() {
        consoleSegments = emptyList()
        consoleChars = 0
    }

    /** Guarda, ejecuta el archivo activo con el intérprete real y muestra el resultado en la consola. */
    fun run() {
        val tab = activeTab ?: return
        if (running || !tab.isPython) return
        viewModelScope.launch {
            // Guarda también los demás archivos modificados: el programa puede importarlos.
            saveTab(tab, force = true)
            saveAll()
            val text = tab.controller.text() ?: return@launch
            val file: File = try {
                withContext(Dispatchers.IO) {
                    when (val ref = tab.ref) {
                        is FileRef.Internal -> container.projects.resolve(ref.relPath)
                        is FileRef.External -> {
                            // Python necesita un archivo real: se ejecuta una copia en la caché.
                            val dir = File(getApplication<Application>().cacheDir, "external_run")
                            dir.mkdirs()
                            File(dir, tab.fileName.ifBlank { "main.py" }).also {
                                it.writeText(text, Charsets.UTF_8)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                message = "No se pudo preparar el archivo: ${t.message}"
                return@launch
            }

            consoleVisible = true
            clearConsole()
            consoleSegments = append(emptyList(), ConsoleKind.INFO, "▶ ${tab.fileName}\n")
            running = true
            awaitingInput = false
            if (!runner.start(file, listener)) {
                running = false
                message = "Ya hay un programa en ejecución"
            }
        }
    }

    fun stop() {
        if (!running) return
        stopping = true
        runner.stop()
    }

    /** Envía a input() la línea escrita en la consola. */
    fun submitInput(line: String) {
        if (!awaitingInput) return
        awaitingInput = false
        consoleSegments = append(consoleSegments, ConsoleKind.INPUT, line + "\n")
        runner.sendInput(line)
    }

    override fun onCleared() {
        runner.stop()
        // Los autoguardados no notifican a las listas: se avisa una vez al salir del editor.
        container.projects.notifyChanged()
        super.onCleared()
    }

    class Factory(private val app: Application, private val ref: FileRef) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(app, ref) as T
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 800L
        const val BIG_FILE_CHARS = 200_000
        const val MAX_CONSOLE_CHARS = 60_000
        const val MAX_TABS = 10
    }
}
