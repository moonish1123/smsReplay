package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.repository.EmailSenderRepository

/**
 * Use case for testing SMTP connection
 */
class TestSmtpConnectionUseCase(
    private val emailSenderRepository: EmailSenderRepository
) {
    /**
     * Test SMTP connection with current configuration
     * @return Result<Unit> success if connection works, failure otherwise
     */
    suspend operator fun invoke(): Result<Unit> {
        return emailSenderRepository.testConnection()
    }
}
