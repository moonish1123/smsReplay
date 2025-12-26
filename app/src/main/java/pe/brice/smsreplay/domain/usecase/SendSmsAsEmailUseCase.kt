package pe.brice.smsreplay.domain.usecase

import pe.brice.smtp.model.Email
import pe.brice.smtp.sender.MailSender
import pe.brice.smsreplay.domain.model.EmailMessage
import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.repository.EmailSenderRepository
import pe.brice.smtp.template.EmailTemplateBuilder

/**
 * Use case for sending SMS as email
 * Single responsibility: Validate filters and send email
 */
class SendSmsAsEmailUseCase(
    private val emailSenderRepository: EmailSenderRepository,
    private val getFilterSettingsUseCase: GetFilterSettingsUseCase
) {
    /**
     * Execute use case
     * @param sms SMS message to send
     * @return SendingResult
     */
    suspend operator fun invoke(sms: SmsMessage): SendingResult {
        // 1. Validate SMS
        if (!sms.isValid()) {
            return SendingResult.Failure(
                SendingResult.ErrorType.UNKNOWN_ERROR,
                "Invalid SMS message"
            )
        }

        // 2. Check filters
        val filters = getFilterSettingsUseCase()
        if (!filters.matches(sms.sender, sms.body)) {
            return SendingResult.Failure(
                SendingResult.ErrorType.UNKNOWN_ERROR,
                "SMS does not match filter criteria"
            )
        }

        // 3. Create email
        val htmlTemplate = EmailTemplateBuilder.buildSmsTemplate(
            sender = sms.sender,
            body = sms.body,
            timestamp = sms.getFormattedTime()
        )

        val emailMessage = EmailMessage(
            from = "", // Will be set from SMTP config
            to = "",   // Will be set from SMTP config
            subject = "[FW SMS] ${sms.sender} (${sms.getFormattedTime()})",
            htmlContent = htmlTemplate,
            timestamp = sms.timestamp
        )

        // 4. Send email
        return emailSenderRepository.sendEmail(emailMessage)
    }
}
