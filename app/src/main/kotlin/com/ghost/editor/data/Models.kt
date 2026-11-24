package com.ghost.editor.data

import android.net.Uri

/** Elemento (carpeta/proyecto o archivo) dentro de "Ghost Proyectos". */
data class Entry(
    val name: String,
    /** Ruta relativa a la carpeta raíz, usando '/' como separador. */
    val relPath: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    /** Para carpetas: número de elementos visibles. Para archivos: tamaño en bytes. */
    val count: Long
)

/** Referencia a un archivo abierto en el editor. */
sealed interface FileRef {
    /** Archivo dentro de Ghost Proyectos. */
    data class Internal(val relPath: String) : FileRef

    /** Archivo externo abierto con Storage Access Framework. */
    data class External(val uri: Uri) : FileRef

    fun encode(): String = when (this) {
        is Internal -> "i|$relPath"
        is External -> "u|$uri"
    }

    companion object {
        fun decode(value: String): FileRef? = when {
            value.startsWith("i|") -> Internal(value.removePrefix("i|"))
            value.startsWith("u|") -> External(Uri.parse(value.removePrefix("u|")))
            else -> null
        }
    }
}

data class EditorSettings(
    val fontSize: Float = 14f,
    val autoSave: Boolean = true,
    val autoIndent: Boolean = true,
    val wordWrap: Boolean = false,
    val autoComplete: Boolean = true
)

data class RecentFile(val uri: String, val name: String)

/** Elemento de la papelera: conserva la ruta original para poder restaurarlo. */
data class TrashItem(
    val id: String,
    val name: String,
    /** Ruta relativa original dentro de Ghost Proyectos (p. ej. "Calculadora/main.py"). */
    val originalPath: String,
    val isDirectory: Boolean,
    val deletedAt: Long
)

/** Proyecto con todos sus archivos (para la pestaña Archivos). */
data class ProjectGroup(
    val name: String,
    val relPath: String,
    val files: List<Entry>
)
