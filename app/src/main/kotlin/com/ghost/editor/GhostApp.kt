package com.ghost.editor

import android.app.Application
import android.content.Context
import com.ghost.editor.data.DraftStore
import com.ghost.editor.data.ProjectRepository
import com.ghost.editor.data.RecentFilesRepository
import com.ghost.editor.data.SettingsRepository

/** Contenedor de dependencias sencillo (sin frameworks de inyección). */
class AppContainer(context: Context) {
    val projects = ProjectRepository(context)
    val settings = SettingsRepository(context)
    val recents = RecentFilesRepository(context)
    val drafts = DraftStore(context)
}

class GhostApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as GhostApp).container
