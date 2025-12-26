package pe.brice.smsreplay.presentation.di

import org.koin.dsl.module
import pe.brice.smsreplay.data.datastore.FilterSettingsDataStore
import pe.brice.smsreplay.data.datastore.SecurePreferencesManager
import pe.brice.smsreplay.data.dao.SentHistoryDao
import pe.brice.smsreplay.data.local.dao.PendingSmsDao
import pe.brice.smsreplay.data.local.database.SmsDatabase
import pe.brice.smsreplay.data.repository.EmailSenderRepositoryImpl
import pe.brice.smsreplay.data.repository.FilterRepositoryImpl
import pe.brice.smsreplay.data.repository.SentHistoryRepositoryImpl
import pe.brice.smsreplay.data.repository.SmtpConfigRepositoryImpl
import pe.brice.smsreplay.data.repository.SmsQueueRepositoryImpl
import pe.brice.smsreplay.domain.repository.EmailSenderRepository
import pe.brice.smsreplay.domain.repository.FilterRepository
import pe.brice.smsreplay.domain.repository.SentHistoryRepository
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository
import pe.brice.smsreplay.domain.repository.SmsQueueRepository
import pe.brice.smsreplay.domain.usecase.AddSentHistoryUseCase
import pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase
import pe.brice.smsreplay.service.ServiceManager
import pe.brice.smsreplay.work.SmsQueueManager

/**
 * Koin DI Module for Repositories
 * Binds interfaces to implementations
 */
val RepositoryModule = module {

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
    single<EmailSenderRepository> { EmailSenderRepositoryImpl(get(), get()) }
    single<SentHistoryRepository> { SentHistoryRepositoryImpl(get()) }

    // Service Manager
    single { ServiceManager(get(), get()) }

    // Queue Manager
    single { SmsQueueManager(get()) }
}
