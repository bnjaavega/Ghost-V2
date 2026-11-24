package com.ghost.editor.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Puente entre Compose y la vista CodeEditor de Sora. Permite al ViewModel y a la barra
 * de herramientas leer el texto y lanzar acciones (deshacer, pegar, insertar símbolos...).
 */
class EditorController {
    private var editor: CodeEditor? = null

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    val isAttached: Boolean get() = editor != null

    fun attach(view: CodeEditor) {
        editor = view
        refreshHistory()
    }

    fun detach(view: CodeEditor) {
        if (editor === view) editor = null
    }

    /** Texto completo actual. Debe llamarse desde el hilo principal. */
    fun text(): String? = editor?.text?.toString()

    /** Longitud del texto sin copiarlo (barata). */
    fun length(): Int = editor?.text?.length ?: 0

    fun refreshHistory() {
        val e = editor ?: return
        canUndo = e.canUndo()
        canRedo = e.canRedo()
    }

    fun undo() {
        editor?.undo()
        refreshHistory()
    }

    fun redo() {
        editor?.redo()
        refreshHistory()
    }

    fun copy() {
        editor?.copyText()
    }

    fun cut() {
        editor?.cutText()
    }

    fun paste() {
        editor?.pasteText()
    }

    fun selectAll() {
        editor?.selectAll()
    }

    /**
     * Inserta [text] en el cursor. [cursorBack] desplaza el cursor hacia atrás desde el final
     * del texto insertado (útil para pares como "()" donde el cursor queda en medio).
     */
    fun insert(text: String, cursorBack: Int = 0) {
        val e = editor ?: return
        e.insertText(text, text.length - cursorBack)
        refreshHistory()
    }
}
