package com.ghost.editor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.editor.container
import com.ghost.editor.data.EditorSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.settings

    val settings: StateFlow<EditorSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, EditorSettings())

    fun setFontSize(value: Float) = viewModelScope.launch { repo.setFontSize(value) }
    fun setAutoSave(value: Boolean) = viewModelScope.launch { repo.setAutoSave(value) }
    fun setAutoIndent(value: Boolean) = viewModelScope.launch { repo.setAutoIndent(value) }
    fun setWordWrap(value: Boolean) = viewModelScope.launch { repo.setWordWrap(value) }
    fun setAutoComplete(value: Boolean) = viewModelScope.launch { repo.setAutoComplete(value) }
}
