package pe.brice.smsreplay.data.service

import pe.brice.smtp.template.EmailTemplateBuilder
import pe.brice.smsreplay.domain.service.EmailTemplateService

/**
 * Implementation of EmailTemplateService using SMTP module's template builder
 *
 * Clean Architecture: Data Layer can depend on external libraries and frameworks.
 * This class bridges between the Domain Layer interface and the SMTP module implementation.
 *
 * @see pe.brice.smsreplay.domain.service.EmailTemplateService
 */
class EmailTemplateServiceImpl : EmailTemplateService {

    /**
     * Build HTML email template for SMS forwarding
     *
     * Delegates to the SMTP module's EmailTemplateBuilder for actual template generation.
     * The SMTP module handles the HTML formatting and styling.
     */
    override fun buildSmsTemplate(
        sender: String,
        body: String,
        timestamp: String,
        subject: String,
        deviceAlias: String
    ): String {
        return EmailTemplateBuilder.buildSmsTemplate(
            sender = sender,
            body = body,
            timestamp = timestamp,
            subject = subject,
            deviceAlias = deviceAlias
        )
    }
}
