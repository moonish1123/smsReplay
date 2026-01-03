package pe.brice.smsreplay.presentation.smtp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase
import pe.brice.smsreplay.domain.usecase.SaveSmtpConfigUseCase
import pe.brice.smsreplay.domain.usecase.TestSmtpConnectionUseCase

/**
 * ViewModel for SMTP Settings Screen
 */
class SmtpSettingsViewModel : ViewModel(), KoinComponent {

    private val getSmtpConfigUseCase: GetSmtpConfigUseCase by inject()
    private val saveSmtpConfigUseCase: SaveSmtpConfigUseCase by inject()
    private val testSmtpConnectionUseCase: TestSmtpConnectionUseCase by inject()

    private val _uiState = MutableStateFlow(SmtpSettingsUiState())
    val uiState: StateFlow<SmtpSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                getSmtpConfigUseCase().collect { config ->
                    config?.let {
                        _uiState.value = _uiState.value.copy(
                            serverAddress = it.serverAddress,
                            port = it.port.toString(),
                            username = it.username,
                            password = it.password,
                            recipientEmail = it.recipientEmail,
                            deviceAlias = it.deviceAlias
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "설정을 불러오는데 실패했습니다"
                )
            }
        }
    }

    fun onServerAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(serverAddress = value, showErrors = false)
    }

    fun onPortChange(value: String) {
        _uiState.value = _uiState.value.copy(port = value, showErrors = false)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, showErrors = false)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, showErrors = false)
    }

    fun onRecipientEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(recipientEmail = value, showErrors = false)
    }

    fun onDeviceAliasChange(value: String) {
        _uiState.value = _uiState.value.copy(deviceAlias = value, showErrors = false)
    }

    // 발신자 이메일 자동 생성 (읽기 전용)
    fun getSenderEmail(): String {
        val state = _uiState.value
        if (state.username.isBlank()) {
            return ""
        }

        // username에 이미 @가 포함되어 있으면 그대로 사용
        if (state.username.contains("@")) {
            return state.username
        }

        // username@domain 형식으로 자동 생성
        // 예: smtp.gmail.com → gmail.com
        if (state.serverAddress.isBlank()) {
            return state.username
        }

        val domain = state.serverAddress
            .removePrefix("smtp.")
            .removePrefix("mail.")
            .removePrefix("smtp")
            .removePrefix("mail")

        return "${state.username}@$domain"
    }

    fun saveSettings() {
        val state = _uiState.value

        // Validate
        if (!isValid(state)) {
            _uiState.value = state.copy(showErrors = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            try {
                // 발신자 이메일 자동 생성
                val senderEmail = getSenderEmail()

                val config = pe.brice.smsreplay.domain.model.SmtpConfig(
                    serverAddress = state.serverAddress.trim(),
                    port = state.port.toIntOrNull() ?: 587,
                    username = state.username.trim(),
                    password = state.password,
                    senderEmail = senderEmail.trim(),
                    recipientEmail = state.recipientEmail.trim(),
                    deviceAlias = state.deviceAlias.trim()
                )

                // 먼저 임시로 저장해서 연결 테스트
                saveSmtpConfigUseCase(config)

                // SMTP 연결 테스트
                val testResult = testSmtpConnectionUseCase()
                testResult.fold(
                    onSuccess = {
                        // 연결 성공 - 상태 업데이트
                        _uiState.value = state.copy(
                            isSaving = false,
                            isSuccess = true,
                            errorMessage = null
                        )
                    },
                    onFailure = { exception ->
                        // 연결 실패 - 에러 메시지 표시
                        _uiState.value = state.copy(
                            isSaving = false,
                            isSuccess = false,
                            errorMessage = "SMTP 연결 실패: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isSaving = false,
                    isSuccess = false,
                    errorMessage = "저장에 실패했습니다: ${e.message}"
                )
            }
        }
    }

    private fun isValid(state: SmtpSettingsUiState): Boolean {
        return state.serverAddress.isNotBlank() &&
                state.port.isNotBlank() &&
                state.username.isNotBlank() &&
                state.password.isNotBlank() &&
                state.recipientEmail.isNotBlank()
    }
}

data class SmtpSettingsUiState(
    val serverAddress: String = "",
    val port: String = "587",
    val username: String = "",
    val password: String = "",
    val recipientEmail: String = "",
    val deviceAlias: String = "", // New field for input
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val showErrors: Boolean = false,
    val errorMessage: String? = null
)
