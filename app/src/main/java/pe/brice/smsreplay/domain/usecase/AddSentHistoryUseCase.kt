package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.model.SentHistory
import pe.brice.smsreplay.domain.repository.SentHistoryRepository

/**
 * Use case for adding a new sent history record
 */
class AddSentHistoryUseCase(
    private val repository: pe.brice.smsreplay.domain.repository.SentHistoryRepository
) {
    suspend operator fun invoke(history: SentHistory): Long {
        return repository.addHistory(history)
    }
}
