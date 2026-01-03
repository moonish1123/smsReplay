package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pe.brice.smtp.model.Email
import pe.brice.smtp.sender.MailSender
import pe.brice.smtp.sender.SmtpException
import pe.brice.smsreplay.domain.model.EmailMessage
import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository
import pe.brice.smsreplay.domain.repository.EmailSenderRepository
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Repository implementation for email sending
 * Bridges SMTP module and Domain Layer
 *
 * Clean Architecture: Only depends on Data Layer (SmtpConfigRepository)
 * History saving is handled by the calling UseCase, not here
 */
class EmailSenderRepositoryImpl(
    private val smtpConfigRepository: SmtpConfigRepository,
    private val ioDispatcher: CoroutineContext = kotlinx.coroutines.Dispatchers.IO
) : EmailSenderRepository {

    /**
     * Send email
     * Creates MailSender and sends email
     */
    override suspend fun sendEmail(email: EmailMessage): SendingResult = withContext(ioDispatcher) {
        try {
            // 1. Get SMTP config
            val smtpConfig = smtpConfigRepository.getSmtpConfig().first()
                ?: return@withContext SendingResult.Failure(
                    SendingResult.ErrorType.AUTHENTICATION_FAILED,
                    "SMTP configuration not found"
                )

            // 2. Validate config
            if (!smtpConfig.isValid()) {
                return@withContext SendingResult.Failure(
                    SendingResult.ErrorType.AUTHENTICATION_FAILED,
                    "Invalid SMTP configuration"
                )
            }

            // 3. Create MailSender
            val mailSender = MailSender(
                serverAddress = smtpConfig.serverAddress,
                port = smtpConfig.port,
                username = smtpConfig.username,
                password = smtpConfig.password
            )

            // 4. Create Email DTO using fromSms factory method
            // This creates the From header as "phone <email>" format
            val emailDto = Email.fromSms(
                sender = email.senderPhone,
                deviceAlias = email.deviceAlias,
                timestamp = email.timestamp,
                fromEmail = smtpConfig.senderEmail,
                toEmail = smtpConfig.recipientEmail,
                htmlTemplate = email.htmlContent
            )

            // 5. Send email
            val result = mailSender.sendEmail(emailDto)

            // 6. Convert Result to SendingResult
            result.fold(
                onSuccess = {
                    SendingResult.Success
                },
                onFailure = { exception ->
                    when (exception) {
                        is SmtpException -> {
                            when {
                                exception.message?.contains("Authentication", ignoreCase = true) == true ->
                                    SendingResult.Failure(SendingResult.ErrorType.AUTHENTICATION_FAILED, exception.message ?: "Auth failed")

                                exception.message?.contains("recipient", ignoreCase = true) == true ->
                                    SendingResult.Failure(SendingResult.ErrorType.INVALID_RECIPIENT, exception.message ?: "Invalid recipient")

                                else -> SendingResult.Failure(SendingResult.ErrorType.SMTP_ERROR, exception.message ?: "SMTP error")
                            }
                        }
                        else -> SendingResult.Failure(SendingResult.ErrorType.UNKNOWN_ERROR, exception.message ?: "Unknown error")
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to send email")
            SendingResult.Failure(
                SendingResult.ErrorType.UNKNOWN_ERROR,
                e.message ?: "Unexpected error"
            )
        }
    }

    /**
     * Test SMTP connection
     */
    override suspend fun testConnection(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val smtpConfig = smtpConfigRepository.getSmtpConfig().first()
                ?: return@withContext Result.failure(Exception("SMTP config not found"))

            val mailSender = MailSender(
                serverAddress = smtpConfig.serverAddress,
                port = smtpConfig.port,
                username = smtpConfig.username,
                password = smtpConfig.password
            )

            mailSender.testConnection()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
