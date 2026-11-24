package com.ghost.editor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.editor.BuildConfig
import com.ghost.editor.data.ProjectRepository
import com.ghost.editor.ui.components.BackHeader
import com.ghost.editor.ui.theme.CodeFont
import com.ghost.editor.ui.theme.GhostColors
import com.ghost.editor.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: SettingsViewModel, projectsPath: String, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    var sliderValue by remember(settings.fontSize) { mutableFloatStateOf(settings.fontSize) }

    Column(Modifier.fillMaxSize().background(GhostColors.Bg)) {
        BackHeader(title = "Ajustes", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Editor")
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tamaño de fuente", color = Color.White, modifier = Modifier.weight(1f))
                    Text("${sliderValue.roundToInt()} sp", color = GhostColors.TextSecondary)
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { vm.setFontSize(sliderValue.roundToInt().toFloat()) },
                    valueRange = 10f..24f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = GhostColors.Card3
                    )
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GhostColors.CodeBg)
                        .padding(12.dp)
                ) {
                    Text(
                        "def saludar(nombre):\n    print(\"Hola\", nombre)",
                        fontFamily = CodeFont,
                        fontSize = sliderValue.roundToInt().sp,
                        color = Color(0xFFECECEC)
                    )
                }
            }

            SectionLabel("Preferencias de edición")
            Card {
                SwitchRow(
                    "Guardado automático",
                    "Guarda al dejar de escribir. Si lo desactivas, usa el botón Guardar del editor (se te avisará de los cambios pendientes)",
                    settings.autoSave
                ) { vm.setAutoSave(it) }
                SwitchRow(
                    "Indentación automática",
                    "Mantiene la sangría al pulsar Enter (4 espacios tras «:»)",
                    settings.autoIndent
                ) { vm.setAutoIndent(it) }
                SwitchRow(
                    "Ajuste de línea",
                    "Divide las líneas largas en lugar de desplazarlas",
                    settings.wordWrap
                ) { vm.setWordWrap(it) }
                SwitchRow(
                    "Autocompletado",
                    "Sugiere palabras reservadas, funciones y nombres del archivo",
                    settings.autoComplete
                ) { vm.setAutoComplete(it) }
            }

            SectionLabel("Información")
            Card {
                InfoRow("Aplicación", "Ghost")
                InfoRow("Versión", BuildConfig.VERSION_NAME)
                InfoRow("Intérprete", "CPython (Chaquopy 17)")
                InfoRow("Editor", "Sora Editor 0.23.6")
                Column {
                    Text("Carpeta de proyectos", color = GhostColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${ProjectRepository.ROOT_NAME}\n$projectsPath",
                        color = Color.White,
                        fontFamily = CodeFont,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = GhostColors.TextSecondary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GhostColors.Card1)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White)
            Text(subtitle, color = GhostColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = GhostColors.TextSecondary,
                uncheckedTrackColor = GhostColors.Card2,
                uncheckedBorderColor = GhostColors.Card3
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, color = GhostColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, color = Color.White)
    }
}
