package pe.brice.smsreplay.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.brice.smsreplay.data.local.database.PendingSmsEntity
import pe.brice.smsreplay.domain.model.SmsMessage

/**
 * Repository interface for SMS offline queue
 */
interface SmsQueueRepository {
    /**
     * Add SMS to queue
     */
    suspend fun enqueue(sms: SmsMessage): Long

    /**
     * Get all pending SMS as Flow
     */
    fun getQueue(): Flow<List<SmsMessage>>

    /**
     * Get all pending SMS once
     */
    suspend fun getQueueOnce(): List<SmsMessage>

    /**
     * Get all pending SMS as Entity list (for internal use)
     */
    fun getAllPendingSms(): Flow<List<PendingSmsEntity>>

    /**
     * Get next pending SMS (oldest)
     */
    suspend fun getNextPendingSms(): PendingSmsEntity?

    /**
     * Find SMS by timestamp
     */
    suspend fun findByTimestamp(timestamp: Long): PendingSmsEntity?

    /**
     * Delete SMS by ID
     */
    suspend fun delete(id: Long)

    /**
     * Update retry count
     */
    suspend fun updateRetryCount(id: Long, retryCount: Int)

    /**
     * Get queue size
     */
    suspend fun getQueueSize(): Int

    /**
     * Clear all queue
     */
    suspend fun clearQueue()
}
