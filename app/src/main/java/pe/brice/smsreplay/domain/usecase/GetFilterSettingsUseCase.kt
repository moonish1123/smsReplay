package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.flow.first
import pe.brice.smsreplay.domain.model.FilterSettings
import pe.brice.smsreplay.domain.repository.FilterRepository

/**
 * Use case for getting filter settings
 */
class GetFilterSettingsUseCase(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(): FilterSettings {
        return try {
            filterRepository.getFilterSettings().first()
        } catch (e: Exception) {
            FilterSettings() // Return default on error
        }
    }
}
