package com.ghost.editor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.editor.data.PracticeContent
import com.ghost.editor.data.PracticeLevel
import com.ghost.editor.data.PracticeTopic
import com.ghost.editor.editor.PythonVocabulary
import com.ghost.editor.ui.components.GhostHeader
import com.ghost.editor.ui.components.MenuEntry
import com.ghost.editor.ui.components.ScreenTitle
import com.ghost.editor.ui.components.pressScale
import com.ghost.editor.ui.theme.CodeFont
import com.ghost.editor.ui.theme.GhostColors

/** Pestaña "Práctica": ejercicios y conceptos de Python separados por nivel. */
@Composable
fun PracticeScreen(
    bottomPadding: Dp,
    onSettings: () -> Unit,
    menu: List<MenuEntry>
) {
    val context = LocalContext.current
    // Los ejercicios se cargan una vez (fuera del hilo principal) y se guardan en caché.
    val topics by produceState<Map<PracticeLevel, List<PracticeTopic>>?>(initialValue = null) {
        value = PracticeContent.load(context)
    }
    var levelIndex by rememberSaveable { mutableIntStateOf(0) }
    // Solo un ejercicio abierto a la vez: menos contenido que componer y medir.
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    val level = PracticeLevel.entries[levelIndex]

    Column(Modifier.fillMaxSize()) {
        GhostHeader(onSettings, menu)
        ScreenTitle("Práctica", Modifier.padding(start = 20.dp, top = 8.dp))
        LevelSelector(
            selected = levelIndex,
            onSelect = { levelIndex = it; expandedId = null },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        val list = topics?.get(level).orEmpty()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "intro-${level.key}") {
                Text(
                    "${list.size} ejercicios · nivel ${level.label.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GhostColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            items(list, key = { it.id }, contentType = { "topic" }) { topic ->
                TopicCard(
                    topic = topic,
                    expanded = topic.id == expandedId,
                    onToggle = { expandedId = if (expandedId == topic.id) null else topic.id }
                )
            }
        }
    }
}

@Composable
private fun LevelSelector(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GhostColors.Card1)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PracticeLevel.entries.forEachIndexed { index, level ->
            val active = index == selected
            val bg by animateColorAsState(if (active) GhostColors.Card3 else Color.Transparent, label = "levelBg")
            val fg by animateColorAsState(if (active) Color.White else GhostColors.TextSecondary, label = "levelFg")
            val source = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(source, 0.96f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable(interactionSource = source, indication = null) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(level.label, color = fg, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TopicCard(topic: PracticeTopic, expanded: Boolean, onToggle: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GhostColors.Card1)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = source, indication = null, onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(topic.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontFamily = CodeFont)
                if (!expanded) {
                    Text(
                        topic.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = GhostColors.TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = GhostColors.TextSecondary,
                // La rotación se lee en la fase de dibujo: no recompone la tarjeta en cada fotograma.
                modifier = Modifier.graphicsLayer { rotationZ = arrow }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TopicDetails(topic)
        }
    }
}

/** Contenido desplegado: solo se compone mientras la tarjeta está abierta. */
@Composable
private fun TopicDetails(topic: PracticeTopic) {
    val clipboard = LocalClipboardManager.current
    // El resaltado se calcula una sola vez por ejercicio (antes se repetía en cada recomposición).
    val highlighted = remember(topic.id) { highlightPython(topic.code) }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Text(topic.summary, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD1D1D6))
        Spacer(Modifier.height(12.dp))
        Label("Ejemplo")
        CodeBlock(highlighted)
        if (topic.output != null) {
            Spacer(Modifier.height(12.dp))
            Label("Resultado")
            CodeBlock(AnnotatedString(topic.output), color = Color(0xFFB5F2C0))
        }
        Spacer(Modifier.height(14.dp))
        CopyButton { clipboard.setText(AnnotatedString(topic.code)) }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = GhostColors.TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun CodeBlock(text: AnnotatedString, color: Color = Color(0xFFECECEC)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GhostColors.CodeBg)
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(text, fontFamily = CodeFont, fontSize = 13.sp, lineHeight = 19.sp, color = color)
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pressScale(source)
            .clip(RoundedCornerShape(12.dp))
            .background(GhostColors.Card2)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Copiar código", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

// ------------------------------------------------- resaltado sencillo de ejemplos

private val TOKEN = Regex(
    "(#.*$)|(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')|(\\b\\d+(?:\\.\\d+)?\\b)|([A-Za-z_][A-Za-z0-9_]*)",
    RegexOption.MULTILINE
)

private val KEYWORD_COLOR = Color(0xFFFC5FA3)
private val STRING_COLOR = Color(0xFFFC8F5D)
private val NUMBER_COLOR = Color(0xFFD9C97C)
private val COMMENT_COLOR = Color(0xFF6C7986)
private val BUILTIN_COLOR = Color(0xFFB48BFF)
private val FUNCTION_COLOR = Color(0xFF6BDFFF)
private val IDENT_COLOR = Color(0xFFA9D5FF)
private val PLAIN_COLOR = Color(0xFFECECEC)

private val KEYWORDS = PythonVocabulary.keywords.toHashSet()
private val BUILTINS = PythonVocabulary.builtins.toHashSet()

fun highlightPython(code: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in TOKEN.findAll(code)) {
        if (m.range.first > last) {
            withStyle(SpanStyle(color = PLAIN_COLOR)) { append(code.substring(last, m.range.first)) }
        }
        val text = m.value
        val color = when {
            m.groups[1] != null -> COMMENT_COLOR
            m.groups[2] != null -> STRING_COLOR
            m.groups[3] != null -> NUMBER_COLOR
            text in KEYWORDS -> KEYWORD_COLOR
            text in BUILTINS -> BUILTIN_COLOR
            code.getOrNull(m.range.last + 1) == '(' -> FUNCTION_COLOR
            else -> IDENT_COLOR
        }
        withStyle(SpanStyle(color = color)) { append(text) }
        last = m.range.last + 1
    }
    if (last < code.length) {
        withStyle(SpanStyle(color = PLAIN_COLOR)) { append(code.substring(last)) }
    }
}
