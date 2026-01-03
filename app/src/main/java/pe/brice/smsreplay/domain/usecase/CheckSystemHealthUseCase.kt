package pe.brice.smsreplay.domain.usecase

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import pe.brice.smsreplay.service.SmsForegroundService

/**
 * UseCase to diagnose system health and readiness for SMS monitoring
 * Checks permissions, battery optimization, service status, and SMTP config
 */
class CheckSystemHealthUseCase(
    private val context: Context,
    private val getSmtpConfigUseCase: GetSmtpConfigUseCase
) {
    suspend operator fun invoke(): List<SystemIssue> {
        val issues = mutableListOf<SystemIssue>()

        // 1. Check SMS Permissions (Critical)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
        ) {
            issues.add(SystemIssue.MissingSmsPermission)
        }

        // 2. Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                issues.add(SystemIssue.MissingNotificationPermission)
            }
        }

        // 3. Check Battery Optimization (Critical for background work)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                issues.add(SystemIssue.BatteryOptimizationEnabled)
            }
        }

        // 4. Check SMTP Configuration
        val smtpConfig = getSmtpConfigUseCase().first()
        if (smtpConfig == null || !smtpConfig.isValid()) {
            issues.add(SystemIssue.InvalidSmtpConfig)
        }

        // 5. Check Service Status
        // Only check if everything else is fine, because service won't start without permissions/config
        if (issues.isEmpty()) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isRunning = am.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == SmsForegroundService::class.java.name }
            
            if (!isRunning) {
                issues.add(SystemIssue.ServiceNotRunning)
            }
        }

        return issues
    }

    /**
     * Sealed class representing possible system issues
     */
    sealed class SystemIssue(val message: String, val actionLabel: String) {
        object MissingSmsPermission : SystemIssue("SMS 권한이 필요합니다.", "권한 허용")
        object MissingNotificationPermission : SystemIssue("알림 권한이 필요합니다.", "권한 허용")
        object BatteryOptimizationEnabled : SystemIssue("배터리 최적화 해제가 필요합니다.", "설정하기")
        object InvalidSmtpConfig : SystemIssue("SMTP 설정이 필요합니다.", "설정하기")
        object ServiceNotRunning : SystemIssue("모니터링 서비스가 중지되었습니다.", "서비스 시작")
    }
}
