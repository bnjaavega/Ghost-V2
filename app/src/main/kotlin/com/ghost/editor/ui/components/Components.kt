package com.ghost.editor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.ghost.editor.R
import com.ghost.editor.ui.theme.GhostColors

/** Logotipo de Ghost (fantasma blanco). */
@Composable
fun GhostLogo(modifier: Modifier = Modifier, height: Dp = 26.dp) {
    Image(
        painter = painterResource(R.drawable.ic_ghost),
        contentDescription = "Ghost",
        modifier = modifier.height(height).width(height * 24f / 32f)
    )
}

/** Animación sutil de escala al pulsar. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.94f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return this.scale(scale)
}

@Composable
fun GhostIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.White,
    size: Dp = 40.dp
) {
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = modifier.size(size).pressScale(source, 0.88f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else GhostColors.Card3,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = GhostColors.Text,
        modifier = modifier
    )
}

// ------------------------------------------------------------------- diálogos

@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "Aceptar",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhostColors.Card1,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = ghostFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) })
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = GhostColors.TextSecondary) } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhostColors.Card1,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, color = GhostColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) GhostColors.Danger else Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = GhostColors.TextSecondary) } }
    )
}

@Composable
fun ghostFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = GhostColors.Card3,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = GhostColors.TextSecondary,
    cursorColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

// ------------------------------------------------------- navegación flotante

enum class NavTab { HOME, FILES, PRACTICE }

/** Opción del menú que se despliega desde el botón "+". */
data class AddAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Composición inferior flotante: barra principal (Inicio / Archivos / Práctica) y, a la
 * derecha y físicamente separado, un botón circular "+" (solo si [addActions] no es null).
 */
@Composable
fun FloatingNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    addActions: List<AddAction>? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Elemento 1: barra principal
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT),
            shape = RoundedCornerShape(30.dp),
            color = GhostColors.Card1,
            shadowElevation = 10.dp,
            border = BorderStroke(0.5.dp, GhostColors.Card2)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem("Inicio", Icons.Filled.Home, Icons.Outlined.Home,
                    selected == NavTab.HOME, { onSelect(NavTab.HOME) }, Modifier.weight(1f))
                NavItem("Archivos", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen,
                    selected == NavTab.FILES, { onSelect(NavTab.FILES) }, Modifier.weight(1f))
                NavItem("Práctica", Icons.Filled.School, Icons.Outlined.School,
                    selected == NavTab.PRACTICE, { onSelect(NavTab.PRACTICE) }, Modifier.weight(1f))
            }
        }

        // Elemento 2: botón circular independiente con menú de opciones
        if (addActions != null) AddButton(addActions)
    }
}

val BAR_HEIGHT = 64.dp

@Composable
private fun AddButton(actions: List<AddAction>) {
    val source = remember { MutableInteractionSource() }
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .size(BAR_HEIGHT)
                .pressScale(source, 0.90f),
            shape = CircleShape,
            color = GhostColors.Card2,
            shadowElevation = 10.dp,
            border = BorderStroke(0.5.dp, GhostColors.Card3),
            onClick = { open = true },
            interactionSource = source
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Añadir",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = GhostColors.Card2
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label, color = Color.White) },
                    leadingIcon = {
                        Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    },
                    onClick = { open = false; action.onClick() }
                )
            }
        }
    }
}

/** Barra inferior del modo selección (eliminar varios elementos). */
@Composable
fun SelectionBar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val cancelSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onCancel,
            interactionSource = cancelSource,
            shape = RoundedCornerShape(30.dp),
            color = GhostColors.Card1,
            shadowElevation = 10.dp,
            border = BorderStroke(0.5.dp, GhostColors.Card2),
            modifier = Modifier.weight(1f).height(BAR_HEIGHT).pressScale(cancelSource, 0.96f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Cancelar", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        val deleteSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onDelete,
            enabled = count > 0,
            interactionSource = deleteSource,
            shape = RoundedCornerShape(30.dp),
            color = if (count > 0) GhostColors.Danger else GhostColors.Card2,
            shadowElevation = 10.dp,
            modifier = Modifier.weight(1f).height(BAR_HEIGHT).pressScale(deleteSource, 0.96f)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (count > 0) "Eliminar ($count)" else "Eliminar",
                    color = if (count > 0) Color.White else GhostColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selectedIcon: ImageVector,
    idleIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) GhostColors.Card3 else Color.Transparent,
        label = "navBackground"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) Color.White else GhostColors.TextSecondary,
        label = "navTint"
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressScale(source, 0.95f)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else idleIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = tint, fontSize = 11.sp, maxLines = 1)
    }
}

/** Indicador circular de selección (modo "Eliminar"). */
@Composable
fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Transparent)
            .then(
                if (selected) Modifier else Modifier.border(1.5.dp, GhostColors.TextSecondary, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = Color.Black, modifier = Modifier.size(16.dp))
        }
    }
}
