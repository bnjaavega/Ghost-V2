package com.ghost.editor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ghost.editor.data.FileRef
import com.ghost.editor.editor.GhostCodeEditor
import com.ghost.editor.viewmodel.EditorTab
import com.ghost.editor.ui.components.GhostIconButton
import com.ghost.editor.ui.components.pressScale
import com.ghost.editor.ui.theme.CodeFont
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.viewmodel.ConsoleKind
import com.ghost.editor.viewmodel.ConsoleSegment
import com.ghost.editor.viewmodel.EditorViewModel
import com.ghost.editor.viewmodel.SaveStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showExitDialog by remember { mutableStateOf(false) }
    var closeTarget by remember { mutableStateOf<EditorTab?>(null) }
    var showOpenSheet by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible

    // Con guardado automático se guarda todo; sin él, se crea un borrador de seguridad.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onAppStop() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    // Al cerrar la última pestaña se vuelve a la pantalla anterior.
    val noTabs = vm.tabs.isEmpty()
    LaunchedEffect(noTabs) { if (noTabs) onBack() }

    val requestExit: () -> Unit = {
        scope.launch {
            if (!vm.settings.value.autoSave && vm.hasUnsavedChanges) {
                showExitDialog = true
            } else if (vm.saveAll()) {
                onBack()
            } else {
                showExitDialog = true // el guardado falló: no se sale sin avisar
            }
        }
    }
    BackHandler(onBack = requestExit)

    val requestClose: (EditorTab) -> Unit = { tab ->
        if (tab.isDirty && (!vm.settings.value.autoSave || tab.saveStatus == SaveStatus.ERROR)) {
            closeTarget = tab
        } else {
            vm.closeTab(tab, save = true)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            EditorTopBar(vm, autoSave = settings.autoSave, onBack = requestExit, onSave = {
                scope.launch { vm.activeTab?.let { vm.saveTab(it, force = true) } }
            })
            TabStrip(
                vm = vm,
                onOpenFile = { vm.loadProjectFiles(); showOpenSheet = true },
                onClose = requestClose
            )
            EditorToolRow(vm)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                // Un editor por pestaña: los inactivos quedan ocultos pero conservan su estado.
                vm.tabs.forEach { tab ->
                    key(tab.id) {
                        val active = tab.id == vm.activeId
                        Box(if (active) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
                            val text = tab.initialText
                            when {
                                tab.loadError != null -> Text(
                                    tab.loadError ?: "",
                                    color = GhostColors.Danger,
                                    modifier = Modifier.padding(20.dp)
                                )

                                text == null -> CircularProgress()
                                else -> GhostCodeEditor(
                                    initialText = text,
                                    settings = settings,
                                    controller = tab.controller,
                                    active = active,
                                    onContentChanged = { vm.onContentChanged(tab) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            val screenHeight = LocalConfiguration.current.screenHeightDp
            AnimatedVisibility(
                visible = vm.consoleVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ConsolePanel(
                    vm = vm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeight * (if (imeVisible) 0.30f else 0.40f)).dp)
                )
            }

            if (imeVisible) SymbolBar(onInsert = { text, back -> vm.activeTab?.controller?.insert(text, back) })
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    // Salir con cambios sin guardar.
    if (showExitDialog) {
        val names = vm.tabs.filter { it.isDirty }.joinToString(", ") { it.fileName }
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = GhostColors.Card1,
            title = { Text("Cambios sin guardar", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Hay cambios sin guardar en: $names.",
                    color = GhostColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    scope.launch { if (vm.saveAll()) onBack() }
                }) { Text("Guardar todo", color = Color.White) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExitDialog = false; vm.discardAll(); onBack() }) {
                        Text("Descartar", color = GhostColors.Danger)
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Cancelar", color = GhostColors.TextSecondary)
                    }
                }
            }
        )
    }

    // Cerrar una pestaña con cambios sin guardar.
    closeTarget?.let { tab ->
        AlertDialog(
            onDismissRequest = { closeTarget = null },
            containerColor = GhostColors.Card1,
            title = { Text("Cambios sin guardar", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text("¿Quieres guardar los cambios de ${tab.fileName} antes de cerrar la pestaña?", color = GhostColors.TextSecondary)
            },
            confirmButton = {
                TextButton(onClick = { closeTarget = null; vm.closeTab(tab, save = true) }) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { closeTarget = null; vm.closeTab(tab, save = false) }) {
                        Text("Descartar", color = GhostColors.Danger)
                    }
                    TextButton(onClick = { closeTarget = null }) {
                        Text("Cancelar", color = GhostColors.TextSecondary)
                    }
                }
            }
        )
    }

    // Selector para abrir otro archivo del proyecto en una pestaña nueva.
    if (showOpenSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOpenSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GhostColors.Card1,
            contentColor = Color.White
        ) {
            Text(
                "Abrir archivo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Box(Modifier.fillMaxWidth().height(440.dp)) {
                FilesList(
                    groups = vm.projectFiles,
                    onOpenFile = { rel ->
                        vm.openTab(FileRef.Internal(rel))
                        showOpenSheet = false
                    },
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun CircularProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}

// ------------------------------------------------------------------ barras

@Composable
private fun EditorTopBar(vm: EditorViewModel, autoSave: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    val tab = vm.activeTab
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Atrás", onBack)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                tab?.fileName ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val (label, color) = when (tab?.saveStatus ?: SaveStatus.SAVED) {
                SaveStatus.SAVED -> "Guardado" to GhostColors.TextSecondary
                SaveStatus.UNSAVED -> "● Sin guardar" to GhostColors.Warning
                SaveStatus.SAVING -> "Guardando…" to GhostColors.TextSecondary
                SaveStatus.ERROR -> "No se pudo guardar" to GhostColors.Danger
            }
            val where = when (val ref = tab?.ref) {
                is FileRef.Internal -> ref.relPath.substringBeforeLast('/', "Ghost Proyectos")
                is FileRef.External -> "Archivo externo"
                null -> ""
            }
            Text(
                "$where · $label",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Con el guardado automático desactivado, el botón Guardar está siempre visible.
        if (!autoSave) {
            val dirty = tab?.isDirty == true
            val source = remember { MutableInteractionSource() }
            Surface(
                onClick = onSave,
                interactionSource = source,
                shape = RoundedCornerShape(20.dp),
                color = if (dirty) Color.White else GhostColors.Card2,
                modifier = Modifier.height(34.dp).pressScale(source, 0.94f)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Save, null,
                        tint = if (dirty) Color.Black else GhostColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Guardar",
                        color = if (dirty) Color.Black else GhostColors.TextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }
        GhostIconButton(
            icon = Icons.Outlined.Terminal,
            description = if (vm.consoleVisible) "Ocultar consola" else "Mostrar consola",
            onClick = vm::toggleConsole,
            tint = if (vm.consoleVisible) Color.White else GhostColors.TextSecondary
        )
        if (tab?.isPython == true) {
            Spacer(Modifier.width(6.dp))
            RunButton(running = vm.running, stopping = vm.stopping, onRun = vm::run, onStop = vm::stop)
        }
    }
}

/** Pestañas de los archivos abiertos (estilo editor de escritorio). */
@Composable
private fun TabStrip(vm: EditorViewModel, onOpenFile: () -> Unit, onClose: (EditorTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        vm.tabs.forEach { tab ->
            key(tab.id) {
                TabChip(
                    tab = tab,
                    active = tab.id == vm.activeId,
                    onClick = { vm.selectTab(tab) },
                    onClose = { onClose(tab) }
                )
            }
        }
        GhostIconButton(Icons.Outlined.Add, "Abrir otro archivo", onOpenFile, tint = GhostColors.TextSecondary, size = 34.dp)
    }
}

@Composable
private fun TabChip(tab: EditorTab, active: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) GhostColors.Card3 else GhostColors.Card1)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            tab.fileName,
            color = if (active) Color.White else GhostColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
        )
        if (tab.isDirty) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(7.dp).clip(CircleShape).background(GhostColors.Warning))
        }
        GhostIconButton(Icons.Outlined.Close, "Cerrar pestaña", onClose, tint = GhostColors.TextSecondary, size = 28.dp)
    }
}

@Composable
private fun RunButton(running: Boolean, stopping: Boolean, onRun: () -> Unit, onStop: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = if (running) onStop else onRun,
        enabled = !stopping,
        shape = CircleShape,
        color = if (running) GhostColors.Danger else Color.White,
        interactionSource = source,
        modifier = Modifier.size(40.dp).pressScale(source, 0.9f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (running) "Detener" else "Ejecutar",
                tint = if (running) Color.White else Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun EditorToolRow(vm: EditorViewModel) {
    val tab = vm.activeTab ?: return
    val c = tab.controller
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostIconButton(Icons.AutoMirrored.Filled.Undo, "Deshacer", c::undo, enabled = c.canUndo, size = 38.dp)
        GhostIconButton(Icons.AutoMirrored.Filled.Redo, "Rehacer", c::redo, enabled = c.canRedo, size = 38.dp)
        Spacer(Modifier.width(8.dp))
        GhostIconButton(Icons.Outlined.ContentCopy, "Copiar", c::copy, size = 38.dp)
        GhostIconButton(Icons.Outlined.ContentCut, "Cortar", c::cut, size = 38.dp)
        GhostIconButton(Icons.Outlined.ContentPaste, "Pegar", c::paste, size = 38.dp)
        GhostIconButton(Icons.Outlined.SelectAll, "Seleccionar todo", c::selectAll, size = 38.dp)
        Spacer(Modifier.width(8.dp))
        val scope = rememberCoroutineScope()
        GhostIconButton(
            Icons.Outlined.Save, "Guardar",
            { scope.launch { vm.saveTab(tab, force = true) } },
            size = 38.dp
        )
        if (tab.ref is FileRef.External) {
            GhostIconButton(Icons.Outlined.SaveAlt, "Guardar copia en Ghost Proyectos", vm::saveCopyToProjects, size = 38.dp)
        }
    }
}

private data class Key(val label: String, val text: String, val back: Int = 0)

private val SYMBOL_KEYS = listOf(
    Key("Tab", "    "),
    Key("( )", "()", 1),
    Key("[ ]", "[]", 1),
    Key("{ }", "{}", 1),
    Key("\" \"", "\"\"", 1),
    Key("' '", "''", 1),
    Key(":", ":"),
    Key("=", "="),
    Key("#", "#"),
    Key(".", "."),
    Key(",", ","),
    Key("_", "_"),
    Key("+", "+"),
    Key("-", "-"),
    Key("*", "*"),
    Key("/", "/"),
    Key("<", "<"),
    Key(">", ">")
)

/** Fila de símbolos frecuentes sobre el teclado, útil para programar desde el teléfono. */
@Composable
private fun SymbolBar(onInsert: (String, Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostColors.Card1)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SYMBOL_KEYS.forEach { key ->
            val source = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .widthIn(min = 40.dp)
                    .height(34.dp)
                    .pressScale(source, 0.92f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(GhostColors.Card2)
                    .clickable(interactionSource = source, indication = null) { onInsert(key.text, key.back) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(key.label, color = Color.White, fontFamily = CodeFont, fontSize = 14.sp)
            }
        }
    }
}

// ----------------------------------------------------------------- consola

@Composable
private fun ConsolePanel(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(GhostColors.CodeBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GhostColors.Card1)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Terminal, null, tint = GhostColors.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    vm.stopping -> "Consola · deteniendo…"
                    vm.running -> "Consola · ejecutando"
                    else -> "Consola"
                },
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            GhostIconButton(Icons.Outlined.DeleteSweep, "Limpiar consola", vm::clearConsole, tint = GhostColors.TextSecondary, size = 36.dp)
            GhostIconButton(Icons.Outlined.KeyboardArrowDown, "Ocultar consola", vm::toggleConsole, tint = GhostColors.TextSecondary, size = 36.dp)
        }

        val scroll = rememberScrollState()
        val segments = vm.consoleSegments
        val totalChars = segments.sumOf { it.text.length }
        LaunchedEffect(totalChars, vm.awaitingInput) { scroll.scrollTo(scroll.maxValue) }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            SelectionContainer {
                Text(
                    text = remember(segments) { consoleText(segments) },
                    fontFamily = CodeFont,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            if (segments.isEmpty()) {
                Text(
                    "La salida de tu programa aparecerá aquí.\nPulsa ▶ para ejecutar.",
                    color = GhostColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (vm.awaitingInput) {
            ConsoleInput(onSubmit = vm::submitInput)
        }
    }
}

private fun consoleText(segments: List<ConsoleSegment>) = buildAnnotatedString {
    segments.forEach { seg ->
        val style = when (seg.kind) {
            ConsoleKind.OUT -> SpanStyle(color = Color(0xFFECECEC))
            ConsoleKind.ERR -> SpanStyle(color = Color(0xFFFF6B6B))
            ConsoleKind.INPUT -> SpanStyle(color = Color(0xFF7CE38B))
            ConsoleKind.INFO -> SpanStyle(color = Color(0xFF8E8E93), fontStyle = FontStyle.Italic)
        }
        withStyle(style) { append(seg.text) }
    }
}

@Composable
private fun ConsoleInput(onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val submit = {
        onSubmit(value)
        value = ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostColors.Card1)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("›", color = Color(0xFF7CE38B), fontFamily = CodeFont, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontFamily = CodeFont, fontSize = 14.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .focusRequester(focus)
        )
        GhostIconButton(Icons.AutoMirrored.Outlined.Send, "Enviar", { submit() }, size = 40.dp)
    }
}
