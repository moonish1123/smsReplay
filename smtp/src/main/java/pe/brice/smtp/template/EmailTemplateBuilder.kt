package pe.brice.smtp.template

/**
 * HTML Email Template Builder
 * Creates card-style email templates that resemble mobile SMS screens
 */
object EmailTemplateBuilder {

    /**
     * Generate HTML email template from SMS data
     * Design: Card-style layout similar to phone SMS screens
     */
    fun buildSmsTemplate(
        sender: String,
        body: String,
        timestamp: String
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SMS from $sender</title>
                <style>
                    /* Reset and Base Styles */
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background-color: #f5f5f5;
                        padding: 20px;
                        line-height: 1.6;
                    }

                    /* Container */
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                    }

                    /* Card Container */
                    .sms-card {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        border-radius: 20px;
                        padding: 2px;
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
                        margin-bottom: 20px;
                    }

                    /* Card Inner */
                    .card-inner {
                        background-color: #ffffff;
                        border-radius: 18px;
                        padding: 24px;
                    }

                    /* Header Section */
                    .card-header {
                        display: flex;
                        align-items: center;
                        margin-bottom: 20px;
                        padding-bottom: 16px;
                        border-bottom: 1px solid #f0f0f0;
                    }

                    /* Avatar */
                    .avatar {
                        width: 48px;
                        height: 48px;
                        border-radius: 50%%;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 20px;
                        font-weight: bold;
                        color: #ffffff;
                        margin-right: 16px;
                        flex-shrink: 0;
                    }

                    /* Sender Info */
                    .sender-info {
                        flex: 1;
                    }

                    .sender-name {
                        font-size: 18px;
                        font-weight: 600;
                        color: #1a1a1a;
                        margin-bottom: 4px;
                    }

                    .timestamp {
                        font-size: 14px;
                        color: #999999;
                    }

                    /* Message Bubble */
                    .message-bubble {
                        background-color: #f0f0f0;
                        border-radius: 18px;
                        padding: 16px 20px;
                        position: relative;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                    }

                    .message-bubble::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: -8px;
                        width: 0;
                        height: 0;
                        border-top: 12px solid #f0f0f0;
                        border-left: 12px solid transparent;
                    }

                    .message-text {
                        font-size: 16px;
                        color: #1a1a1a;
                        white-space: pre-wrap;
                        word-break: break-word;
                        line-height: 1.5;
                    }

                    /* Footer */
                    .card-footer {
                        margin-top: 16px;
                        padding-top: 16px;
                        border-top: 1px solid #f0f0f0;
                        text-align: center;
                    }

                    .footer-text {
                        font-size: 12px;
                        color: #999999;
                    }

                    /* Responsive Design */
                    @media only screen and (max-width: 600px) {
                        body {
                            padding: 10px;
                        }

                        .card-inner {
                            padding: 20px;
                        }

                        .sender-name {
                            font-size: 16px;
                        }

                        .message-text {
                            font-size: 15px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="sms-card">
                        <div class="card-inner">
                            <!-- Header: Avatar and Sender Info -->
                            <div class="card-header">
                                <div class="avatar">
                                    ${escapeHtml(sender.first().uppercaseChar().toString())}
                                </div>
                                <div class="sender-info">
                                    <div class="sender-name">${escapeHtml(sender)}</div>
                                    <div class="timestamp">$timestamp</div>
                                </div>
                            </div>

                            <!-- Message Bubble -->
                            <div class="message-bubble">
                                <div class="message-text">${escapeHtml(body)}</div>
                            </div>

                            <!-- Footer -->
                            <div class="card-footer">
                                <div class="footer-text">
                                    SMS Forwarding Service
                                </div>
                            </div>
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
        timestamp: String
    ): String {
        return """
            SMS from $sender ($timestamp)

            $body

            ---
            Sent by SMS Forwarding Service
        """.trimIndent()
    }
}
