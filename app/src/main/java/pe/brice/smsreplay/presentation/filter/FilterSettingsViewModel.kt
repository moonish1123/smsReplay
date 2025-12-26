package pe.brice.smsreplay.presentation.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.GetFilterSettingsUseCase
import pe.brice.smsreplay.domain.usecase.SaveFilterSettingsUseCase

/**
 * ViewModel for Filter Settings Screen
 */
class FilterSettingsViewModel : ViewModel(), KoinComponent {

    private val getFilterSettingsUseCase: GetFilterSettingsUseCase by inject()
    private val saveFilterSettingsUseCase: SaveFilterSettingsUseCase by inject()

    private val _uiState = MutableStateFlow(FilterSettingsUiState())
    val uiState: StateFlow<FilterSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = getFilterSettingsUseCase()
                _uiState.value = _uiState.value.copy(
                    senderNumber = settings.senderNumber ?: "",
                    senderNumberEnabled = settings.senderNumber != null,
                    bodyKeyword = settings.bodyKeyword ?: "",
                    bodyKeywordEnabled = settings.bodyKeyword != null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "설정을 불러오는데 실패했습니다"
                )
            }
        }
    }

    fun toggleSenderNumberEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(senderNumberEnabled = enabled)
        if (!enabled) {
            _uiState.value = _uiState.value.copy(senderNumber = "")
        }
    }

    fun toggleBodyKeywordEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(bodyKeywordEnabled = enabled)
        if (!enabled) {
            _uiState.value = _uiState.value.copy(bodyKeyword = "")
        }
    }

    fun onSenderNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(senderNumber = value)
    }

    fun onBodyKeywordChange(value: String) {
        _uiState.value = _uiState.value.copy(bodyKeyword = value)
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            try {
                val settings = pe.brice.smsreplay.domain.model.FilterSettings(
                    senderNumber = if (_uiState.value.senderNumberEnabled) {
                        _uiState.value.senderNumber.trim().ifBlank { null }
                    } else {
                        null
                    },
                    bodyKeyword = if (_uiState.value.bodyKeywordEnabled) {
                        _uiState.value.bodyKeyword.trim().ifBlank { null }
                    } else {
                        null
                    }
                )

                saveFilterSettingsUseCase(settings)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSuccess = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSuccess = false,
                    errorMessage = "저장에 실패했습니다: ${e.message}"
                )
            }
        }
    }
}

data class FilterSettingsUiState(
    val senderNumber: String = "",
    val senderNumberEnabled: Boolean = false,
    val bodyKeyword: String = "",
    val bodyKeywordEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
