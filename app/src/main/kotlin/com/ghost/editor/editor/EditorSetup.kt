package com.ghost.editor.editor

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * Inicialización única de Sora Editor: gramática TextMate de Python y tema oscuro de Ghost.
 * Los archivos viven en app/src/main/assets/textmate/.
 */
object EditorSetup {
    private const val THEME_NAME = "ghost-dark"
    private const val THEME_PATH = "textmate/themes/ghost-dark.json"
    const val SCOPE_PYTHON = "source.python"

    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val assets = context.applicationContext.assets
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

        val themeStream = assets.open(THEME_PATH)
        val themeModel = ThemeModel(
            IThemeSource.fromInputStream(themeStream, THEME_PATH, null),
            THEME_NAME
        )
        themeModel.isDark = true
        ThemeRegistry.getInstance().loadTheme(themeModel)
        ThemeRegistry.getInstance().setTheme(THEME_NAME)
        initialized = true
    }

    fun createColorScheme(): EditorColorScheme {
        val scheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, 0xFF0B0B0C.toInt())
        scheme.setColor(EditorColorScheme.TEXT_NORMAL, 0xFFECECEC.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF0B0B0C.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER, 0xFF55555B.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, 0xFFB0B0B8.toInt())
        scheme.setColor(EditorColorScheme.LINE_DIVIDER, 0x00000000)
        scheme.setColor(EditorColorScheme.CURRENT_LINE, 0xFF151517.toInt())
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, 0xFF3A4A6B.toInt())
        scheme.setColor(EditorColorScheme.SELECTION_INSERT, 0xFFFFFFFF.toInt())
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, 0xFFFFFFFF.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, 0xFF1C1C1E.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT, 0xFF2C2C2E.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, 0xFFFFFFFF.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, 0xFF8E8E93.toInt())
        return scheme
    }

    fun createPythonLanguage(): GhostPythonLanguage {
        val base = TextMateLanguage.create(SCOPE_PYTHON, false)
        return GhostPythonLanguage(base)
    }
}
