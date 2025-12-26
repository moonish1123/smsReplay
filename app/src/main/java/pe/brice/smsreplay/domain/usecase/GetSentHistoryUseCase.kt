package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.brice.smsreplay.domain.model.SentHistory
import pe.brice.smsreplay.domain.repository.SentHistoryRepository

/**
 * Use case for getting sent history
 */
class GetSentHistoryUseCase(
    private val repository: pe.brice.smsreplay.domain.repository.SentHistoryRepository
) {
    operator fun invoke(keyword: String = ""): Flow<List<SentHistory>> {
        return repository.searchHistory(keyword)
    }
}
