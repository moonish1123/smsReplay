package pe.brice.smsreplay.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for sent email history
 * Stores successfully sent SMS-to-Email records
 */
@Entity(
    tableName = "sent_history",
    indices = [
        Index(value = ["timestamp"]), // For date-based queries
        Index(value = ["sender"]), // For sender filtering
        Index(value = ["body"]) // For full-text search
    ]
)
data class SentHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // SMS Information
    val sender: String, // 발신자 번호
    val body: String, // SMS 본문
    val timestamp: Long, // 수신 시간 (epoch milliseconds)

    // Email Information
    val recipientEmail: String, // 수신자 이메일
    val senderEmail: String, // 발신자 이메일 (SMTP)

    // Status
    val sentAt: Long, // 전송 완료 시간 (epoch milliseconds)
    val retryCount: Int = 0 // 재시도 횟수
) {
    companion object {
        // Constants
        const val MAX_HISTORY_DAYS = 30L // 최대 30일 보관
        const val MAX_HISTORY_SIZE = 1000 // 최대 1000개 보관
    }
}
