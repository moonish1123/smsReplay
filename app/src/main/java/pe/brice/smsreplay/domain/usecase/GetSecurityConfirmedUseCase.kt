package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.repository.PreferenceRepository

/**
 * Use case for checking if security was confirmed
 */
class GetSecurityConfirmedUseCase(
    private val preferenceRepository: PreferenceRepository
) {
    suspend operator fun invoke(): Boolean {
        return preferenceRepository.isSecurityConfirmed()
    }
}
