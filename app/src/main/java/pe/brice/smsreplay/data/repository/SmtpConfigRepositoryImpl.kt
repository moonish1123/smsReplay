package pe.brice.smsreplay.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.brice.smsreplay.data.datastore.SecurePreferencesManager
import pe.brice.smsreplay.data.model.SmtpConfigData
import pe.brice.smsreplay.domain.model.SmtpConfig
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository

/**
 * Repository implementation for SMTP configuration
 * Bridges Data Layer and Domain Layer
 */
class SmtpConfigRepositoryImpl(
    private val securePreferencesManager: SecurePreferencesManager
) : SmtpConfigRepository {

    /**
     * Get SMTP config as Flow
     * Converts Data Model to Domain Model
     */
    override fun getSmtpConfig(): Flow<SmtpConfig> {
        // Since EncryptedSharedPreferences doesn't support Flow natively,
        // we'll emit current value once
        // In a real app, you might want to use a different approach
        return kotlinx.coroutines.flow.flow {
            emit(securePreferencesManager.getSmtpConfig().toDomainModel())
        }
    }

    /**
     * Save SMTP config
     * Converts Domain Model to Data Model
     */
    override suspend fun saveSmtpConfig(config: SmtpConfig) {
        securePreferencesManager.saveSmtpConfig(config.toDataModel())
    }

    /**
     * Clear SMTP config
     */
    override suspend fun clearSmtpConfig() {
        securePreferencesManager.clearSmtpConfig()
    }

    /**
     * Check if config exists
     */
    override suspend fun hasConfig(): Boolean {
        return securePreferencesManager.hasSmtpConfig()
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
