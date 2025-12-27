package pe.brice.smsreplay.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.brice.smsreplay.domain.model.SmtpConfig

/**
 * Repository interface for SMTP configuration
 */
interface SmtpConfigRepository {
    /**
     * Get SMTP config as Flow
     * Returns null if config is not set
     */
    fun getSmtpConfig(): Flow<SmtpConfig?>

    /**
     * Save SMTP config
     */
    suspend fun saveSmtpConfig(config: SmtpConfig)

    /**
     * Clear SMTP config
     */
    suspend fun clearSmtpConfig()

    /**
     * Check if config exists
     */
    suspend fun hasConfig(): Boolean
}
