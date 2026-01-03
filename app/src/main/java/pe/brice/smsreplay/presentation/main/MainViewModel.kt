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
import pe.brice.smsreplay.domain.usecase.CheckSystemHealthUseCase
import pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase
import pe.brice.smsreplay.domain.usecase.GetSecurityConfirmedUseCase
import pe.brice.smsreplay.domain.usecase.SetSecurityConfirmedUseCase
import pe.brice.smsreplay.domain.service.BatteryOptimizationChecker
import pe.brice.smsreplay.domain.service.PermissionChecker
import pe.brice.smsreplay.domain.service.ServiceControl
import pe.brice.smsreplay.domain.service.SmsQueueControl
import timber.log.Timber

/**
 * ViewModel for Main Screen
 * Handles service control and status display
 *
 * Clean Architecture: Depends only on Domain Layer (UseCases and service interfaces)
 * No direct dependencies on Infrastructure Layer implementations.
 */
class MainViewModel : ViewModel(), KoinComponent {

    // Domain Layer service interfaces (abstractions from Infrastructure Layer)
    private val serviceControl: ServiceControl by inject()
    private val permissionChecker: PermissionChecker by inject()
    private val batteryOptimizationChecker: BatteryOptimizationChecker by inject()
    private val smsQueueControl: SmsQueueControl by inject()

    // Domain UseCases
    private val getSmtpConfigUseCase: GetSmtpConfigUseCase by inject()
    private val canStartMonitoringUseCase: CanStartMonitoringUseCase by inject()
    private val getSecurityConfirmedUseCase: GetSecurityConfirmedUseCase by inject()
    private val setSecurityConfirmedUseCase: SetSecurityConfirmedUseCase by inject()
    private val checkSystemHealthUseCase: CheckSystemHealthUseCase by inject()
    private val saveSmtpConfigUseCase: pe.brice.smsreplay.domain.usecase.SaveSmtpConfigUseCase by inject()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadServiceStatus()
        loadPermissionsStatus()
        loadBatteryStatus()
        loadQueueStatus()
        loadSecurityConfirmation()
        loadSmtpConfigStatus()
        checkSystemHealth() // Initial check
    }

    // ... (omitted)

    fun checkSystemHealth() {
        viewModelScope.launch {
            Timber.d("Checking system health...")
            // Give system some time to update settings database
            kotlinx.coroutines.delay(500)
            val issues = checkSystemHealthUseCase()
            Timber.d("System health check completed. Found ${issues.size} issues.")
            _uiState.value = _uiState.value.copy(systemIssues = issues)
        }
    }

    private fun loadServiceStatus() {
        viewModelScope.launch {
            serviceControl.isServiceRunning.collect { isRunning ->
                _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
                checkSystemHealth() // Re-check issues when service starts/stops
            }
        }
    }

    private fun loadPermissionsStatus() {
        viewModelScope.launch {
            permissionChecker.hasRequiredPermissions.collect { hasPermissions ->
                _uiState.value = _uiState.value.copy(hasPermissions = hasPermissions)
                checkSystemHealth() // Re-check issues when permissions change
            }
        }
    }

    private fun loadBatteryStatus() {
        viewModelScope.launch {
            batteryOptimizationChecker.isIgnoringBatteryOptimizations.collect { isIgnoring ->
                _uiState.value = _uiState.value.copy(isIgnoringBatteryOptimizations = isIgnoring)
                checkSystemHealth() // Re-check issues when battery optimization changes
            }
        }
    }

    private fun loadQueueStatus() {
        viewModelScope.launch {
            smsQueueControl.queueSize.collect { size ->
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
                
                Timber.e("SMTP config status updated: isConfigured=$isConfigured, config=$config")
                _uiState.value = _uiState.value.copy(
                    isConfigured = isConfigured,
                    currentSmtpConfig = config
                )
                checkSystemHealth() // Re-check issues when SMTP config changes
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
        val hasPermissions = permissionChecker.checkAllPermissions()

        if (canStart && hasPermissions) {
            serviceControl.startMonitoring()
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
        serviceControl.stopMonitoring()
    }

    fun refreshPermissions() {
        permissionChecker.refresh()
        batteryOptimizationChecker.refresh()
        checkSystemHealth() // Re-check health on refresh
    }
}

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val isConfigured: Boolean = false,
    val hasPermissions: Boolean = false,
    val queueSize: Int = 0,
    val showSecurityDialog: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isSecurityConfirmed: Boolean = false,
    val systemIssues: List<pe.brice.smsreplay.domain.usecase.CheckSystemHealthUseCase.SystemIssue> = emptyList(),
    val currentSmtpConfig: pe.brice.smsreplay.domain.model.SmtpConfig? = null
)
