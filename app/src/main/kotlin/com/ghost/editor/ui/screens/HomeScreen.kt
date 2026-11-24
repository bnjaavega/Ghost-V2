package com.ghost.editor.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.editor.data.Entry
import com.ghost.editor.data.RecentFile
import com.ghost.editor.ui.components.ConfirmDialog
import com.ghost.editor.ui.components.GhostHeader
import com.ghost.editor.ui.components.GhostIconButton
import com.ghost.editor.ui.components.GhostLogo
import com.ghost.editor.ui.components.MenuEntry
import com.ghost.editor.ui.components.NameDialog
import com.ghost.editor.ui.components.ScreenTitle
import com.ghost.editor.ui.components.SelectionMark
import com.ghost.editor.ui.components.pressScale
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.util.relativeDate
import com.ghost.editor.viewmodel.HomeViewModel

private const val QUOTE = "No hay proyectos fallidos, solo borradores que enseñan qué mejorar."

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    bottomPadding: Dp,
    selecting: Boolean,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenRecent: (RecentFile) -> Unit,
    onSettings: () -> Unit,
    menu: List<MenuEntry>
) {
    val recents by vm.recents.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var renameTarget by remember { mutableStateOf<Entry?>(null) }
    var deleteTarget by remember { mutableStateOf<Entry?>(null) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GhostHeader(onSettings, menu)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        ScreenTitle(
                            if (selecting) "Selecciona proyectos" else "Mis proyectos",
                            Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        if (!selecting) QuoteCard()
                    }
                }

                items(vm.projects, key = { it.relPath }) { entry ->
                    ProjectCard(
                        entry = entry,
                        selecting = selecting,
                        isSelected = entry.relPath in selected,
                        onClick = { if (selecting) onToggle(entry.relPath) else onOpenProject(entry.relPath) },
                        onRename = { renameTarget = entry },
                        onDelete = { deleteTarget = entry }
                    )
                }

                if (vm.loaded && vm.projects.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Aún no tienes proyectos.\nPulsa el botón + para crear uno.",
                            color = GhostColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp)
                        )
                    }
                }

                if (recents.isNotEmpty() && !selecting) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Archivos externos recientes",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
                        )
                    }
                    items(recents, key = { it.uri }, span = { GridItemSpan(maxLineSpan) }) { recent ->
                        RecentRow(recent, onClick = { onOpenRecent(recent) }, onForget = { vm.forgetRecent(recent.uri) })
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
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "Renombrar proyecto",
            label = "Nombre",
            initial = target.name,
            onConfirm = { vm.rename(target.relPath, it); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Eliminar proyecto",
            message = "«${target.name}» se moverá a la papelera. Podrás restaurarlo desde el menú ⋮ → Papelera.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = { vm.trash(listOf(target.relPath)); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

/** Frase inspiradora en el espacio que ocupaban «Nuevo proyecto» y «Abrir .py». */
@Composable
private fun QuoteCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhostColors.Card1)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostLogo(height = 22.dp, modifier = Modifier.alpha(0.55f))
        Spacer(Modifier.width(14.dp))
        Text(
            text = QUOTE,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = Color(0xFFD1D1D6)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    entry: Entry,
    selecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    val date = remember(entry.lastModified) { relativeDate(entry.lastModified) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .pressScale(source, 0.96f)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isSelected) GhostColors.Card3 else GhostColors.Card1)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { if (!selecting) menu = true }
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(GhostColors.Card2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                if (selecting) {
                    SelectionMark(isSelected)
                } else {
                    Text("Python · .py", style = MaterialTheme.typography.labelSmall, color = GhostColors.TextSecondary)
                }
            }
            Column {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${entry.count} ${if (entry.count == 1L) "elemento" else "elementos"} · $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = GhostColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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

@Composable
private fun RecentRow(recent: RecentFile, onClick: () -> Unit, onForget: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, 0.98f)
            .clip(RoundedCornerShape(16.dp))
            .background(GhostColors.Card1)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = GhostColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            recent.name,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        GhostIconButton(Icons.Outlined.Close, "Quitar de recientes", onForget, tint = GhostColors.TextSecondary, size = 36.dp)
    }
}
