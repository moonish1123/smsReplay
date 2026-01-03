package pe.brice.smsreplay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Sent Email History
 */
@Dao
interface SentHistoryDao {

    /**
     * Insert sent history record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: pe.brice.smsreplay.data.local.database.SentHistoryEntity): Long

    /**
     * Get all history sorted by sent time (newest first)
     */
    @Query("SELECT * FROM sent_history ORDER BY sentAt DESC")
    fun getAllHistory(): Flow<List<pe.brice.smsreplay.data.local.database.SentHistoryEntity>>

    /**
     * Get history by date range (sentAt timestamp)
     * Used for showing last 30 days
     */
    @Query("SELECT * FROM sent_history WHERE sentAt >= :startTime ORDER BY sentAt DESC")
    fun getHistoryByDateRange(startTime: Long): Flow<List<pe.brice.smsreplay.data.local.database.SentHistoryEntity>>

    /**
     * Search history by keyword (sender or body)
     * Used for search functionality
     */
    @Query("""
        SELECT * FROM sent_history
        WHERE sender LIKE '%' || :keyword || '%'
        OR body LIKE '%' || :keyword || '%'
        ORDER BY sentAt DESC
    """)
    fun searchHistory(keyword: String): Flow<List<pe.brice.smsreplay.data.local.database.SentHistoryEntity>>

    /**
     * Get history by specific date (day granularity)
     */
    @Query("""
        SELECT * FROM sent_history
        WHERE sentAt >= :dayStart AND sentAt < :dayEnd
        ORDER BY sentAt DESC
    """)
    fun getHistoryByDay(dayStart: Long, dayEnd: Long): Flow<List<pe.brice.smsreplay.data.local.database.SentHistoryEntity>>

    /**
     * Delete old records (older than 30 days)
     * Called automatically on insert
     */
    @Query("DELETE FROM sent_history WHERE sentAt < :cutoffTime")
    suspend fun deleteOldRecords(cutoffTime: Long): Int

    /**
     * Delete all records (user action)
     */
    @Query("DELETE FROM sent_history")
    suspend fun deleteAll(): Int

    /**
     * Get total count
     */
    @Query("SELECT COUNT(*) FROM sent_history")
    fun getCount(): Flow<Int>

    /**
     * Delete record by ID
     */
    @Query("DELETE FROM sent_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
