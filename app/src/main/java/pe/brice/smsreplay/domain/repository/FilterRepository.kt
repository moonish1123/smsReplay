package pe.brice.smsreplay.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.brice.smsreplay.domain.model.FilterSettings

/**
 * Repository interface for filter settings
 */
interface FilterRepository {
    /**
     * Get filter settings as Flow
     */
    fun getFilterSettings(): Flow<FilterSettings>

    /**
     * Save filter settings
     */
    suspend fun saveFilterSettings(settings: FilterSettings)

    /**
     * Clear filter settings
     */
    suspend fun clearFilterSettings()
}
