package com.ghost.editor.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghost.editor.container
import com.ghost.editor.data.Entry
import com.ghost.editor.data.PracticeContent
import com.ghost.editor.data.ProjectGroup
import com.ghost.editor.data.RecentFile
import com.ghost.editor.data.TrashItem
import com.ghost.editor.ui.components.NavTab
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de las pestañas principales (Inicio / Archivos / Práctica); vive en la Activity. */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    var tab by mutableStateOf(NavTab.HOME)

    /** Modo "Eliminar proyectos" de Inicio. */
    var selecting by mutableStateOf(false)
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        // Precarga los ejercicios de Práctica para que la pestaña abra al instante.
        viewModelScope.launch { PracticeContent.load(app) }
    }

    fun startSelecting() {
        tab = NavTab.HOME
        selected = emptySet()
        selecting = true
    }

    fun stopSelecting() {
        selecting = false
        selected = emptySet()
    }

    fun toggle(relPath: String) {
        selected = if (relPath in selected) selected - relPath else selected + relPath
    }
}

/** Pantalla de inicio: cuadrícula de proyectos + archivos externos recientes. */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.projects
    private val recentsRepo = app.container.recents

    var projects by mutableStateOf<List<Entry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    val recents: StateFlow<List<RecentFile>> =
        recentsRepo.recents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            repo.revision.collect { reload() }
        }
    }

    private suspend fun reload() {
        projects = repo.list("").filter { it.isDirectory }
            .sortedByDescending { it.lastModified }
        loaded = true
    }

    fun rename(relPath: String, newName: String) {
        viewModelScope.launch {
            repo.rename(relPath, newName).onFailure { message = it.message }
        }
    }

    /** Envía proyectos a la papelera (se pueden restaurar). */
    fun trash(paths: Collection<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val n = repo.moveToTrash(paths.toList())
            message = when {
                n == 0 -> "No se pudo eliminar"
                n == 1 -> "1 proyecto movido a la papelera"
                else -> "$n proyectos movidos a la papelera"
            }
            onDone()
        }
    }

    fun createProject(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repo.createProject(name)
                .onSuccess(onCreated)
                .onFailure { message = it.message }
        }
    }

    fun forgetRecent(uri: String) {
        viewModelScope.launch { recentsRepo.remove(uri) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
    }
}

/** Pestaña Archivos: archivos de todos los proyectos agrupados por proyecto. */
class FilesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.projects

    var groups by mutableStateOf<List<ProjectGroup>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repo.revision.collect {
                groups = repo.listAllFiles()
                loaded = true
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FilesViewModel(app) as T
    }
}

/** Explorador de una carpeta de proyecto: crear, renombrar, eliminar (a la papelera). */
class FolderViewModel(app: Application, val path: String) : AndroidViewModel(app) {
    private val repo = app.container.projects

    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            repo.revision.collect { reload() }
        }
    }

    private suspend fun reload() {
        entries = try {
            repo.list(path)
        } catch (_: Exception) {
            emptyList() // carpeta eliminada o inaccesible
        }
        loaded = true
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repo.createFolder(path, name).onFailure { message = it.message }
        }
    }

    fun createFile(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repo.createPythonFile(path, name)
                .onSuccess(onCreated)
                .onFailure { message = it.message }
        }
    }

    fun rename(relPath: String, newName: String) {
        viewModelScope.launch {
            repo.rename(relPath, newName).onFailure { message = it.message }
        }
    }

    fun trash(paths: Collection<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val n = repo.moveToTrash(paths.toList())
            message = when {
                n == 0 -> "No se pudo eliminar"
                n == 1 -> "1 elemento movido a la papelera"
                else -> "$n elementos movidos a la papelera"
            }
            onDone()
        }
    }

    class Factory(private val app: Application, private val path: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FolderViewModel(app, path) as T
    }
}

/** Papelera: ver, restaurar, eliminar definitivamente y vaciar. */
class TrashViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.projects

    var items by mutableStateOf<List<TrashItem>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            repo.revision.collect {
                items = repo.trashItems()
                loaded = true
            }
        }
    }

    fun restore(item: TrashItem) {
        viewModelScope.launch {
            repo.restore(item.id)
                .onSuccess { message = "Restaurado en ${it.substringBeforeLast('/', "Ghost Proyectos")}" }
                .onFailure { message = it.message ?: "No se pudo restaurar" }
        }
    }

    fun deleteForever(item: TrashItem) {
        viewModelScope.launch {
            repo.deleteForever(item.id)
            message = "Eliminado definitivamente"
        }
    }

    fun empty() {
        viewModelScope.launch {
            repo.emptyTrash()
            message = "Papelera vaciada"
        }
    }
}
