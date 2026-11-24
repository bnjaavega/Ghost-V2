package com.ghost.editor.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference

/**
 * Lenguaje Python de Ghost.
 *
 * Delega en el lenguaje TextMate de Sora (resaltado de sintaxis y pares de símbolos) y añade:
 *  - Autocompletado propio (palabras reservadas, funciones integradas y nombres del documento).
 *  - Indentación automática al pulsar Enter (4 espacios tras ':' o un paréntesis abierto).
 */
class GhostPythonLanguage(private val base: Language) : Language by base {

    @Volatile
    var autoIndent: Boolean = true

    private val newlineHandlers: Array<NewlineHandler> =
        arrayOf(PythonNewlineHandler { autoIndent })

    override fun getNewlineHandlers(): Array<NewlineHandler> = newlineHandlers

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val lineText = content.getLine(position.line).toString()
        val column = position.column.coerceIn(0, lineText.length)
        val before = lineText.substring(0, column)
        var start = before.length
        while (start > 0 && (before[start - 1].isLetterOrDigit() || before[start - 1] == '_')) start--
        val prefix = before.substring(start)
        if (prefix.isEmpty()) return

        val seen = HashSet<String>()
        val results = ArrayList<SimpleCompletionItem>()

        fun offer(word: String, kind: String) {
            if (word.length > prefix.length &&
                word.startsWith(prefix, ignoreCase = true) &&
                seen.add(word)
            ) {
                results.add(SimpleCompletionItem(word, kind, prefix.length, word))
            }
        }

        PythonVocabulary.keywords.forEach { offer(it, "palabra reservada") }
        PythonVocabulary.builtins.forEach { offer(it, "función integrada") }

        // Nombres ya presentes en el documento (variables, funciones propias...).
        val lineCount = minOf(content.lineCount, 4000)
        for (i in 0 until lineCount) {
            val line = content.getLine(i).toString()
            WORD.findAll(line).forEach { m ->
                // No se sugiere la palabra que se está escribiendo en este mismo punto.
                if (!(i == position.line && m.range.first == start)) offer(m.value, "en este archivo")
            }
        }

        results.sortWith(compareBy({ it.label.length }, { it.label.toString().lowercase() }))
        publisher.addItems(results.take(40))
    }

    private companion object {
        val WORD = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}

/** Indentación automática de Python al pulsar Enter. */
private class PythonNewlineHandler(private val enabled: () -> Boolean) : NewlineHandler {

    override fun matchesRequirement(text: Content, position: CharPosition, style: Styles?): Boolean =
        enabled()

    override fun handleNewline(
        text: Content,
        position: CharPosition,
        style: Styles?,
        tabSize: Int
    ): NewlineHandleResult {
        val line = text.getLine(position.line).toString()
        val before = line.substring(0, position.column.coerceIn(0, line.length))
        val indent = before.takeWhile { it == ' ' || it == '\t' }
        val code = stripComment(before).trimEnd()

        val unit = " ".repeat(if (tabSize > 0) tabSize else 4)
        val newIndent = when {
            code.endsWith(":") || code.endsWith("(") || code.endsWith("[") || code.endsWith("{") ->
                indent + unit
            DEDENT_AFTER.any { code.trim() == it || code.trim().startsWith("$it ") } ->
                if (indent.length >= unit.length) indent.dropLast(unit.length) else ""
            else -> indent
        }
        return NewlineHandleResult("\n$newIndent", 0)
    }

    private fun stripComment(s: String): String {
        var inSingle = false
        var inDouble = false
        for (i in s.indices) {
            when (s[i]) {
                '\'' -> if (!inDouble) inSingle = !inSingle
                '"' -> if (!inSingle) inDouble = !inDouble
                '#' -> if (!inSingle && !inDouble) return s.substring(0, i)
            }
        }
        return s
    }

    private companion object {
        val DEDENT_AFTER = listOf("return", "break", "continue", "pass", "raise")
    }
}

object PythonVocabulary {
    val keywords = listOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
        "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise",
        "return", "try", "while", "with", "yield", "match", "case"
    )

    val builtins = listOf(
        "print", "input", "int", "float", "str", "bool", "len", "range", "list", "dict", "set",
        "tuple", "open", "type", "isinstance", "enumerate", "zip", "map", "filter", "sum", "min",
        "max", "abs", "round", "sorted", "reversed", "any", "all", "chr", "ord", "bin", "hex",
        "oct", "id", "iter", "next", "super", "getattr", "setattr", "hasattr", "repr", "format",
        "divmod", "pow", "eval", "exec", "callable", "dir", "help", "bytes", "complex",
        "frozenset", "object", "property", "staticmethod", "classmethod", "__name__", "__main__",
        "append", "extend", "insert", "remove", "pop", "split", "join", "strip", "lower",
        "upper", "replace", "startswith", "endswith", "format", "keys", "values", "items"
    )
}
