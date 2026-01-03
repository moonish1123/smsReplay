package pe.brice.smsreplay.service

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.brice.smsreplay.domain.service.BatteryOptimizationChecker
import timber.log.Timber

/**
 * Battery Optimization Manager for Android battery optimization settings
 *
 * Clean Architecture: Implements BatteryOptimizationChecker interface from Domain Layer
 * This allows Domain Layer to check battery optimization without Android dependencies.
 */
class BatteryOptimizationManagerImpl(private val context: Context) : BatteryOptimizationChecker {

    // Initialize with default value to avoid recursion during property initialization
    private val _isIgnoringBatteryOptimizations = MutableStateFlow(false)
    override val isIgnoringBatteryOptimizations: StateFlow<Boolean> =
        _isIgnoringBatteryOptimizations.asStateFlow()

    init {
        // Perform initial check
        checkBatteryOptimization()
    }

    override fun checkBatteryOptimization(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            // Safe to update value here as property is already initialized
            _isIgnoringBatteryOptimizations.value = isIgnoring
            Timber.d("Battery optimization check: isIgnoring=$isIgnoring")
            return isIgnoring
        }
        // For Android < 6.0, battery optimization doesn't exist
        _isIgnoringBatteryOptimizations.value = true
        return true
    }

    override fun refresh() {
        Timber.d("Refreshing battery optimization status")
        checkBatteryOptimization()
    }
}
