package pe.brice.smsreplay.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.brice.smsreplay.data.model.FilterSettingsData

/**
 * DataStore for filter settings
 * Type-safe and async alternative to SharedPreferences
 */
class FilterSettingsDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "filter_settings")

        // Preference keys
        private val SENDER_NUMBER = stringPreferencesKey("sender_number")
        private val BODY_KEYWORD = stringPreferencesKey("body_keyword")
    }

    /**
     * Get filter settings as Flow
     */
    val filterSettings: Flow<FilterSettingsData> = context.dataStore.data.map { preferences ->
        FilterSettingsData(
            senderNumber = preferences[SENDER_NUMBER],
            bodyKeyword = preferences[BODY_KEYWORD]
        )
    }

    /**
     * Save filter settings
     */
    suspend fun saveFilterSettings(settings: FilterSettingsData) {
        context.dataStore.edit { preferences ->
            // Save empty string if null to distinguish from "not set"
            preferences[SENDER_NUMBER] = settings.senderNumber ?: ""
            preferences[BODY_KEYWORD] = settings.bodyKeyword ?: ""
        }
    }

    /**
     * Clear all filter settings
     */
    suspend fun clearFilterSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Get filter settings once (suspend function)
     */
    suspend fun getFilterSettingsOnce(): FilterSettingsData {
        var result: FilterSettingsData? = null
        context.dataStore.data.collect { preferences ->
            result = FilterSettingsData(
                senderNumber = preferences[SENDER_NUMBER]?.takeIf { it.isNotBlank() },
                bodyKeyword = preferences[BODY_KEYWORD]?.takeIf { it.isNotBlank() }
            )
        }
        return result ?: FilterSettingsData()
    }
}
