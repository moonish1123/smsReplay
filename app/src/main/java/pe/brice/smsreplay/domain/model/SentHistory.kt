package pe.brice.smsreplay.domain.model

/**
 * Domain model for sent email history
 */
data class SentHistory(
    val id: Long = 0,
    val sender: String, // 발신자 번호
    val body: String, // SMS 본문
    val timestamp: Long, // SMS 수신 시간
    val recipientEmail: String, // 수신자 이메일
    val senderEmail: String, // 발신자 이메일
    val sentAt: Long, // 이메일 전송 완료 시간
    val retryCount: Int = 0 // 재시도 횟수
) {
    companion object {
        // Constants
        const val MAX_HISTORY_DAYS = 30L // 최대 30일 보관
    }
}
