package pe.brice.smsreplay.di

import org.koin.dsl.module
import pe.brice.smsreplay.domain.usecase.*

/**
 * Koin DI Module for Domain Layer
 * Clean Architecture: Use Cases (business logic)
 */
val DomainModule = module {

    // SMTP & Filter Use Cases
    single { GetSmtpConfigUseCase(get()) }
    single { SaveSmtpConfigUseCase(get()) }
    single { TestSmtpConnectionUseCase(get()) }
    single { GetFilterSettingsUseCase(get()) }
    single { SaveFilterSettingsUseCase(get()) }
    single { SendSmsAsEmailUseCase(get(), get(), get(), get(), get()) }

    // SMS Processing Use Cases
    single { HandleSmsSendingResultUseCase(get()) }
    single { FlushPendingMessagesUseCase(get(), get()) }

    // Service Control Use Cases
    single { CanStartMonitoringUseCase(get()) }
    single { CheckSystemHealthUseCase(get(), get()) }

    // Sent History Use Cases
    single { GetSentHistoryUseCase(get()) }
    single { AddSentHistoryUseCase(get()) }
    single { DeleteSentHistoryUseCase(get()) }

    // Preference Use Cases
    single { GetSecurityConfirmedUseCase(get()) }
    single { SetSecurityConfirmedUseCase(get()) }
}
