package pe.brice.smsreplay.domain.repository

/**
 * Repository for app preferences and settings
 * Handles security confirmation and other persistent settings
 */
interface PreferenceRepository {
    /**
     * Set security confirmation status
     * @param confirmed true if user confirmed security warning
     */
    suspend fun setSecurityConfirmed(confirmed: Boolean)

    /**
     * Check if security was confirmed
     * @return true if user already confirmed security warning
     */
    suspend fun isSecurityConfirmed(): Boolean
}
