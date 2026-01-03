package pe.brice.smsreplay.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.brice.smsreplay.domain.service.PermissionChecker
import timber.log.Timber

/**
 * Permission Manager for Android runtime permissions
 *
 * Clean Architecture: Implements PermissionChecker interface from Domain Layer
 * This allows Domain Layer to check permissions without Android dependencies.
 */
class PermissionManagerImpl(private val context: Context) : PermissionChecker {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val _hasRequiredPermissions = MutableStateFlow(false)
    override val hasRequiredPermissions: StateFlow<Boolean> = _hasRequiredPermissions.asStateFlow()

    init {
        checkAllPermissions()
    }

    override fun checkAllPermissions(): Boolean {
        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        _hasRequiredPermissions.value = allGranted
        Timber.d("Permission check: allGranted=$allGranted")
        return allGranted
    }

    override fun refresh() {
        Timber.d("Refreshing permission status")
        checkAllPermissions()
    }
}
