package com.ghost.editor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.ghost.editor.data.TrashItem
import com.ghost.editor.ui.components.BackHeader
import com.ghost.editor.ui.components.ConfirmDialog
import com.ghost.editor.ui.components.GhostIconButton
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.util.relativeDate
import com.ghost.editor.viewmodel.TrashViewModel

/** Papelera: elementos eliminados con opción de restaurar, eliminar definitivamente o vaciar. */
@Composable
fun TrashScreen(vm: TrashViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<TrashItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Box(Modifier.fillMaxSize().background(GhostColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            BackHeader(
                title = "Papelera",
                onBack = onBack,
                actions = {
                    if (vm.items.isNotEmpty()) {
                        TextButton(onClick = { confirmEmpty = true }) {
                            Text("Vaciar", color = GhostColors.Danger)
                        }
                    }
                }
            )
            if (vm.loaded && vm.items.isEmpty()) {
                Text(
                    "La papelera está vacía.\nLos proyectos, carpetas y archivos que elimines aparecerán aquí.",
                    color = GhostColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.items, key = { it.id }) { item ->
                    TrashRow(item, onRestore = { vm.restore(item) }, onDelete = { deleteTarget = item })
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = "Eliminar definitivamente",
            message = "«${item.name}» se eliminará para siempre. Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = { vm.deleteForever(item); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
    if (confirmEmpty) {
        ConfirmDialog(
            title = "Vaciar papelera",
            message = "Se eliminarán definitivamente ${vm.items.size} elementos. Esta acción no se puede deshacer.",
            confirmLabel = "Vaciar",
            destructive = true,
            onConfirm = { vm.empty(); confirmEmpty = false },
            onDismiss = { confirmEmpty = false }
        )
    }
}

@Composable
private fun TrashRow(item: TrashItem, onRestore: () -> Unit, onDelete: () -> Unit) {
    val date = remember(item.deletedAt) { relativeDate(item.deletedAt) }
    val parent = item.originalPath.substringBeforeLast('/', "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhostColors.Card1)
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
                    item.isDirectory -> Icons.Outlined.Folder
                    item.name.endsWith(".py", true) -> Icons.Outlined.Code
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
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                (if (parent.isEmpty()) "Ghost Proyectos" else "En $parent") + " · $date",
                style = MaterialTheme.typography.bodySmall,
                color = GhostColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        GhostIconButton(Icons.Outlined.RestoreFromTrash, "Restaurar", onRestore, size = 40.dp)
        GhostIconButton(Icons.Outlined.DeleteForever, "Eliminar definitivamente", onDelete, tint = GhostColors.Danger, size = 40.dp)
    }
}
