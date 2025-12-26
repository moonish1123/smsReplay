package pe.brice.smsreplay.data.repository

import pe.brice.smsreplay.data.datastore.SecurePreferencesManager
import pe.brice.smsreplay.domain.repository.PreferenceRepository

/**
 * Implementation of PreferenceRepository
 * Uses SecurePreferencesManager for persistent storage
 */
class PreferenceRepositoryImpl(
    private val securePreferencesManager: SecurePreferencesManager
) : PreferenceRepository {

    override suspend fun setSecurityConfirmed(confirmed: Boolean) {
        securePreferencesManager.setSecurityConfirmed(confirmed)
    }

    override suspend fun isSecurityConfirmed(): Boolean {
        return securePreferencesManager.isSecurityConfirmed()
    }
}
