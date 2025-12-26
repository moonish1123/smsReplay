package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.flow.first
import pe.brice.smsreplay.domain.model.SmtpConfig
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository

/**
 * Use case for getting SMTP config
 */
class GetSmtpConfigUseCase(
    private val smtpConfigRepository: SmtpConfigRepository
) {
    suspend operator fun invoke(): SmtpConfig? {
        return try {
            val config = smtpConfigRepository.getSmtpConfig().first()
            if (config.isConfigured()) config else null
        } catch (e: Exception) {
            null
        }
    }
}
