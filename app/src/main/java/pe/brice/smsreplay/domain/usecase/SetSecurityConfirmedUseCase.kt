package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.repository.PreferenceRepository

/**
 * Use case for setting security confirmation status
 */
class SetSecurityConfirmedUseCase(
    private val preferenceRepository: PreferenceRepository
) {
    suspend operator fun invoke(confirmed: Boolean) {
        preferenceRepository.setSecurityConfirmed(confirmed)
    }
}
