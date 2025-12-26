package pe.brice.smsreplay.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pe.brice.smsreplay.data.model.PendingSmsData

/**
 * Room Entity for pending SMS in offline queue
 */
@Entity(
    tableName = "pending_sms",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["retryCount"])
    ]
)
data class PendingSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sender: String,
    val body: String,
    val timestamp: Long,
    val retryCount: Int = 0
) {
    /**
     * Convert to domain model
     */
    fun toDataModel(): PendingSmsData {
        return PendingSmsData(
            id = id,
            sender = sender,
            body = body,
            timestamp = timestamp,
            retryCount = retryCount
        )
    }

    companion object {
        /**
         * Convert from domain model
         */
        fun fromDataModel(data: PendingSmsData): PendingSmsEntity {
            return PendingSmsEntity(
                id = data.id,
                sender = data.sender,
                body = data.body,
                timestamp = data.timestamp,
                retryCount = data.retryCount
            )
        }
    }
}
