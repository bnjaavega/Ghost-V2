package com.ghost.editor.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

/**
 * Ejecuta archivos Python con el intérprete real de Chaquopy.
 *
 * Estrategia de E/S (ver ghost_runner.py):
 *  - El programa corre en un hilo propio, así que la interfaz nunca se bloquea.
 *  - sys.stdout/sys.stderr se sustituyen por objetos que llaman a [PythonBridge.write].
 *  - input() y sys.stdin llaman a [PythonBridge.readLine], que BLOQUEA el hilo de Python
 *    en una cola hasta que la consola entrega una línea con [sendInput].
 *  - [stop] desbloquea la cola (EOF) y lanza KeyboardInterrupt en el hilo de Python.
 */
class PythonRunner(context: Context) {

    interface Listener {
        fun onOutput(text: String, isError: Boolean)
        fun onInputRequested(prompt: String)

        /** [exitCode]: 0 correcto, -2 detenido por el usuario, otro valor = error. */
        fun onFinished(exitCode: Int, internalError: String?)
    }

    private val appContext = context.applicationContext

    @Volatile
    private var thread: Thread? = null

    @Volatile
    private var stopRequested = false

    private val inputQueue = LinkedBlockingQueue<Array<String?>>()

    val isRunning: Boolean get() = thread != null

    fun start(file: File, listener: Listener): Boolean {
        if (thread != null) return false
        stopRequested = false
        inputQueue.clear()

        val bridge = PythonBridge(listener)
        val worker = Thread(null, {
            var exitCode = -1
            var internalError: String? = null
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(appContext))
                }
                val module = Python.getInstance().getModule("ghost_runner")
                exitCode = module.callAttr("run_file", file.absolutePath, bridge).toInt()
                if (stopRequested && exitCode != 0) exitCode = -2
            } catch (t: Throwable) {
                internalError = t.message ?: t.javaClass.simpleName
            } finally {
                thread = null
                listener.onFinished(exitCode, internalError)
            }
        }, "ghost-python", 16L * 1024 * 1024)
        thread = worker
        worker.isDaemon = true
        worker.start()
        return true
    }

    /** Entrega a input() la línea escrita por el usuario. */
    fun sendInput(line: String) {
        inputQueue.offer(arrayOf(line))
    }

    /** Detiene el programa: EOF para input() + KeyboardInterrupt en el hilo de Python. */
    fun stop() {
        if (thread == null) return
        stopRequested = true
        inputQueue.offer(arrayOf(null))
        Thread {
            try {
                Python.getInstance().getModule("ghost_runner").callAttr("interrupt")
            } catch (_: Throwable) {
            }
        }.start()
    }

    /** Objeto que Python recibe como "bridge". Sus métodos públicos son invocados desde Python. */
    inner class PythonBridge(private val listener: Listener) {

        fun write(text: String, isError: Boolean) {
            listener.onOutput(text, isError)
        }

        /** Devuelve la línea escrita por el usuario, o null en EOF. */
        fun readLine(prompt: String): String? {
            if (stopRequested) return null
            listener.onInputRequested(prompt)
            val item = inputQueue.take()
            return item[0]
        }
    }
}
