package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import pe.brice.smsreplay.data.datastore.SecurePreferencesManager
import pe.brice.smsreplay.data.model.SmtpConfigData
import pe.brice.smsreplay.domain.model.SmtpConfig
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository
import timber.log.Timber

/**
 * Repository implementation for SMTP configuration
 * Bridges Data Layer and Domain Layer
 *
 * Uses in-memory cache with refresh on save and subscription
 */
class SmtpConfigRepositoryImpl(
    private val securePreferencesManager: SecurePreferencesManager
) : SmtpConfigRepository {

    // In-memory cache
    private val cachedConfig = MutableStateFlow<SmtpConfig?>(null)

    init {
        // Load config immediately when repository is created
        // This runs in the background since we're using Koin singleton
        CoroutineScope(Dispatchers.IO).launch {
            refreshFromStorage()
        }
    }

    /**
     * Get SMTP config as Flow
     * Always emits the cached value first, then refreshes on subscription
     */
    override fun getSmtpConfig(): Flow<SmtpConfig?> {
        return cachedConfig
            .onSubscription {
                // Refresh when someone subscribes to ensure fresh data
                refreshFromStorage()
            }
    }

    /**
     * Save SMTP config
     * Converts Domain Model to Data Model
     * Updates cache after saving
     */
    override suspend fun saveSmtpConfig(config: SmtpConfig) {
        securePreferencesManager.saveSmtpConfig(config.toDataModel())
        refreshFromStorage()
    }

    /**
     * Clear SMTP config
     */
    override suspend fun clearSmtpConfig() {
        securePreferencesManager.clearSmtpConfig()
        cachedConfig.value = null
    }

    /**
     * Check if config exists
     */
    override suspend fun hasConfig(): Boolean {
        return securePreferencesManager.hasSmtpConfig()
    }

    /**
     * Refresh config from storage and update cache
     */
    private suspend fun refreshFromStorage() {
        try {
            val configData = securePreferencesManager.getSmtpConfig()
            val newConfig = if (configData.serverAddress.isNotBlank() &&
                                 configData.username.isNotBlank()) {
                configData.toDomainModel()
            } else {
                null
            }
            cachedConfig.value = newConfig
            Timber.e("SMTP config refreshed: server=${configData.serverAddress}, username=${configData.username}, isConfigured=${newConfig != null}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh SMTP config")
        }
    }

    /**
     * Mapper: Data Model → Domain Model
     */
    private fun SmtpConfigData.toDomainModel(): SmtpConfig {
        return SmtpConfig(
            serverAddress = serverAddress,
            port = port,
            username = username,
            password = password,
            senderEmail = senderEmail,
            recipientEmail = recipientEmail
        )
    }

    /**
     * Mapper: Domain Model → Data Model
     */
    private fun SmtpConfig.toDataModel(): SmtpConfigData {
        return SmtpConfigData(
            serverAddress = serverAddress,
            port = port,
            username = username,
            password = password,
            senderEmail = senderEmail,
            recipientEmail = recipientEmail
        )
    }
}
