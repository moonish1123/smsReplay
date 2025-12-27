package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.flow.first
import pe.brice.smsreplay.domain.model.SmtpConfig
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository

/**
 * Use case to check if SMTP is configured
 * Encapsulates the logic for validating SMTP configuration
 */
class IsSmtpConfiguredUseCase(
    private val smtpConfigRepository: SmtpConfigRepository
) {
    /**
     * Check if SMTP is properly configured
     * @return true if config exists and is valid
     */
    suspend operator fun invoke(): Boolean {
        return try {
            val config = getCurrentConfig()
            config?.isValid() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get current SMTP config (first value from flow)
     */
    private suspend fun getCurrentConfig(): SmtpConfig? {
        // We need to get the first value from the flow
        // This is a synchronous check for use in decisions
        return smtpConfigRepository.getSmtpConfig().first()
    }
}
