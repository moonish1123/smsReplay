package pe.brice.smsreplay.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.DeleteSentHistoryUseCase
import pe.brice.smsreplay.domain.usecase.GetSentHistoryUseCase

/**
 * ViewModel for Sent Email History Screen
 */
class SentHistoryViewModel : ViewModel(), KoinComponent {

    private val getSentHistoryUseCase: GetSentHistoryUseCase by inject()
    private val deleteSentHistoryUseCase: DeleteSentHistoryUseCase by inject()

    private val _uiState = MutableStateFlow(SentHistoryUiState())
    val uiState: StateFlow<SentHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory(keyword: String = "") {
        viewModelScope.launch {
            getSentHistoryUseCase(keyword)
                .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "히스토리를 불러오는데 실패했습니다: ${e.message}"
                    )
                }
                .collect { historyList ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        historyList = historyList,
                        errorMessage = null
                    )
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadHistory(query)
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            try {
                deleteSentHistoryUseCase(id)
                // History will be automatically updated via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "삭제에 실패했습니다: ${e.message}"
                )
            }
        }
    }
}

data class SentHistoryUiState(
    val isLoading: Boolean = true,
    val historyList: List<pe.brice.smsreplay.domain.model.SentHistory> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)
