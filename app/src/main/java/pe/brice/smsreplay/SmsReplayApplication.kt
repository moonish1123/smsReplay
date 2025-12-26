package pe.brice.smsreplay

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pe.brice.smsreplay.presentation.di.RepositoryModule
import pe.brice.smsreplay.presentation.di.UseCaseModule
import pe.brice.smsreplay.presentation.di.ViewModelModule

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
            // Modules
            modules(RepositoryModule, UseCaseModule, ViewModelModule)
        }

        // Initialize Timber
        if (BuildConfig.DEBUG) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        }
    }
}
