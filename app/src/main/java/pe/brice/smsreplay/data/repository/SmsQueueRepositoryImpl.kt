package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.brice.smsreplay.data.local.dao.PendingSmsDao
import pe.brice.smsreplay.data.local.database.PendingSmsEntity
import pe.brice.smsreplay.data.model.PendingSmsData
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.repository.SmsQueueRepository

/**
 * Repository implementation for SMS offline queue
 * Bridges Data Layer and Domain Layer
 */
class SmsQueueRepositoryImpl(
    private val pendingSmsDao: PendingSmsDao
) : SmsQueueRepository {

    companion object {
        private const val MAX_QUEUE_SIZE = 100
    }

    /**
     * Add SMS to queue
     * Converts Domain Model to Entity
     */
    override suspend fun enqueue(sms: SmsMessage): Long {
        val entity = sms.toEntity()
        return pendingSmsDao.insertWithQueueManagement(entity, MAX_QUEUE_SIZE)
    }

    /**
     * Get all pending SMS as Flow
     * Converts Entity to Domain Model
     */
    override fun getQueue(): Flow<List<SmsMessage>> {
        return pendingSmsDao.getAllPendingSms().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Get all pending SMS once
     */
    override suspend fun getQueueOnce(): List<SmsMessage> {
        return pendingSmsDao.getAllPendingSmsOnce().map { it.toDomainModel() }
    }

    /**
     * Get all pending SMS as Entity list (for internal use)
     */
    override fun getAllPendingSms(): Flow<List<PendingSmsEntity>> {
        return pendingSmsDao.getAllPendingSms()
    }

    /**
     * Get next pending SMS (oldest)
     */
    override suspend fun getNextPendingSms(): PendingSmsEntity? {
        return pendingSmsDao.getOldestPendingSms()
    }

    /**
     * Find SMS by timestamp
     */
    override suspend fun findByTimestamp(timestamp: Long): PendingSmsEntity? {
        return pendingSmsDao.findByTimestamp(timestamp)
    }

    /**
     * Delete SMS by ID
     */
    override suspend fun delete(id: Long) {
        pendingSmsDao.deleteById(id)
    }

    /**
     * Update retry count
     */
    override suspend fun updateRetryCount(id: Long, retryCount: Int) {
        pendingSmsDao.updateRetryCount(id, retryCount)
    }

    /**
     * Get queue size
     */
    override suspend fun getQueueSize(): Int {
        return pendingSmsDao.getCount()
    }

    /**
     * Clear all queue
     */
    override suspend fun clearQueue() {
        pendingSmsDao.clearAll()
    }

    /**
     * Mapper: Domain Model → Entity
     */
    private fun SmsMessage.toEntity(): PendingSmsEntity {
        return PendingSmsEntity(
            sender = sender,
            body = body,
            timestamp = timestamp,
            retryCount = 0
        )
    }

    /**
     * Mapper: Entity → Domain Model
     */
    private fun PendingSmsEntity.toDomainModel(): SmsMessage {
        return SmsMessage(
            sender = sender,
            body = body,
            timestamp = timestamp
        )
    }
}
