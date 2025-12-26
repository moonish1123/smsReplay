package pe.brice.smsreplay.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase
import pe.brice.smsreplay.service.SmsForegroundService
import pe.brice.smsreplay.service.ServiceManager
import pe.brice.smsreplay.work.SmsQueueManager

/**
 * ViewModel for Main Screen
 * Handles service control and status display
 */
class MainViewModel : ViewModel(), KoinComponent {

    private val serviceManager: ServiceManager by inject()
    private val smsQueueManager: SmsQueueManager by inject()
    private val getSmtpConfigUseCase: GetSmtpConfigUseCase by inject()
    private val securePreferencesManager: pe.brice.smsreplay.data.datastore.SecurePreferencesManager by inject()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadServiceStatus()
        loadQueueStatus()
        loadSecurityConfirmation()
    }

    private fun loadServiceStatus() {
        viewModelScope.launch {
            serviceManager.isServiceRunning.collect { isRunning ->
                _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
            }
        }

        viewModelScope.launch {
            serviceManager.hasRequiredPermissions.collect { hasPermissions ->
                _uiState.value = _uiState.value.copy(hasPermissions = hasPermissions)
            }
        }

        viewModelScope.launch {
            serviceManager.isIgnoringBatteryOptimizations.collect { isIgnoring ->
                _uiState.value = _uiState.value.copy(isIgnoringBatteryOptimizations = isIgnoring)
            }
        }

        viewModelScope.launch {
            getSmtpConfigUseCase().collect { config ->
                _uiState.value = _uiState.value.copy(
                    isConfigured = config?.isValid() ?: false
                )
            }
        }
    }

    private fun loadQueueStatus() {
        viewModelScope.launch {
            smsQueueManager.queueSize.collect { size ->
                _uiState.value = _uiState.value.copy(queueSize = size)
            }
        }
    }

    private fun loadSecurityConfirmation() {
        viewModelScope.launch {
            val isConfirmed = securePreferencesManager.isSecurityConfirmed()
            _uiState.value = _uiState.value.copy(isSecurityConfirmed = isConfirmed)
        }
    }

    fun startService() {
        // 보안 확인이 이미 되어 있으면 바로 시작, 아니면 팝업 표시
        if (_uiState.value.isSecurityConfirmed) {
            serviceManager.startMonitoring()
        } else {
            _uiState.value = _uiState.value.copy(showSecurityDialog = true)
        }
    }

    fun confirmStartService() {
        viewModelScope.launch {
            // 보안 확인 저장
            securePreferencesManager.setSecurityConfirmed(true)
            _uiState.value = _uiState.value.copy(
                showSecurityDialog = false,
                isSecurityConfirmed = true
            )
            serviceManager.startMonitoring()
        }
    }

    fun cancelStartService() {
        _uiState.value = _uiState.value.copy(showSecurityDialog = false)
    }

    fun stopService() {
        serviceManager.stopMonitoring()
    }

    fun testSmsReceiver() {
        viewModelScope.launch {
            try {
                timber.log.Timber.i("===== Testing SMS Receiver manually =====")
                val testSms = pe.brice.smsreplay.domain.model.SmsMessage(
                    sender = "01012345678",
                    body = "테스트 메시지입니다",
                    timestamp = System.currentTimeMillis()
                )
                val sendSmsAsEmailUseCase: pe.brice.smsreplay.domain.usecase.SendSmsAsEmailUseCase by inject()
                val result = sendSmsAsEmailUseCase(testSms)
                timber.log.Timber.i("Test SMS result: $result")
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Test SMS failed")
            }
        }
    }
}

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val isConfigured: Boolean = false,
    val hasPermissions: Boolean = false,
    val queueSize: Int = 0,
    val showSecurityDialog: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isSecurityConfirmed: Boolean = false
)
