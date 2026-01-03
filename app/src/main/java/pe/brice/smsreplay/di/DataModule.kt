package pe.brice.smsreplay.di

import org.koin.dsl.module
import pe.brice.smsreplay.data.datastore.FilterSettingsDataStore
import pe.brice.smsreplay.data.datastore.SecurePreferencesManager
import pe.brice.smsreplay.data.local.dao.PendingSmsDao
import pe.brice.smsreplay.data.local.database.SmsDatabase
import pe.brice.smsreplay.data.repository.EmailSenderRepositoryImpl
import pe.brice.smsreplay.data.repository.FilterRepositoryImpl
import pe.brice.smsreplay.data.repository.PreferenceRepositoryImpl
import pe.brice.smsreplay.data.repository.SentHistoryRepositoryImpl
import pe.brice.smsreplay.data.repository.SmtpConfigRepositoryImpl
import pe.brice.smsreplay.data.repository.SmsQueueRepositoryImpl
import pe.brice.smsreplay.data.service.EmailTemplateServiceImpl
import pe.brice.smsreplay.data.local.dao.SentHistoryDao
import pe.brice.smsreplay.domain.repository.EmailSenderRepository
import pe.brice.smsreplay.domain.repository.FilterRepository
import pe.brice.smsreplay.domain.repository.PreferenceRepository
import pe.brice.smsreplay.domain.repository.SentHistoryRepository
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository
import pe.brice.smsreplay.domain.repository.SmsQueueRepository
import pe.brice.smsreplay.domain.service.EmailTemplateService

/**
 * Koin DI Module for Data Layer
 * Clean Architecture: Data Layer implementations (repositories, database, services)
 */
val DataModule = module {

    // DataStore & Preferences
    single { SecurePreferencesManager(get()) }
    single { FilterSettingsDataStore(get()) }

    // Database
    single { SmsDatabase.getDatabase(get()) }
    single { get<SmsDatabase>().pendingSmsDao() }
    single { get<SmsDatabase>().sentHistoryDao() }

    // Repository Implementations
    single<SmtpConfigRepository> { SmtpConfigRepositoryImpl(get()) }
    single<FilterRepository> { FilterRepositoryImpl(get()) }
    single<SmsQueueRepository> { SmsQueueRepositoryImpl(get()) }
    single<EmailSenderRepository> { EmailSenderRepositoryImpl(get()) }
    single<SentHistoryRepository> { SentHistoryRepositoryImpl(get()) }
    single<PreferenceRepository> { PreferenceRepositoryImpl(get()) }

    // Email Template Service (bridges Domain Layer to SMTP module)
    single<EmailTemplateService> { EmailTemplateServiceImpl() }
}

/**
 * Koin DI Module for Infrastructure Services
 * Android-specific services that implement Domain Layer interfaces
 *
 * Clean Architecture: Infrastructure Layer → Domain Layer (Dependency Inversion)
 */
val InfrastructureModule = module {

    // Service Manager - implements ServiceControl interface
    single<pe.brice.smsreplay.domain.service.ServiceControl> {
        pe.brice.smsreplay.service.ServiceManager(get())
    }

    // Permission Manager - implements PermissionChecker interface
    single<pe.brice.smsreplay.domain.service.PermissionChecker> {
        pe.brice.smsreplay.service.PermissionManagerImpl(get())
    }

    // Battery Optimization Manager - implements BatteryOptimizationChecker interface
    single<pe.brice.smsreplay.domain.service.BatteryOptimizationChecker> {
        pe.brice.smsreplay.service.BatteryOptimizationManagerImpl(get())
    }

    // Queue Manager - implements SmsQueueControl interface
    single<pe.brice.smsreplay.domain.service.SmsQueueControl> {
        pe.brice.smsreplay.work.SmsQueueManager(get())
    }
}
