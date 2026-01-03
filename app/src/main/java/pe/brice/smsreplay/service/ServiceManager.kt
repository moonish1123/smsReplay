package pe.brice.smsreplay.service

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import androidx.core.net.toUri
import pe.brice.smsreplay.domain.service.ServiceControl

/**
 * Service Manager for SMS monitoring service lifecycle
 * Responsible ONLY for service start/stop and state management
 *
 * Clean Architecture: Implements ServiceControl interface from Domain Layer
 * This allows Domain Layer to control service without Android dependencies.
 *
 * Note: This is an infrastructure service, not part of Domain Layer
 */
class ServiceManager(private val context: Context) : ServiceControl {

    private val _isServiceRunning = MutableStateFlow(false)
    override val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    init {
        // Check if service is actually running on initialization
        _isServiceRunning.value = isServiceActuallyRunning()
        Timber.d("ServiceManager initialized: isServiceRunning=${_isServiceRunning.value}")
    }

    /**
     * Start SMS monitoring service
     */
    override fun startMonitoring() {
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
    override fun stopMonitoring() {
        if (!_isServiceRunning.value) {
            Timber.d("Service not running")
            return
        }

        SmsForegroundService.stopService(context)
        _isServiceRunning.value = false
        Timber.i("SMS monitoring service stopped")
    }

    /**
     * Check if service is actually running in the system
     * Uses ActivityManager to check real service status
     */
    private fun isServiceActuallyRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)

        return runningServices.any { serviceInfo ->
            serviceInfo.service.className == SmsForegroundService::class.java.name
        }
    }
}

/**
 * Permission Manager for checking app permissions
 * Separated from ServiceManager for single responsibility
 */
class PermissionManager(private val context: Context) {

    private val _hasRequiredPermissions = MutableStateFlow(false)
    val hasRequiredPermissions: StateFlow<Boolean> = _hasRequiredPermissions.asStateFlow()

    init {
        // Check permissions on initialization
        _hasRequiredPermissions.value = checkAllPermissions()
        Timber.d("PermissionManager initialized: hasPermissions=${_hasRequiredPermissions.value}")
    }

    /**
     * Check all required permissions for SMS monitoring
     */
    fun checkAllPermissions(): Boolean {
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
        Timber.d("Permissions checked: hasPermissions=$result")
        return result
    }

    /**
     * Refresh permission state
     */
    fun refresh() {
        _hasRequiredPermissions.value = checkAllPermissions()
    }
}

/**
 * Battery Optimization Manager for checking battery optimization status
 * Separated from ServiceManager for single responsibility
 */
class BatteryOptimizationManager(private val context: Context) {

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(false)
    val isIgnoringBatteryOptimizations: StateFlow<Boolean> = _isIgnoringBatteryOptimizations.asStateFlow()

    init {
        // Check battery optimization on initialization
        checkBatteryOptimization()
    }

    /**
     * Check if battery optimization is ignored for this app
     */
    fun checkBatteryOptimization(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = pm.isIgnoringBatteryOptimizations(context.packageName)
                _isIgnoringBatteryOptimizations.value = isIgnoring
                Timber.d("Battery optimization ignored: $isIgnoring")
                return isIgnoring
            } catch (e: Exception) {
                Timber.e(e, "Failed to check battery optimization")
                _isIgnoringBatteryOptimizations.value = false
                return false
            }
        }
        // Android 6.0 미만은 배터리 최적화가 없음
        _isIgnoringBatteryOptimizations.value = true
        return true
    }

    /**
     * Create intent to request battery optimization exemption
     */
    fun createBatteryOptimizationIntent(): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
    }

    /**
     * Refresh battery optimization state
     */
    fun refresh() {
        checkBatteryOptimization()
    }
}
