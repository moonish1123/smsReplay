package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.brice.smsreplay.data.dao.SentHistoryDao
import pe.brice.smsreplay.data.local.database.SentHistoryEntity
import pe.brice.smsreplay.domain.model.SentHistory
import pe.brice.smsreplay.domain.repository.SentHistoryRepository
import java.util.concurrent.TimeUnit

/**
 * Repository implementation for sent email history
 */
class SentHistoryRepositoryImpl(
    private val sentHistoryDao: SentHistoryDao
) : SentHistoryRepository {

    override fun getAllHistory(): Flow<List<SentHistory>> {
        // Get last 30 days only
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(SentHistory.MAX_HISTORY_DAYS)
        return sentHistoryDao.getHistoryByDateRange(cutoffTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchHistory(keyword: String): Flow<List<SentHistory>> {
        return if (keyword.isBlank()) {
            getAllHistory()
        } else {
            sentHistoryDao.searchHistory(keyword).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }
    }

    override suspend fun addHistory(history: SentHistory): Long {
        // First, delete old records (keep database clean)
        deleteOldRecords()

        // Then insert new record
        val entity = history.toEntity()
        return sentHistoryDao.insert(entity)
    }

    override suspend fun deleteHistory(id: Long) {
        sentHistoryDao.deleteById(id)
    }

    override suspend fun deleteAllHistory(): Int {
        return sentHistoryDao.deleteAll()
    }

    override suspend fun deleteOldRecords(): Int {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(SentHistory.MAX_HISTORY_DAYS)
        return sentHistoryDao.deleteOldRecords(cutoffTime)
    }

    override fun getCount(): Flow<Int> {
        return sentHistoryDao.getCount()
    }
}

// Extension functions for mapping
private fun SentHistoryEntity.toDomainModel(): SentHistory {
    return SentHistory(
        id = id,
        sender = sender,
        body = body,
        timestamp = timestamp,
        recipientEmail = recipientEmail,
        senderEmail = senderEmail,
        sentAt = sentAt,
        retryCount = retryCount
    )
}

private fun SentHistory.toEntity(): SentHistoryEntity {
    return SentHistoryEntity(
        id = id,
        sender = sender,
        body = body,
        timestamp = timestamp,
        recipientEmail = recipientEmail,
        senderEmail = senderEmail,
        sentAt = sentAt,
        retryCount = retryCount
    )
}
