package com.ghost.editor.data

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Borradores de seguridad: si el guardado automático está desactivado y la app pasa a segundo
 * plano con cambios sin guardar, se escribe una copia aquí (sin tocar el archivo original).
 * Al volver a abrir el archivo se recuperan esos cambios.
 */
class DraftStore(context: Context) {
    private val dir = File(context.filesDir, "drafts").also { it.mkdirs() }

    private fun file(key: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, hash)
    }

    fun save(key: String, text: String) {
        runCatching { file(key).writeText(text, Charsets.UTF_8) }
    }

    fun load(key: String): String? =
        runCatching { file(key).takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()

    fun delete(key: String) {
        runCatching { file(key).delete() }
    }
}
