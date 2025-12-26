package pe.brice.smsreplay.service

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase
import timber.log.Timber

/**
 * Service Manager for SMS monitoring service lifecycle
 * Handles permissions, battery optimization, and service state
 */
class ServiceManager(
    private val context: Context,
    getSmtpConfigUseCase: GetSmtpConfigUseCase
) : KoinComponent {

    private val getSmtpConfigUseCase: GetSmtpConfigUseCase by inject()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _hasRequiredPermissions = MutableStateFlow(false)
    val hasRequiredPermissions: StateFlow<Boolean> = _hasRequiredPermissions.asStateFlow()

    /**
     * Start SMS monitoring service
     * Requires: SMTP config + permissions
     */
    fun startMonitoring() {
        if (!hasAllPermissions()) {
            Timber.w("Cannot start service: Missing permissions")
            return
        }

        if (_isServiceRunning.value) {
            Timber.d("Service already running")
            return
        }

        SmsForegroundService.startService(context)
        _isServiceRunning.value = true
        Timber.i("SMS monitoring service started")
    }

    /**
     * Stop SMS monitoring service
     */
    fun stopMonitoring() {
        if (!_isServiceRunning.value) {
            Timber.d("Service not running")
            return
        }

        SmsForegroundService.stopService(context)
        _isServiceRunning.value = false
        Timber.i("SMS monitoring service stopped")
    }

    /**
     * Check if SMTP is configured
     */
    suspend fun isConfigured(): Boolean {
        return try {
            getSmtpConfigUseCase().firstOrNull()?.isValid() ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check SMTP configuration")
            false
        }
    }

    /**
     * Check all required permissions
     */
    fun hasAllPermissions(): Boolean {
        val smsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val result = smsPermission && notificationPermission
        _hasRequiredPermissions.value = result
        return result
    }

    /**
     * Check if battery optimization is ignored
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        // Simplified: return true for now
        // In production, check actual AppOpsManager state
        return true
    }

    /**
     * Create intent to request battery optimization exemption
     */
    fun createBatteryOptimizationIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent()
        }
    }

    /**
     * Create intent to app notification settings
     */
    fun createNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent()
        }
    }

    /**
     * Update service state from external sources
     */
    fun updateServiceState(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }

    /**
     * Refresh permission state
     */
    fun refreshPermissions() {
        _hasRequiredPermissions.value = hasAllPermissions()
    }
}
