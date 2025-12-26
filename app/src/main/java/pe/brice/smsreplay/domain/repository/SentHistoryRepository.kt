package pe.brice.smsreplay.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.brice.smsreplay.domain.model.SentHistory

/**
 * Repository interface for sent email history
 */
interface SentHistoryRepository {

    /**
     * Get all sent history (last 30 days)
     */
    fun getAllHistory(): Flow<List<SentHistory>>

    /**
     * Search history by keyword (sender or body)
     */
    fun searchHistory(keyword: String): Flow<List<SentHistory>>

    /**
     * Add a new sent history record
     */
    suspend fun addHistory(history: SentHistory): Long

    /**
     * Delete history by ID
     */
    suspend fun deleteHistory(id: Long)

    /**
     * Delete all history
     */
    suspend fun deleteAllHistory(): Int

    /**
     * Delete old records (older than 30 days)
     * Called automatically when adding new history
     */
    suspend fun deleteOldRecords(): Int

    /**
     * Get total count
     */
    fun getCount(): Flow<Int>
}
