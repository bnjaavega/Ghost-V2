package com.ghost.editor.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal val Context.ghostDataStore by preferencesDataStore(name = "ghost_prefs")

/** Preferencias persistentes del editor (DataStore). */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.ghostDataStore

    val settings: Flow<EditorSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            val d = EditorSettings()
            EditorSettings(
                fontSize = p[FONT_SIZE] ?: d.fontSize,
                autoSave = p[AUTO_SAVE] ?: d.autoSave,
                autoIndent = p[AUTO_INDENT] ?: d.autoIndent,
                wordWrap = p[WORD_WRAP] ?: d.wordWrap,
                autoComplete = p[AUTO_COMPLETE] ?: d.autoComplete
            )
        }

    suspend fun setFontSize(value: Float) = store.edit { it[FONT_SIZE] = value.coerceIn(10f, 24f) }
    suspend fun setAutoSave(value: Boolean) = store.edit { it[AUTO_SAVE] = value }
    suspend fun setAutoIndent(value: Boolean) = store.edit { it[AUTO_INDENT] = value }
    suspend fun setWordWrap(value: Boolean) = store.edit { it[WORD_WRAP] = value }
    suspend fun setAutoComplete(value: Boolean) = store.edit { it[AUTO_COMPLETE] = value }

    private companion object {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val AUTO_COMPLETE = booleanPreferencesKey("auto_complete")
    }
}

/** Lista de archivos externos abiertos recientemente (URI SAF persistentes). */
class RecentFilesRepository(context: Context) {
    private val store = context.applicationContext.ghostDataStore

    val recents: Flow<List<RecentFile>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it) }

    suspend fun add(uri: String, name: String) {
        store.edit { prefs ->
            val current = decode(prefs).filter { it.uri != uri }
            val updated = (listOf(RecentFile(uri, name)) + current).take(MAX)
            prefs[RECENT] = updated.joinToString("\n") { "${it.uri}\t${it.name}" }
        }
    }

    suspend fun remove(uri: String) {
        store.edit { prefs ->
            val updated = decode(prefs).filter { it.uri != uri }
            prefs[RECENT] = updated.joinToString("\n") { "${it.uri}\t${it.name}" }
        }
    }

    private fun decode(p: Preferences): List<RecentFile> =
        (p[RECENT] ?: "").lines().mapNotNull { line ->
            val parts = line.split('\t', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) RecentFile(parts[0], parts[1]) else null
        }

    private companion object {
        val RECENT = stringPreferencesKey("recent_external")
        const val MAX = 8
    }
}
