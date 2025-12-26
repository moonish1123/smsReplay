package pe.brice.smsreplay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pe.brice.smsreplay.data.local.database.PendingSmsEntity

/**
 * Room DAO for pending SMS operations
 */
@Dao
interface PendingSmsDao {

    /**
     * Insert new pending SMS
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingSms: PendingSmsEntity): Long

    /**
     * Get all pending SMS as Flow (observable)
     * Ordered by timestamp (oldest first)
     */
    @Query("SELECT * FROM pending_sms ORDER BY timestamp ASC")
    fun getAllPendingSms(): Flow<List<PendingSmsEntity>>

    /**
     * Get all pending SMS once (not observable)
     */
    @Query("SELECT * FROM pending_sms ORDER BY timestamp ASC")
    suspend fun getAllPendingSmsOnce(): List<PendingSmsEntity>

    /**
     * Get pending SMS by ID
     */
    @Query("SELECT * FROM pending_sms WHERE id = :id")
    suspend fun getSmsById(id: Long): PendingSmsEntity?

    /**
     * Delete pending SMS by ID
     */
    @Query("DELETE FROM pending_sms WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Delete oldest SMS when queue is full
     * Returns the number of deleted rows
     */
    @Query("DELETE FROM pending_sms WHERE id IN (SELECT id FROM pending_sms ORDER BY timestamp ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int): Int

    /**
     * Update retry count
     */
    @Query("UPDATE pending_sms SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: Long, retryCount: Int): Int

    /**
     * Get count of pending SMS
     */
    @Query("SELECT COUNT(*) FROM pending_sms")
    suspend fun getCount(): Int

    /**
     * Clear all pending SMS
     */
    @Query("DELETE FROM pending_sms")
    suspend fun clearAll()

    /**
     * Transaction: Insert new SMS and enforce queue size limit
     * If queue exceeds 100 items, delete oldest entries
     */
    @Transaction
    suspend fun insertWithQueueManagement(
        pendingSms: PendingSmsEntity,
        maxQueueSize: Int = 100
    ): Long {
        val currentCount = getCount()
        val insertedId = insert(pendingSms)

        // If queue exceeds max size, delete oldest entries
        if (currentCount >= maxQueueSize) {
            val excessCount = currentCount - maxQueueSize + 1
            deleteOldest(excessCount)
        }

        return insertedId
    }

    /**
     * Get SMS that can be retried (retryCount < 3)
     */
    @Query("SELECT * FROM pending_sms WHERE retryCount < 3 ORDER BY timestamp ASC")
    suspend fun getRetryableSms(): List<PendingSmsEntity>

    /**
     * Get oldest pending SMS
     */
    @Query("SELECT * FROM pending_sms ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestPendingSms(): PendingSmsEntity?

    /**
     * Find SMS by timestamp
     */
    @Query("SELECT * FROM pending_sms WHERE timestamp = :timestamp LIMIT 1")
    suspend fun findByTimestamp(timestamp: Long): PendingSmsEntity?
}
