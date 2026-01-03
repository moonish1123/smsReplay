package pe.brice.smsreplay.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain service for SMS monitoring lifecycle control
 *
 * Abstracts infrastructure service management, allowing the Domain Layer
 * to control service lifecycle without depending on Android framework.
 *
 * Clean Architecture: Domain Layer defines the interface,
 * Infrastructure Layer (ServiceManager) provides implementation.
 */
interface ServiceControl {
    /**
     * Observable state of service running status
     */
    val isServiceRunning: StateFlow<Boolean>

    /**
     * Start SMS monitoring service
     */
    fun startMonitoring()

    /**
     * Stop SMS monitoring service
     */
    fun stopMonitoring()
}
