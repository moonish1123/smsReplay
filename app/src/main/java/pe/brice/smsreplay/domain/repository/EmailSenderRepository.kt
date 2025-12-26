package pe.brice.smsreplay.domain.repository

import pe.brice.smsreplay.domain.model.EmailMessage
import pe.brice.smsreplay.domain.model.SendingResult

/**
 * Repository interface for email sending
 */
interface EmailSenderRepository {
    /**
     * Send email
     */
    suspend fun sendEmail(email: EmailMessage): SendingResult

    /**
     * Test SMTP connection
     */
    suspend fun testConnection(): Result<Unit>
}
