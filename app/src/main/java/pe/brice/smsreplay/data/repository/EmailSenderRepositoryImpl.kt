package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.brice.smtp.model.Email
import pe.brice.smtp.sender.MailSender
import pe.brice.smtp.sender.SmtpException
import pe.brice.smsreplay.domain.model.EmailMessage
import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.repository.EmailSenderRepository
import pe.brice.smtp.template.EmailTemplateBuilder
import pe.brice.smsreplay.domain.usecase.AddSentHistoryUseCase
import timber.log.Timber

/**
 * Repository implementation for email sending
 * Bridges SMTP module and Domain Layer
 */
class EmailSenderRepositoryImpl(
    private val getSmtpConfigUseCase: pe.brice.smsreplay.domain.usecase.GetSmtpConfigUseCase,
    private val addSentHistoryUseCase: AddSentHistoryUseCase
) : EmailSenderRepository {

    /**
     * Send email
     * Creates MailSender and sends email
     */
    override suspend fun sendEmail(email: EmailMessage): SendingResult = withContext(Dispatchers.IO) {
        try {
            // 1. Get SMTP config
            val smtpConfig = getSmtpConfigUseCase().firstOrNull()
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

            // 4. Create Email DTO
            val emailDto = Email(
                from = smtpConfig.senderEmail,  // SMTP 설정의 발신자 이메일 사용
                to = smtpConfig.recipientEmail, // SMTP 설정의 수신자 이메일 사용
                subject = email.subject,
                htmlContent = email.htmlContent
            )

            // 5. Send email
            val result = mailSender.sendEmail(emailDto)

            // 6. Convert Result to SendingResult
            result.fold(
                onSuccess = {
                    // 이메일 전송 성공 - 히스토리 저장
                    try {
                        val history = pe.brice.smsreplay.domain.model.SentHistory(
                            sender = smtpConfig.username, // 발신자 (SMTP ID)
                            body = email.subject, // 이메일 제목 (발신자 번호 포함)
                            timestamp = email.timestamp,
                            recipientEmail = smtpConfig.recipientEmail,
                            senderEmail = smtpConfig.senderEmail,
                            sentAt = System.currentTimeMillis(),
                            retryCount = 0
                        )
                        addSentHistoryUseCase(history)
                    } catch (e: Exception) {
                        // 히스토리 저장 실패해도 이메일 전송은 성공한 것으로 처리
                        // 로그만 남기고 계속 진행
                        CoroutineScope(Dispatchers.IO).launch {
                            Timber.e(e, "Failed to save sent history")
                        }
                    }
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
            SendingResult.Failure(
                SendingResult.ErrorType.UNKNOWN_ERROR,
                e.message ?: "Unexpected error"
            )
        }
    }

    /**
     * Test SMTP connection
     */
    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val smtpConfig = getSmtpConfigUseCase().firstOrNull()
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
