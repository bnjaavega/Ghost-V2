package com.ghost.editor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ghost.editor.ui.theme.GhostColors

/** Opción de un menú de tres puntos. */
data class MenuEntry(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Botón de tres puntos con menú de esquinas redondeadas. */
@Composable
fun OverflowMenu(entries: List<MenuEntry>) {
    var open by remember { mutableStateOf(false) }
    Box {
        GhostIconButton(Icons.Outlined.MoreVert, "Más opciones", { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = GhostColors.Card2
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label, color = Color.White) },
                    leadingIcon = {
                        Icon(entry.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    },
                    onClick = { open = false; entry.onClick() }
                )
            }
        }
    }
}

/** Encabezado principal: logotipo + nombre, ajustes y menú de tres puntos. */
@Composable
fun GhostHeader(
    onSettings: () -> Unit,
    menu: List<MenuEntry>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostLogo(height = 26.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Ghost",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        GhostIconButton(Icons.Outlined.Settings, "Ajustes", onSettings)
        OverflowMenu(menu)
    }
}

/** Encabezado simple para pantallas secundarias: flecha atrás + título. */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GhostIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Atrás", onBack)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}
