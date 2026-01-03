package pe.brice.smsreplay.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain service for permission status checking
 *
 * Abstracts Android permission system, allowing the Domain Layer
 * to check permissions without depending on Android framework.
 *
 * Clean Architecture: Domain Layer defines the interface,
 * Infrastructure Layer (PermissionManager) provides implementation.
 */
interface PermissionChecker {
    /**
     * Observable state of required permissions grant status
     */
    val hasRequiredPermissions: StateFlow<Boolean>

    /**
     * Check if all required permissions are granted
     * @return true if all permissions granted, false otherwise
     */
    fun checkAllPermissions(): Boolean

    /**
     * Refresh permission status
     */
    fun refresh()
}
