package pe.brice.smsreplay

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pe.brice.smsreplay.di.DataModule
import pe.brice.smsreplay.di.DomainModule
import pe.brice.smsreplay.di.InfrastructureModule
import pe.brice.smsreplay.presentation.di.PresentationModule
import timber.log.Timber

/**
 * SMS Replay Application
 * Initializes Koin DI
 */
class SmsReplayApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin DI
        startKoin {
            // Android Context
            androidContext(this@SmsReplayApplication)
            // Logger for debugging
            androidLogger()
            // Modules organized by layer
            modules(
                DataModule,             // Data Layer: Repositories, Database, Services
                InfrastructureModule,   // Infrastructure Layer: Android-specific implementations
                DomainModule,           // Domain Layer: Use Cases (business logic)
                PresentationModule      // Presentation Layer: ViewModels
            )
        }

        // Initialize Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
