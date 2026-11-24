package com.ghost.editor.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Gestiona la carpeta "Ghost Proyectos" y su papelera.
 *
 * Ubicación: <Documentos de la app>/Ghost Proyectos, es decir
 * Android/data/com.ghost.editor/files/Documents/Ghost Proyectos.
 * La papelera es una carpeta oculta hermana (.papelera): cada elemento eliminado se guarda
 * con un archivo meta.json que recuerda su ruta original para poder restaurarlo.
 */
class ProjectRepository(context: Context) {

    val root: File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
        ROOT_NAME
    ).also { it.mkdirs() }

    private val canonicalRoot: File = root.canonicalFile
    private val trashDir: File = File(canonicalRoot.parentFile ?: canonicalRoot, ".papelera").also { it.mkdirs() }

    private val _revision = MutableStateFlow(0)

    /** Se incrementa tras cada cambio estructural para que las pantallas recarguen sus listas. */
    val revision: StateFlow<Int> = _revision

    private fun bump() = _revision.update { it + 1 }

    /** Avisa a las listas de que algo cambió (p. ej. al cerrar el editor tras guardar). */
    fun notifyChanged() = bump()

    // ---------------------------------------------------------------- rutas

    fun resolve(relPath: String): File {
        val file = (if (relPath.isEmpty()) canonicalRoot else File(canonicalRoot, relPath)).canonicalFile
        val inside = file.path == canonicalRoot.path ||
            file.path.startsWith(canonicalRoot.path + File.separator)
        require(inside) { "Ruta fuera de Ghost Proyectos" }
        return file
    }

    private fun relative(file: File): String =
        file.canonicalFile.relativeTo(canonicalRoot).path.replace(File.separatorChar, '/')

    private fun isVisible(f: File) = !f.name.startsWith(".") && f.name != "__pycache__"

    private fun latestModified(dir: File): Long =
        dir.walkTopDown().maxDepth(3).filter { isVisible(it) || it == dir }
            .maxOfOrNull { it.lastModified() } ?: dir.lastModified()

    // ------------------------------------------------------------- lecturas

    suspend fun list(relPath: String): List<Entry> = withContext(Dispatchers.IO) {
        val dir = resolve(relPath)
        val children = dir.listFiles()?.filter { isVisible(it) } ?: emptyList()
        children
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            .map { f ->
                if (f.isDirectory) {
                    Entry(
                        name = f.name,
                        relPath = relative(f),
                        isDirectory = true,
                        lastModified = latestModified(f),
                        count = (f.listFiles()?.count { isVisible(it) } ?: 0).toLong()
                    )
                } else {
                    Entry(f.name, relative(f), false, f.lastModified(), f.length())
                }
            }
    }

    /** Todos los archivos de todos los proyectos, agrupados por proyecto (pestaña Archivos). */
    suspend fun listAllFiles(): List<ProjectGroup> = withContext(Dispatchers.IO) {
        val top = canonicalRoot.listFiles()?.filter { isVisible(it) } ?: emptyList()
        val groups = ArrayList<ProjectGroup>()

        for (dir in top.filter { it.isDirectory }.sortedBy { it.name.lowercase() }) {
            val files = dir.walkTopDown().maxDepth(8)
                .onEnter { isVisible(it) || it == dir }
                .filter { it.isFile && isVisible(it) }
                .map { Entry(it.name, relative(it), false, it.lastModified(), it.length()) }
                .sortedBy { it.relPath.lowercase() }
                .toList()
            groups.add(ProjectGroup(dir.name, relative(dir), files))
        }

        val loose = top.filter { it.isFile }
            .map { Entry(it.name, relative(it), false, it.lastModified(), it.length()) }
            .sortedBy { it.name.lowercase() }
        if (loose.isNotEmpty()) groups.add(ProjectGroup(ROOT_NAME, "", loose))
        groups
    }

    suspend fun read(relPath: String): String = withContext(Dispatchers.IO) {
        resolve(relPath).readText(Charsets.UTF_8)
    }

    // ----------------------------------------------------------- escrituras

    /**
     * Guarda un archivo. El guardado automático no notifica a las listas (se avisa una vez al
     * cerrar el editor con [notifyChanged]) para no recargar pantallas en cada autoguardado.
     */
    suspend fun write(relPath: String, text: String, notify: Boolean = true) = withContext(Dispatchers.IO) {
        val target = resolve(relPath)
        target.parentFile?.mkdirs()
        // Escritura segura: primero a un temporal y luego se reemplaza el original.
        val tmp = File(target.parentFile, ".${target.name}.tmp")
        tmp.writeText(text, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(text, Charsets.UTF_8)
            tmp.delete()
        }
        if (notify) bump()
    }

    suspend fun createProject(rawName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val name = cleanName(rawName)
            val dir = resolve(name)
            require(!dir.exists()) { "Ya existe un proyecto llamado \"$name\"" }
            check(dir.mkdirs()) { "No se pudo crear la carpeta" }
            bump()
            relative(dir)
        }
    }

    suspend fun createFolder(parentRel: String, rawName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = cleanName(rawName)
                val dir = File(resolve(parentRel), name).canonicalFile
                resolve(relative(dir)) // valida que siga dentro de la raíz
                require(!dir.exists()) { "Ya existe \"$name\"" }
                check(dir.mkdirs()) { "No se pudo crear la carpeta" }
                bump()
                relative(dir)
            }
        }

    /** Crea un archivo .py (añade la extensión si falta) y devuelve su ruta relativa. */
    suspend fun createPythonFile(
        parentRel: String,
        rawName: String,
        content: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var name = cleanName(rawName)
            if (!name.endsWith(".py", ignoreCase = true)) name += ".py"
            val parent = resolve(parentRel)
            parent.mkdirs()
            val file = File(parent, name).canonicalFile
            resolve(relative(file))
            require(!file.exists()) { "Ya existe \"$name\"" }
            file.writeText(content ?: "# $name\n\n", Charsets.UTF_8)
            bump()
            relative(file)
        }
    }

    suspend fun rename(relPath: String, rawNewName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = resolve(relPath)
                require(source != canonicalRoot) { "No se puede renombrar la carpeta raíz" }
                var name = cleanName(rawNewName)
                if (source.isFile && source.extension.equals("py", true) &&
                    !name.endsWith(".py", ignoreCase = true)
                ) name += ".py"
                val target = File(source.parentFile, name).canonicalFile
                resolve(relative(target))
                require(!target.exists() || target == source) { "Ya existe \"$name\"" }
                check(source.renameTo(target)) { "No se pudo renombrar" }
                bump()
                relative(target)
            }
        }

    // ------------------------------------------------------------- papelera

    /** Mueve proyectos, carpetas o archivos a la papelera. Devuelve cuántos se movieron. */
    suspend fun moveToTrash(relPaths: List<String>): Int = withContext(Dispatchers.IO) {
        var moved = 0
        relPaths.forEachIndexed { index, rel ->
            val source = runCatching { resolve(rel) }.getOrNull() ?: return@forEachIndexed
            if (source == canonicalRoot || !source.exists()) return@forEachIndexed
            val originalPath = relative(source)
            val isDir = source.isDirectory
            val entryDir = File(trashDir, "${System.currentTimeMillis()}_${index}_${System.nanoTime() % 100000}")
            entryDir.mkdirs()
            if (moveFile(source, File(entryDir, DATA))) {
                File(entryDir, META).writeText(
                    JSONObject()
                        .put("name", source.name)
                        .put("path", originalPath)
                        .put("dir", isDir)
                        .put("at", System.currentTimeMillis())
                        .toString(),
                    Charsets.UTF_8
                )
                moved++
            } else {
                entryDir.deleteRecursively()
            }
        }
        bump()
        moved
    }

    suspend fun trashItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        (trashDir.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                runCatching {
                    val meta = JSONObject(File(dir, META).readText(Charsets.UTF_8))
                    TrashItem(
                        id = dir.name,
                        name = meta.getString("name"),
                        originalPath = meta.getString("path"),
                        isDirectory = meta.getBoolean("dir"),
                        deletedAt = meta.getLong("at")
                    )
                }.getOrNull()
            }
            .sortedByDescending { it.deletedAt }
    }

    /**
     * Restaura un elemento a su ubicación original, recreando las carpetas que falten.
     * Si ya existe algo con ese nombre: las carpetas se fusionan y los archivos en conflicto
     * se restauran con el sufijo " (restaurado)".
     */
    suspend fun restore(id: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val entryDir = File(trashDir, id)
            val meta = JSONObject(File(entryDir, META).readText(Charsets.UTF_8))
            val data = File(entryDir, DATA)
            check(data.exists()) { "El elemento ya no existe en la papelera" }
            val target = resolve(meta.getString("path"))
            target.parentFile?.mkdirs()
            mergeInto(data, target)
            entryDir.deleteRecursively()
            bump()
            relative(target)
        }
    }

    suspend fun deleteForever(id: String) = withContext(Dispatchers.IO) {
        File(trashDir, id).deleteRecursively()
        bump()
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { it.deleteRecursively() }
        bump()
    }

    private fun moveFile(source: File, dest: File): Boolean {
        if (source.renameTo(dest)) return true
        return source.copyRecursively(dest, overwrite = true) && source.deleteRecursively()
    }

    private fun mergeInto(source: File, dest: File) {
        when {
            !dest.exists() -> check(moveFile(source, dest)) { "No se pudo restaurar" }
            source.isDirectory && dest.isDirectory -> {
                source.listFiles()?.forEach { child -> mergeInto(child, File(dest, child.name)) }
                source.deleteRecursively()
            }
            else -> check(moveFile(source, uniqueSibling(dest))) { "No se pudo restaurar" }
        }
    }

    private fun uniqueSibling(file: File): File {
        val stem = file.nameWithoutExtension
        val ext = if (file.extension.isEmpty()) "" else ".${file.extension}"
        var n = 1
        var candidate = File(file.parentFile, "$stem (restaurado)$ext")
        while (candidate.exists()) {
            n++
            candidate = File(file.parentFile, "$stem (restaurado $n)$ext")
        }
        return candidate
    }

    /** Crea un proyecto de bienvenida la primera vez que se abre Ghost. */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val marker = File(canonicalRoot, ".ghost_seeded")
        if (marker.exists()) return@withContext
        val empty = canonicalRoot.listFiles()?.none { isVisible(it) } ?: true
        if (empty) {
            val dir = File(canonicalRoot, "Primeros pasos").also { it.mkdirs() }
            File(dir, "main.py").writeText(SAMPLE, Charsets.UTF_8)
        }
        marker.createNewFile()
        bump()
    }

    // ----------------------------------------------------------- utilidades

    private fun cleanName(raw: String): String {
        val name = raw.trim()
        require(name.isNotEmpty()) { "El nombre no puede estar vacío" }
        require(name.length <= 80) { "El nombre es demasiado largo" }
        require(!name.startsWith(".")) { "El nombre no puede empezar por punto" }
        require(name.none { it in "/\\:*?\"<>|" || it.isISOControl() }) {
            "El nombre no puede contener / \\ : * ? \" < > |"
        }
        return name
    }

    companion object {
        const val ROOT_NAME = "Ghost Proyectos"
        private const val DATA = "data"
        private const val META = "meta.json"

        private const val SAMPLE = """# Bienvenido a Ghost
nombre = input("¿Cómo te llamas? ")
print("Hola", nombre)

for i in range(1, 4):
    print("Contando:", i)
"""
    }
}
