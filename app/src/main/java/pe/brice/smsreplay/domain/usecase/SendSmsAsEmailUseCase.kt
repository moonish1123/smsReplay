package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.brice.smtp.template.EmailTemplateBuilder
import pe.brice.smsreplay.domain.model.EmailMessage
import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.model.SentHistory
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.repository.EmailSenderRepository
import timber.log.Timber

/**
 * Use case for sending SMS as email
 * Single responsibility: Validate filters, send email, and save history
 */
class SendSmsAsEmailUseCase(
    private val emailSenderRepository: EmailSenderRepository,
    private val getFilterSettingsUseCase: GetFilterSettingsUseCase,
    private val getSmtpConfigUseCase: GetSmtpConfigUseCase,
    private val addSentHistoryUseCase: AddSentHistoryUseCase
) {
    /**
     * Execute use case
     * @param sms SMS message to send
     * @return SendingResult
     */
    suspend operator fun invoke(sms: SmsMessage): SendingResult = withContext(Dispatchers.IO) {
        // 1. Validate SMS
        if (!sms.isValid()) {
            return@withContext SendingResult.Failure(
                SendingResult.ErrorType.UNKNOWN_ERROR,
                "Invalid SMS message"
            )
        }

        // 2. Check filters
        val filters = getFilterSettingsUseCase()
        if (!filters.matches(sms.sender, sms.body)) {
            return@withContext SendingResult.Failure(
                SendingResult.ErrorType.UNKNOWN_ERROR,
                "SMS does not match filter criteria"
            )
        }

        // 3. Get SMTP config for history
        val smtpConfig = getSmtpConfigUseCase().first()
        if (smtpConfig == null || !smtpConfig.isValid()) {
            return@withContext SendingResult.Failure(
                SendingResult.ErrorType.AUTHENTICATION_FAILED,
                "SMTP configuration not found or invalid"
            )
        }

        // 4. Create email
        val subject = "[FW SMS] ${sms.sender} (${sms.getFormattedTime()})"
        val deviceAlias = smtpConfig.deviceAlias.ifBlank { "알 수 없는 기기" }
        
        val htmlTemplate = EmailTemplateBuilder.buildSmsTemplate(
            sender = sms.sender,
            body = sms.body,
            timestamp = sms.getFormattedTime(),
            subject = subject,
            deviceAlias = deviceAlias
        )

        // Format sender address to include sender phone number as display name
        // Format: "010-1234-5678 <my-email@example.com>"
        val fromAddress = "\"${sms.sender}\" <${smtpConfig.senderEmail}>"

        val emailMessage = EmailMessage(
            from = fromAddress, 
            to = smtpConfig.recipientEmail,
            subject = subject,
            htmlContent = htmlTemplate,
            timestamp = sms.timestamp,
            senderPhone = sms.sender
        )

        // 5. Send email
        val result = emailSenderRepository.sendEmail(emailMessage)

        // 6. Save history on success (fire and forget)
        if (result is SendingResult.Success) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val history = SentHistory(
                        sender = sms.sender,
                        body = sms.body,
                        timestamp = sms.timestamp,
                        recipientEmail = smtpConfig.recipientEmail,
                        senderEmail = smtpConfig.senderEmail,
                        sentAt = System.currentTimeMillis(),
                        retryCount = 0
                    )
                    addSentHistoryUseCase(history)
                } catch (e: Exception) {
                    // History save failure shouldn't affect the result
                    Timber.e(e, "Failed to save sent history")
                }
            }
        }

        result
    }
}
