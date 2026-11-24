package com.ghost.editor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.editor.data.Entry
import com.ghost.editor.ui.components.AddAction
import com.ghost.editor.ui.components.BackHeader
import com.ghost.editor.ui.components.ConfirmDialog
import com.ghost.editor.ui.components.FloatingNavBar
import com.ghost.editor.ui.components.GhostIconButton
import com.ghost.editor.ui.components.MenuEntry
import com.ghost.editor.ui.components.NameDialog
import com.ghost.editor.ui.components.NavTab
import com.ghost.editor.ui.components.OverflowMenu
import com.ghost.editor.ui.components.SelectionBar
import com.ghost.editor.ui.components.SelectionMark
import com.ghost.editor.ui.components.pressScale
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.util.formatSize
import com.ghost.editor.util.relativeDate
import com.ghost.editor.viewmodel.FolderViewModel

/**
 * Contenido de un proyecto (o subcarpeta). Mantiene la barra de navegación inferior; su botón
 * "+" ofrece "Nueva carpeta" y "Nuevo archivo". El menú ⋮ permite eliminar varios elementos
 * (se envían a la papelera) y abrir la papelera.
 */
@Composable
fun FolderScreen(
    vm: FolderViewModel,
    title: String,
    selectedTab: NavTab,
    bottomPadding: Dp,
    onTab: (NavTab) -> Unit,
    onBack: () -> Unit,
    onTrash: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenFile: (String) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    var showNewFile by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Entry?>(null) }
    var deleteTarget by remember { mutableStateOf<Entry?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmSelection by remember { mutableStateOf(false) }

    BackHandler(enabled = selecting) {
        selecting = false
        selected = emptySet()
    }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackHeader(
                title = if (selecting) "Selecciona elementos" else title,
                onBack = {
                    if (selecting) { selecting = false; selected = emptySet() } else onBack()
                },
                actions = {
                    if (!selecting) {
                        OverflowMenu(
                            listOf(
                                MenuEntry("Eliminar archivos", Icons.Outlined.Delete) { selecting = true },
                                MenuEntry("Papelera", Icons.Outlined.RestoreFromTrash, onTrash)
                            )
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.entries, key = { it.relPath }) { entry ->
                    EntryRow(
                        entry = entry,
                        selecting = selecting,
                        isSelected = entry.relPath in selected,
                        onClick = {
                            when {
                                selecting -> selected = if (entry.relPath in selected) selected - entry.relPath else selected + entry.relPath
                                entry.isDirectory -> onOpenFolder(entry.relPath)
                                else -> onOpenFile(entry.relPath)
                            }
                        },
                        onRename = { renameTarget = entry },
                        onDelete = { deleteTarget = entry }
                    )
                }

                if (vm.loaded && vm.entries.isEmpty()) {
                    item {
                        Text(
                            "Esta carpeta está vacía.\nUsa el botón + para crear una carpeta o un archivo.",
                            color = GhostColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )

        if (selecting) {
            SelectionBar(
                count = selected.size,
                onCancel = { selecting = false; selected = emptySet() },
                onDelete = { confirmSelection = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            FloatingNavBar(
                selected = selectedTab,
                onSelect = onTab,
                modifier = Modifier.align(Alignment.BottomCenter),
                addActions = listOf(
                    AddAction("Nueva carpeta", Icons.Outlined.CreateNewFolder) { showNewFolder = true },
                    AddAction("Nuevo archivo", Icons.Outlined.NoteAdd) { showNewFile = true }
                )
            )
        }
    }

    if (showNewFile) {
        NameDialog(
            title = "Nuevo archivo Python",
            label = "Nombre (ej. main.py)",
            confirmLabel = "Crear",
            onConfirm = { name ->
                showNewFile = false
                vm.createFile(name) { rel -> onOpenFile(rel) }
            },
            onDismiss = { showNewFile = false }
        )
    }
    if (showNewFolder) {
        NameDialog(
            title = "Nueva carpeta",
            label = "Nombre",
            confirmLabel = "Crear",
            onConfirm = { name -> showNewFolder = false; vm.createFolder(name) },
            onDismiss = { showNewFolder = false }
        )
    }
    renameTarget?.let { target ->
        NameDialog(
            title = "Renombrar",
            label = "Nuevo nombre",
            initial = target.name,
            onConfirm = { vm.rename(target.relPath, it); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = if (target.isDirectory) "Eliminar carpeta" else "Eliminar archivo",
            message = "«${target.name}» se moverá a la papelera. Podrás restaurarlo desde el menú ⋮ → Papelera.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = { vm.trash(listOf(target.relPath)); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
    if (confirmSelection) {
        ConfirmDialog(
            title = "Eliminar elementos",
            message = "Se moverán ${selected.size} elementos a la papelera. Podrás restaurarlos desde el menú ⋮ → Papelera.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                confirmSelection = false
                vm.trash(selected) { selecting = false; selected = emptySet() }
            },
            onDismiss = { confirmSelection = false }
        )
    }
}

@Composable
private fun EntryRow(
    entry: Entry,
    selecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    val isPython = entry.name.endsWith(".py", ignoreCase = true)
    val date = remember(entry.lastModified) { relativeDate(entry.lastModified) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, 0.98f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) GhostColors.Card3 else GhostColors.Card1)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GhostColors.Card2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    entry.isDirectory -> Icons.Outlined.Folder
                    isPython -> Icons.Outlined.Code
                    else -> Icons.Outlined.Description
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = if (entry.isDirectory) {
                "${entry.count} ${if (entry.count == 1L) "elemento" else "elementos"}"
            } else {
                formatSize(entry.count)
            }
            Text(
                "$detail · $date",
                style = MaterialTheme.typography.bodySmall,
                color = GhostColors.TextSecondary,
                maxLines = 1
            )
        }
        if (selecting) {
            Box(Modifier.padding(end = 12.dp)) { SelectionMark(isSelected) }
        } else {
            Box {
                GhostIconButton(Icons.Outlined.MoreVert, "Opciones", { menu = true }, tint = GhostColors.TextSecondary, size = 36.dp)
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = GhostColors.Card2
                ) {
                    DropdownMenuItem(text = { Text("Renombrar", color = Color.White) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Eliminar", color = GhostColors.Danger) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
