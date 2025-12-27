package pe.brice.smsreplay.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.CanStartMonitoringUseCase
import pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase
import pe.brice.smsreplay.domain.usecase.GetSecurityConfirmedUseCase
import pe.brice.smsreplay.domain.usecase.SetSecurityConfirmedUseCase
import pe.brice.smsreplay.service.BatteryOptimizationManager
import pe.brice.smsreplay.service.PermissionManager
import pe.brice.smsreplay.service.ServiceManager
import pe.brice.smsreplay.work.SmsQueueManager

/**
 * ViewModel for Main Screen
 * Handles service control and status display
 *
 * Clean Architecture: Only depends on Domain Layer UseCases and infrastructure services
 */
class MainViewModel : ViewModel(), KoinComponent {

    // Infrastructure services (not Domain Layer, but necessary for Android integration)
    private val serviceManager: ServiceManager by inject()
    private val permissionManager: PermissionManager by inject()
    private val batteryOptimizationManager: BatteryOptimizationManager by inject()
    private val smsQueueManager: SmsQueueManager by inject()

    // Domain UseCases
    private val getSmtpConfigUseCase: GetSmtpConfigUseCase by inject()
    private val canStartMonitoringUseCase: CanStartMonitoringUseCase by inject()
    private val getSecurityConfirmedUseCase: GetSecurityConfirmedUseCase by inject()
    private val setSecurityConfirmedUseCase: SetSecurityConfirmedUseCase by inject()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadServiceStatus()
        loadPermissionsStatus()
        loadBatteryStatus()
        loadQueueStatus()
        loadSecurityConfirmation()
        loadSmtpConfigStatus()
    }

    private fun loadServiceStatus() {
        viewModelScope.launch {
            serviceManager.isServiceRunning.collect { isRunning ->
                _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
            }
        }
    }

    private fun loadPermissionsStatus() {
        viewModelScope.launch {
            permissionManager.hasRequiredPermissions.collect { hasPermissions ->
                _uiState.value = _uiState.value.copy(hasPermissions = hasPermissions)
            }
        }
    }

    private fun loadBatteryStatus() {
        viewModelScope.launch {
            batteryOptimizationManager.isIgnoringBatteryOptimizations.collect { isIgnoring ->
                _uiState.value = _uiState.value.copy(isIgnoringBatteryOptimizations = isIgnoring)
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
            val isConfirmed = getSecurityConfirmedUseCase()
            _uiState.value = _uiState.value.copy(isSecurityConfirmed = isConfirmed)
        }
    }

    private fun loadSmtpConfigStatus() {
        viewModelScope.launch {
            getSmtpConfigUseCase().collect { config ->
                val isConfigured = config?.isValid() ?: false
                timber.log.Timber.e("SMTP config status updated: isConfigured=$isConfigured, config=$config")
                _uiState.value = _uiState.value.copy(
                    isConfigured = isConfigured
                )
            }
        }
    }

    fun startService() {
        viewModelScope.launch {
            // 보안 확인이 이미 되어 있으면 바로 시작, 아니면 팝업 표시
            if (_uiState.value.isSecurityConfirmed) {
                attemptStartService()
            } else {
                _uiState.value = _uiState.value.copy(showSecurityDialog = true)
            }
        }
    }

    fun confirmStartService() {
        viewModelScope.launch {
            // 보안 확인 저장
            setSecurityConfirmedUseCase(true)
            _uiState.value = _uiState.value.copy(
                showSecurityDialog = false,
                isSecurityConfirmed = true
            )
            attemptStartService()
        }
    }

    private suspend fun attemptStartService() {
        // Check if service can be started
        val canStart = canStartMonitoringUseCase()
        val hasPermissions = permissionManager.checkAllPermissions()

        if (canStart && hasPermissions) {
            serviceManager.startMonitoring()
        } else {
            // Update UI state to reflect current conditions
            _uiState.value = _uiState.value.copy(
                isConfigured = canStart,
                hasPermissions = hasPermissions
            )
        }
    }

    fun cancelStartService() {
        _uiState.value = _uiState.value.copy(showSecurityDialog = false)
    }

    fun stopService() {
        serviceManager.stopMonitoring()
    }

    fun refreshPermissions() {
        permissionManager.refresh()
        batteryOptimizationManager.refresh()
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
