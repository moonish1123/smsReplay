package pe.brice.smsreplay.presentation.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.brice.smsreplay.presentation.filter.FilterSettingsViewModel
import pe.brice.smsreplay.presentation.history.SentHistoryViewModel
import pe.brice.smsreplay.presentation.main.MainViewModel
import pe.brice.smsreplay.presentation.smtp.SmtpSettingsViewModel

/**
 * Koin DI Module for Presentation Layer
 * Clean Architecture: ViewModels
 */
val PresentationModule = module {

    // Main Screen ViewModel
    viewModel { MainViewModel() }

    // SMTP Settings ViewModel
    viewModel { SmtpSettingsViewModel() }

    // Filter Settings ViewModel
    viewModel { FilterSettingsViewModel() }

    // Sent History ViewModel
    viewModel { SentHistoryViewModel() }
}
