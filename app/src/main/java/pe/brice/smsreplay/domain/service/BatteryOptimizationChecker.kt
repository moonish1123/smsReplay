package pe.brice.smsreplay.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain service for battery optimization status
 *
 * Abstracts Android battery optimization system, allowing the Domain Layer
 * to check battery optimization status without depending on Android framework.
 *
 * Clean Architecture: Domain Layer defines the interface,
 * Infrastructure Layer (BatteryOptimizationManager) provides implementation.
 */
interface BatteryOptimizationChecker {
    /**
     * Observable state of battery optimization ignoring status
     * true = app is ignoring battery optimizations (good for background service)
     * false = app is subject to battery optimizations (bad for background service)
     */
    val isIgnoringBatteryOptimizations: StateFlow<Boolean>

    /**
     * Check if app is ignoring battery optimizations
     * @return true if ignoring, false otherwise
     */
    fun checkBatteryOptimization(): Boolean

    /**
     * Refresh battery optimization status
     */
    fun refresh()
}
