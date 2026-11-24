package com.ghost.editor.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.editor.data.RecentFile
import com.ghost.editor.ui.components.AddAction
import com.ghost.editor.ui.components.BAR_HEIGHT
import com.ghost.editor.ui.components.ConfirmDialog
import com.ghost.editor.ui.components.FloatingNavBar
import com.ghost.editor.ui.components.MenuEntry
import com.ghost.editor.ui.components.NameDialog
import com.ghost.editor.ui.components.NavTab
import com.ghost.editor.ui.components.SelectionBar
import com.ghost.editor.ui.nav.navBarBottomPadding
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.viewmodel.FilesViewModel
import com.ghost.editor.viewmodel.HomeViewModel
import com.ghost.editor.viewmodel.MainViewModel

/** Contenedor de las pestañas principales (Inicio, Archivos, Práctica) con la barra flotante. */
@Composable
fun MainScreen(
    mainVm: MainViewModel,
    onOpenProject: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onOpenRecent: (RecentFile) -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app))
    val filesVm: FilesViewModel = viewModel(factory = FilesViewModel.Factory(app))

    var showNewProject by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = mainVm.selecting) { mainVm.stopSelecting() }

    // Menú de tres puntos del encabezado.
    val menu = remember(onTrash) {
        listOf(
            MenuEntry("Eliminar proyectos", Icons.Outlined.Delete) { mainVm.startSelecting() },
            MenuEntry("Papelera", Icons.Outlined.RestoreFromTrash, onTrash)
        )
    }

    // Altura de la barra flotante + márgenes + inset del sistema.
    val bottomPadding = BAR_HEIGHT + 40.dp + navBarBottomPadding()

    Box(Modifier.fillMaxSize().background(GhostColors.Bg)) {
        Crossfade(targetState = mainVm.tab, animationSpec = tween(160), label = "tabs") { tab ->
            when (tab) {
                NavTab.HOME -> HomeScreen(
                    vm = homeVm,
                    bottomPadding = bottomPadding,
                    selecting = mainVm.selecting,
                    selected = mainVm.selected,
                    onToggle = mainVm::toggle,
                    onOpenProject = onOpenProject,
                    onOpenRecent = onOpenRecent,
                    onSettings = onSettings,
                    menu = menu
                )

                NavTab.FILES -> FilesScreen(
                    vm = filesVm,
                    bottomPadding = bottomPadding,
                    onOpenFile = onOpenFile,
                    onSettings = onSettings,
                    menu = menu
                )

                NavTab.PRACTICE -> PracticeScreen(
                    bottomPadding = bottomPadding,
                    onSettings = onSettings,
                    menu = menu
                )
            }
        }

        if (mainVm.selecting) {
            SelectionBar(
                count = mainVm.selected.size,
                onCancel = mainVm::stopSelecting,
                onDelete = { confirmDelete = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            FloatingNavBar(
                selected = mainVm.tab,
                onSelect = { mainVm.tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
                // En Práctica la barra no lleva el botón "+".
                addActions = if (mainVm.tab == NavTab.PRACTICE) null else listOf(
                    AddAction("Nuevo proyecto", Icons.Outlined.CreateNewFolder) { showNewProject = true },
                    AddAction("Abrir .py", Icons.Outlined.FileOpen, onOpenExternal)
                )
            )
        }
    }

    if (showNewProject) {
        NameDialog(
            title = "Nuevo proyecto",
            label = "Nombre del proyecto",
            confirmLabel = "Crear",
            onConfirm = { name ->
                showNewProject = false
                homeVm.createProject(name) { rel -> onOpenProject(rel) }
            },
            onDismiss = { showNewProject = false }
        )
    }

    if (confirmDelete) {
        val count = mainVm.selected.size
        ConfirmDialog(
            title = "Eliminar proyectos",
            message = (if (count == 1) "Se moverá 1 proyecto" else "Se moverán $count proyectos") +
                " a la papelera. Podrás restaurarlos desde el menú ⋮ → Papelera.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                homeVm.trash(mainVm.selected) { mainVm.stopSelecting() }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
