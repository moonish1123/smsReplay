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
     * @param isAd Optional: true if this is an ad message (adds [광고] suffix)
     */
    fun buildSmsTemplate(
        sender: String,
        body: String,
        timestamp: String,
        subject: String,
        isAd: Boolean = false
    ): String {
        // Add [광고] suffix if it's an ad
        val adSuffix = if (isAd) " [광고]" else ""
        val displaySender = sender + adSuffix

        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background-color: #f5f5f5;
                        padding: 20px;
                        line-height: 1.6;
                        margin: 0;
                    }

                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                        overflow: hidden;
                    }

                    .email-header {
                        background-color: #667eea;
                        padding: 20px 24px;
                        color: #ffffff;
                    }

                    .email-body {
                        padding: 24px;
                    }

                    .info-row {
                        margin-bottom: 12px;
                        font-size: 14px;
                    }

                    .info-label {
                        font-weight: 600;
                        color: #667eea;
                        margin-right: 8px;
                    }

                    .info-value {
                        color: #333333;
                    }

                    .divider {
                        border: none;
                        border-top: 1px solid #e6e6e6;
                        margin: 20px 0;
                    }

                    .message-content {
                        white-space: pre-wrap;
                        word-break: break-word;
                        color: #1a1a1a;
                        font-size: 15px;
                        line-height: 1.6;
                    }

                    .footer {
                        margin-top: 20px;
                        padding-top: 16px;
                        border-top: 1px solid #e6e6e6;
                        text-align: center;
                        font-size: 12px;
                        color: #999999;
                    }

                    .ad-badge {
                        display: inline-block;
                        background-color: #ff6b6b;
                        color: #ffffff;
                        font-size: 11px;
                        font-weight: 600;
                        padding: 2px 8px;
                        border-radius: 4px;
                        margin-left: 6px;
                    }

                    @media only screen and (max-width: 600px) {
                        body {
                            padding: 10px;
                        }

                        .email-header, .email-body {
                            padding: 16px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="email-header">
                        <h2 style="margin: 0; font-size: 20px;">SMS 수신 알림</h2>
                    </div>
                    <div class="email-body">
                        <div class="info-row">
                            <span class="info-label">보낸사람:</span>
                            <span class="info-value">${escapeHtml(displaySender)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">제목:</span>
                            <span class="info-value">${escapeHtml(subject)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">일시:</span>
                            <span class="info-value">$timestamp</span>
                        </div>
                        <hr class="divider">
                        <div class="message-content">${escapeHtml(body)}</div>
                        <div class="footer">
                            SMS Forwarding Service
                        </div>
                    </div>
                </div>
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
