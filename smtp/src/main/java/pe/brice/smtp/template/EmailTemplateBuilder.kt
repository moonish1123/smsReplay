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
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="color-scheme" content="light dark">
                <meta name="supported-color-schemes" content="light dark">
                <style>
                    :root {
                        color-scheme: light dark;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background-color: #f5f5f5;
                        padding: 20px;
                        line-height: 1.6;
                        margin: 0;
                        color: #333333;
                    }

                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
                        overflow: hidden;
                    }

                    .email-header {
                        background-color: #FEE500; /* Kakao Yellow */
                        padding: 20px 24px;
                        color: #191919; /* Black text */
                        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
                    }

                    .email-body {
                        padding: 24px;
                    }

                    .info-row {
                        margin-bottom: 12px;
                        font-size: 14px;
                    }

                    .info-label {
                        font-weight: 700;
                        color: #555555;
                        margin-right: 8px;
                    }

                    .info-value {
                        color: #191919;
                    }

                    .divider {
                        border: none;
                        border-top: 1px solid #eeeeee;
                        margin: 20px 0;
                    }

                    .message-content {
                        white-space: pre-wrap;
                        word-break: break-word;
                        color: #191919;
                        font-size: 16px;
                        line-height: 1.6;
                        background-color: #f9f9f9;
                        padding: 16px;
                        border-radius: 8px;
                        border: 1px solid #eeeeee;
                    }

                    .footer {
                        margin-top: 20px;
                        padding-top: 16px;
                        border-top: 1px solid #eeeeee;
                        text-align: center;
                        font-size: 12px;
                        color: #999999;
                    }

                    .ad-space {
                        margin-top: 20px;
                        height: 100px;
                        background-color: #f0f0f0;
                        border-radius: 4px;
                        ${if (showAd) "" else "display: none;"}
                    }

                    /* Dark Mode Styles */
                    @media (prefers-color-scheme: dark) {
                        body {
                            background-color: #121212;
                            color: #e0e0e0;
                        }
                        
                        .email-container {
                            background-color: #1E1E1E;
                            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                        }
                        
                        .email-header {
                            background-color: #FEE500; /* Keep yellow for identity, or use darker yellow #C6B300 */
                            color: #191919; /* Always black on yellow */
                            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
                        }
                        
                        .info-label {
                            color: #aaaaaa;
                        }
                        
                        .info-value {
                            color: #ffffff;
                        }
                        
                        .divider, .footer {
                            border-color: #333333;
                        }
                        
                        .message-content {
                            background-color: #2C2C2C;
                            color: #e0e0e0;
                            border-color: #333333;
                        }
                        
                        .ad-space {
                            background-color: #2C2C2C;
                        }
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
                            <span class="info-label">수신 단말:</span>
                            <span class="info-value"><strong>${escapeHtml(deviceAlias)}</strong></span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">보낸사람:</span>
                            <span class="info-value">${escapeHtml(sender)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">일시:</span>
                            <span class="info-value">$timestamp</span>
                        </div>
                        <hr class="divider">
                        <div class="message-content">${escapeHtml(body)}</div>

                        <div class="ad-space"></div>

                        <div class="footer">
                            SMS Replay Service
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
