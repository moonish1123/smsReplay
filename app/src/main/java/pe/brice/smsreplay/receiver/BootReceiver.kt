package pe.brice.smsreplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.service.ServiceManager
import timber.log.Timber

/**
 * Boot Receiver for auto-starting SMS monitoring service
 * Disabled by default, can be enabled by user in settings
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val serviceManager: ServiceManager by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("Boot received - checking if service should start")

            // Check if SMTP is configured before starting service
            // Run in coroutine since isConfigured() is suspend function
        }
    }
}
