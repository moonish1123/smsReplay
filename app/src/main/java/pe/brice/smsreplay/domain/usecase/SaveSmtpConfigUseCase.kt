package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.model.SmtpConfig
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository

/**
 * Use case for saving SMTP config
 */
class SaveSmtpConfigUseCase(
    private val smtpConfigRepository: SmtpConfigRepository
) {
    suspend operator fun invoke(config: SmtpConfig) {
        smtpConfigRepository.saveSmtpConfig(config)
    }
}
