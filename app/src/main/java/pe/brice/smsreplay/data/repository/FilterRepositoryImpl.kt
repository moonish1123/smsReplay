package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.brice.smsreplay.data.datastore.FilterSettingsDataStore
import pe.brice.smsreplay.data.model.FilterSettingsData
import pe.brice.smsreplay.domain.model.FilterSettings
import pe.brice.smsreplay.domain.repository.FilterRepository

/**
 * Repository implementation for filter settings
 * Bridges Data Layer and Domain Layer
 */
class FilterRepositoryImpl(
    private val filterSettingsDataStore: FilterSettingsDataStore
) : FilterRepository {

    /**
     * Get filter settings as Flow
     * Converts Data Model to Domain Model
     */
    override fun getFilterSettings(): Flow<FilterSettings> {
        return filterSettingsDataStore.filterSettings.map { it.toDomainModel() }
    }

    /**
     * Save filter settings
     * Converts Domain Model to Data Model
     */
    override suspend fun saveFilterSettings(settings: FilterSettings) {
        filterSettingsDataStore.saveFilterSettings(settings.toDataModel())
    }

    /**
     * Clear filter settings
     */
    override suspend fun clearFilterSettings() {
        filterSettingsDataStore.clearFilterSettings()
    }

    /**
     * Mapper: Data Model → Domain Model
     */
    private fun FilterSettingsData.toDomainModel(): FilterSettings {
        return FilterSettings(
            senderNumber = senderNumber?.takeIf { it.isNotBlank() },
            bodyKeyword = bodyKeyword?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Mapper: Domain Model → Data Model
     */
    private fun FilterSettings.toDataModel(): FilterSettingsData {
        return FilterSettingsData(
            senderNumber = senderNumber,
            bodyKeyword = bodyKeyword
        )
    }
}
