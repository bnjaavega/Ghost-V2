package com.ghost.editor.ui.nav

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghost.editor.container
import com.ghost.editor.data.FileRef
import com.ghost.editor.data.RecentFile
import com.ghost.editor.ui.components.BAR_HEIGHT
import com.ghost.editor.ui.screens.EditorScreen
import com.ghost.editor.ui.screens.FolderScreen
import com.ghost.editor.ui.screens.MainScreen
import com.ghost.editor.ui.screens.SettingsScreen
import com.ghost.editor.ui.screens.TrashScreen
import com.ghost.editor.viewmodel.EditorViewModel
import com.ghost.editor.viewmodel.FolderViewModel
import com.ghost.editor.viewmodel.MainViewModel
import com.ghost.editor.viewmodel.TrashViewModel
import com.ghost.editor.viewmodel.SettingsViewModel

object Routes {
    const val MAIN = "main"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    const val FOLDER = "folder?path={path}"
    const val EDITOR = "editor?ref={ref}"

    fun folder(path: String) = "folder?path=" + Uri.encode(path)
    fun editor(ref: FileRef) = "editor?ref=" + Uri.encode(ref.encode())
}

/** Selector nativo de archivos (Storage Access Framework), empezando en Descargas. */
class OpenPythonDocument : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download")
            )
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}

private fun displayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
} catch (_: Exception) {
    null
}

private fun persistPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Algunos proveedores solo conceden lectura; el guardado informará si falla.
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
    }
}

@Composable
fun navBarBottomPadding(extra: Dp = 0.dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extra

@Composable
fun GhostNavHost(externalUri: Uri?, onExternalConsumed: () -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val container = context.container
    // Pestaña activa (Inicio / Archivos / Práctica): compartida entre Main y las pantallas de proyecto.
    val mainVm: MainViewModel = viewModel()

    fun openInternal(rel: String) = nav.navigate(Routes.editor(FileRef.Internal(rel)))

    val openExternal: (Uri, Boolean) -> Unit = { uri, requirePy ->
        val name = displayName(context, uri) ?: uri.lastPathSegment.orEmpty()
        if (requirePy && !name.endsWith(".py", ignoreCase = true)) {
            Toast.makeText(context, "Selecciona un archivo .py", Toast.LENGTH_SHORT).show()
        } else {
            persistPermission(context, uri)
            nav.navigate(Routes.editor(FileRef.External(uri)))
        }
    }

    val picker = rememberLauncherForActivityResult(OpenPythonDocument()) { uri ->
        if (uri != null) openExternal(uri, true)
    }

    // Archivo .py abierto desde otra app (intent VIEW).
    LaunchedEffect(externalUri) {
        if (externalUri != null) {
            openExternal(externalUri, false)
            onExternalConsumed()
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.MAIN,
        enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 } },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = { fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { it / 12 } }
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                mainVm = mainVm,
                onOpenProject = { nav.navigate(Routes.folder(it)) },
                onOpenFile = { openInternal(it) },
                onOpenRecent = { recent: RecentFile -> openExternal(Uri.parse(recent.uri), false) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onTrash = { nav.navigate(Routes.TRASH) },
                onOpenExternal = { picker.launch(Unit) }
            )
        }

        composable(
            Routes.FOLDER,
            arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val path = entry.arguments?.getString("path").orEmpty()
            val vm: FolderViewModel = viewModel(key = "folder:$path", factory = FolderViewModel.Factory(app, path))
            FolderScreen(
                vm = vm,
                title = path.substringAfterLast('/'),
                selectedTab = mainVm.tab,
                bottomPadding = navBarBottomPadding(BAR_HEIGHT + 40.dp),
                onTab = { tab ->
                    // Vuelve a la pantalla principal en la pestaña elegida.
                    mainVm.tab = tab
                    nav.popBackStack(Routes.MAIN, inclusive = false)
                },
                onBack = { nav.popBackStack() },
                onTrash = { nav.navigate(Routes.TRASH) },
                onOpenFolder = { nav.navigate(Routes.folder(it)) },
                onOpenFile = { openInternal(it) }
            )
        }

        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("ref") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val encoded = entry.arguments?.getString("ref").orEmpty()
            val ref = FileRef.decode(encoded)
            if (ref != null) {
                // Una instancia por pantalla de editor: gestiona todas sus pestañas.
                val vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory(app, ref))
                EditorScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }

        composable(Routes.TRASH) {
            val vm: TrashViewModel = viewModel()
            TrashScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel()
            SettingsScreen(
                vm = vm,
                projectsPath = container.projects.root.absolutePath,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
