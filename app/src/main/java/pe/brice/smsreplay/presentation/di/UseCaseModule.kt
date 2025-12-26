package pe.brice.smsreplay.presentation.di

import org.koin.dsl.module
import pe.brice.smsreplay.domain.usecase.*

/**
 * Koin DI Module for Use Cases
 */
val UseCaseModule = module {

    // SMTP & Filter Use Cases
    single { GetSmtpConfigUseCase(get()) }
    single { SaveSmtpConfigUseCase(get()) }
    single { TestSmtpConnectionUseCase(get()) }
    single { GetFilterSettingsUseCase(get()) }
    single { SaveFilterSettingsUseCase(get()) }
    single { SendSmsAsEmailUseCase(get(), get()) }

    // Sent History Use Cases
    single { GetSentHistoryUseCase(get()) }
    single { AddSentHistoryUseCase(get()) }
    single { DeleteSentHistoryUseCase(get()) }

    // Preference Use Cases
    single { GetSecurityConfirmedUseCase(get()) }
    single { SetSecurityConfirmedUseCase(get()) }
}
