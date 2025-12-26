package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.repository.SentHistoryRepository

/**
 * Use case for deleting sent history
 */
class DeleteSentHistoryUseCase(
    private val repository: pe.brice.smsreplay.domain.repository.SentHistoryRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteHistory(id)
    }

    suspend fun deleteAll(): Int {
        return repository.deleteAllHistory()
    }
}
