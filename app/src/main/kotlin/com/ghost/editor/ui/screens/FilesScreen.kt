package com.ghost.editor.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.editor.data.Entry
import com.ghost.editor.data.ProjectGroup
import com.ghost.editor.ui.components.GhostHeader
import com.ghost.editor.ui.components.MenuEntry
import com.ghost.editor.ui.components.ScreenTitle
import com.ghost.editor.ui.components.pressScale
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.util.formatSize
import com.ghost.editor.viewmodel.FilesViewModel

/** Pestaña "Archivos": todos los archivos de todos los proyectos, agrupados por proyecto. */
@Composable
fun FilesScreen(
    vm: FilesViewModel,
    bottomPadding: Dp,
    onOpenFile: (String) -> Unit,
    onSettings: () -> Unit,
    menu: List<MenuEntry>
) {
    Column(Modifier.fillMaxSize()) {
        GhostHeader(onSettings, menu)
        ScreenTitle("Archivos", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
        if (vm.loaded && vm.groups.isEmpty()) {
            Text(
                "Todavía no hay archivos.\nCrea un proyecto con el botón +.",
                color = GhostColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        } else {
            FilesList(
                groups = vm.groups,
                onOpenFile = onOpenFile,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding)
            )
        }
    }
}

private sealed interface FileRow {
    data class Header(val group: ProjectGroup) : FileRow
    data class File(val projectPath: String, val entry: Entry) : FileRow
    data class Empty(val key: String) : FileRow
}

/** Lista de proyectos con sus archivos debajo (reutilizada por el selector del editor). */
@Composable
fun FilesList(
    groups: List<ProjectGroup>,
    onOpenFile: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val rows = remember(groups) {
        buildList<FileRow> {
            groups.forEach { g ->
                add(FileRow.Header(g))
                if (g.files.isEmpty()) add(FileRow.Empty(g.relPath)) else g.files.forEach { add(FileRow.File(g.relPath, it)) }
            }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(rows.size, key = { i ->
            when (val r = rows[i]) {
                is FileRow.Header -> "h:" + r.group.relPath
                is FileRow.File -> "f:" + r.entry.relPath
                is FileRow.Empty -> "e:" + r.key
            }
        }) { i ->
            when (val row = rows[i]) {
                is FileRow.Header -> ProjectHeader(row.group)
                is FileRow.File -> FileItem(row.projectPath, row.entry, onOpenFile)
                is FileRow.Empty -> Text(
                    "Sin archivos",
                    color = GhostColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ProjectHeader(group: ProjectGroup) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Folder, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            group.name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${group.files.size}",
            style = MaterialTheme.typography.labelMedium,
            color = GhostColors.TextSecondary
        )
    }
}

@Composable
private fun FileItem(projectPath: String, entry: Entry, onOpenFile: (String) -> Unit) {
    val source = remember { MutableInteractionSource() }
    // Ruta dentro del proyecto (p. ej. "utils/helpers.py").
    val shown = if (projectPath.isEmpty()) entry.name else entry.relPath.removePrefix("$projectPath/")
    val isPython = entry.name.endsWith(".py", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .pressScale(source, 0.98f)
            .clip(RoundedCornerShape(14.dp))
            .background(GhostColors.Card1)
            .clickable(interactionSource = source, indication = null) { onOpenFile(entry.relPath) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isPython) Icons.Outlined.Code else Icons.Outlined.Description,
            contentDescription = null,
            tint = if (isPython) Color.White else GhostColors.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            shown,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(formatSize(entry.count), style = MaterialTheme.typography.bodySmall, color = GhostColors.TextSecondary)
    }
}
