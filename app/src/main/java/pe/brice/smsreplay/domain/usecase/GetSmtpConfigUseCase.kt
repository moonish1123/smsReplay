package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.brice.smsreplay.domain.model.SmtpConfig
import pe.brice.smsreplay.domain.repository.SmtpConfigRepository
import timber.log.Timber

/**
 * Use case for getting SMTP config
 */
class GetSmtpConfigUseCase(
    private val smtpConfigRepository: SmtpConfigRepository
) {
    operator fun invoke(): Flow<SmtpConfig?> {
        return smtpConfigRepository.getSmtpConfig()
            .map { config ->
                Timber.e("GetSmtpConfigUseCase: config=$config, isValid=${config?.isValid()}")
                config
            }
    }
}
