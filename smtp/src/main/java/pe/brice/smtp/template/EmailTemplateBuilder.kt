package pe.brice.smtp.template

/**
 * HTML Email Template Builder
 * Creates card-style email templates that resemble mobile SMS screens
 */
object EmailTemplateBuilder {

    /**
     * Generate HTML email template from SMS data
     * Design: Card-style layout similar to phone SMS screens
     * @param sender SMS sender phone number
     * @param body SMS message body
     * @param timestamp Formatted timestamp string
     * @param subject Email subject (sender + timestamp)
     * @param showAd Optional: for future monetization (Google AdSense)
     */
    fun buildSmsTemplate(
        sender: String,
        body: String,
        timestamp: String,
        subject: String,
        deviceAlias: String,
        showAd: Boolean = false
    ): String {
        return """
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <title>$subject</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f5f5f5;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #f5f5f5;">
                    <tr>
                        <td align="center" style="padding: 20px 10px;">
                            <!-- Card Container -->
                            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e0e0e0;">
                                <!-- Header -->
                                <tr>
                                    <td bgcolor="#FEE500" style="padding: 20px 24px; color: #191919; border-bottom: 1px solid #f0f0f0;">
                                        <h2 style="margin: 0; font-family: sans-serif; font-size: 20px; font-weight: bold; color: #191919;">SMS 수신 알림</h2>
                                    </td>
                                </tr>
                                
                                <!-- Body -->
                                <tr>
                                    <td style="padding: 24px;">
                                        <!-- Meta Info Table -->
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="margin-bottom: 20px;">
                                            <tr>
                                                <td width="80" style="padding-bottom: 8px; font-family: sans-serif; font-size: 14px; color: #555555; font-weight: bold;">수신 단말:</td>
                                                <td style="padding-bottom: 8px; font-family: sans-serif; font-size: 14px; color: #191919;"><strong>${escapeHtml(deviceAlias)}</strong></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-bottom: 8px; font-family: sans-serif; font-size: 14px; color: #555555; font-weight: bold;">보낸사람:</td>
                                                <td style="padding-bottom: 8px; font-family: sans-serif; font-size: 14px; color: #191919;">${escapeHtml(sender)}</td>
                                            </tr>
                                            <tr>
                                                <td style="padding-bottom: 8px; font-family: sans-serif; font-size: 14px; color: #555555; font-weight: bold;">일시:</td>
                                                <td style="padding-bottom: 8px; font-family: sans-serif; font-size: 14px; color: #191919;">$timestamp</td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Divider -->
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                            <tr>
                                                <td style="border-top: 1px solid #eeeeee; height: 1px; font-size: 0; line-height: 0;">&nbsp;</td>
                                            </tr>
                                        </table>
                                        
                                        <div style="height: 20px; font-size: 0; line-height: 0;">&nbsp;</div>
                                        
                                        <!-- Message Content Box (Bubble) -->
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                            <tr>
                                                <td style="background-color: #f9f9f9; padding: 16px; border-radius: 8px; border: 1px solid #eeeeee; font-family: sans-serif; font-size: 16px; line-height: 1.6; color: #191919; white-space: pre-wrap; word-break: break-word;">
                                                    ${escapeHtml(body)}
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Ad Space -->
                                        ${if (showAd) """
                                        <div style="height: 20px; font-size: 0; line-height: 0;">&nbsp;</div>
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                            <tr>
                                                <td height="100" bgcolor="#f0f0f0" style="border-radius: 4px;">&nbsp;</td>
                                            </tr>
                                        </table>
                                        """ else ""}
                                        
                                        <!-- Footer -->
                                        <div style="height: 20px; font-size: 0; line-height: 0;">&nbsp;</div>
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                            <tr>
                                                <td style="border-top: 1px solid #eeeeee; padding-top: 16px; text-align: center; font-family: sans-serif; font-size: 12px; color: #999999;">
                                                    SMS Replay Service
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Escape HTML to prevent XSS attacks
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("\n", "<br>")
    }

    /**
     * Build simple text template (fallback)
     */
    fun buildTextTemplate(
        sender: String,
        body: String,
        timestamp: String,
        subject: String
    ): String {
        return """
            From: $sender
            Subject: $subject
            Date: $timestamp

            Body:

            $body

            ---
            Sent by SMS Forwarding Service
        """.trimIndent()
    }
}
