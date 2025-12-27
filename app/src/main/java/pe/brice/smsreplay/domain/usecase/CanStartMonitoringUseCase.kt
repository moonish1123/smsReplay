package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.flow.first
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository

/**
 * Use case to check if all prerequisites are met for starting the service
 * Combines SMTP config check into a single use case
 */
class CanStartMonitoringUseCase(
    private val smtpConfigRepository: SmtpConfigRepository
) {
    /**
     * Check if service can be started
     * @return true if SMTP is configured
     */
    suspend operator fun invoke(): Boolean {
        return try {
            val config = smtpConfigRepository.getSmtpConfig().first()
            config?.isValid() == true
        } catch (e: Exception) {
            false
        }
    }
}
