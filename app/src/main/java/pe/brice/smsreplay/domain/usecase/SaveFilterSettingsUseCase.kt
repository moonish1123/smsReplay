package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.model.FilterSettings
import pe.brice.smsreplay.domain.repository.FilterRepository

/**
 * Use case for saving filter settings
 */
class SaveFilterSettingsUseCase(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(settings: FilterSettings) {
        filterRepository.saveFilterSettings(settings)
    }
}
