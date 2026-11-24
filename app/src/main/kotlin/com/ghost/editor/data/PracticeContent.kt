package com.ghost.editor.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

enum class PracticeLevel(val label: String, val key: String) {
    BASIC("Básico", "basico"),
    INTERMEDIATE("Intermedio", "intermedio"),
    ADVANCED("Avanzado", "avanzado")
}

/** Ejercicio/concepto de la biblioteca "Práctica". */
data class PracticeTopic(
    val id: String,
    val level: PracticeLevel,
    val name: String,
    val summary: String,
    val code: String,
    /** Salida real del ejemplo (generada ejecutándolo), o null si no imprime nada. */
    val output: String?
)

/**
 * Los ejercicios viven en assets/practice/practice.json (se cargan una sola vez, fuera del
 * hilo principal, y se guardan en caché). Las salidas se generaron ejecutando cada ejemplo.
 */
object PracticeContent {
    @Volatile
    private var cache: Map<PracticeLevel, List<PracticeTopic>>? = null

    suspend fun load(context: Context): Map<PracticeLevel, List<PracticeTopic>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            cache ?: parse(context).also { cache = it }
        }
    }

    private fun parse(context: Context): Map<PracticeLevel, List<PracticeTopic>> {
        val text = context.assets.open("practice/practice.json").use { it.readBytes().toString(Charsets.UTF_8) }
        val array = JSONArray(text)
        val topics = ArrayList<PracticeTopic>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val level = PracticeLevel.entries.firstOrNull { it.key == o.getString("level") } ?: continue
            topics.add(
                PracticeTopic(
                    id = o.getString("id"),
                    level = level,
                    name = o.getString("name"),
                    summary = o.getString("summary"),
                    code = o.getString("code"),
                    output = if (o.isNull("output")) null else o.getString("output")
                )
            )
        }
        return PracticeLevel.entries.associateWith { lvl -> topics.filter { it.level == lvl } }
    }
}
