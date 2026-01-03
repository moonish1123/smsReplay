package pe.brice.smsreplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.CanStartMonitoringUseCase
import pe.brice.smsreplay.domain.service.PermissionChecker
import pe.brice.smsreplay.domain.service.ServiceControl
import timber.log.Timber

/**
 * Boot Receiver for auto-starting SMS monitoring service
 * Automatically starts service on device boot if prerequisites are met
 *
 * Clean Architecture: Depends on Domain Layer interfaces
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val serviceControl: ServiceControl by inject()
    private val permissionChecker: PermissionChecker by inject()
    private val canStartMonitoringUseCase: CanStartMonitoringUseCase by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed - checking if service should auto-start")

            // Check prerequisites and start service if all conditions met
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isConfigured = canStartMonitoringUseCase()
                    val hasPermissions = permissionChecker.checkAllPermissions()

                    if (isConfigured && hasPermissions) {
                        Timber.i("SMTP configured and permissions granted - starting service")
                        serviceControl.startMonitoring()
                    } else {
                        Timber.w("Service not started: configured=$isConfigured, permissions=$hasPermissions")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start service on boot")
                }
            }
        }
    }
}
