package pe.brice.smsreplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import pe.brice.smsreplay.presentation.filter.FilterSettingsScreen
import pe.brice.smsreplay.presentation.history.SentHistoryScreen
import pe.brice.smsreplay.presentation.main.MainScreen
import pe.brice.smsreplay.presentation.smtp.SmtpSettingsScreen
import pe.brice.smsreplay.ui.theme.SmsReplayTheme
import androidx.core.net.toUri
import timber.log.Timber
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity(), org.koin.core.component.KoinComponent {

    private val permissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val deniedPermissions = permissionsMap.filterValues { !it }
        if (deniedPermissions.isNotEmpty()) {
            // 권한 거부됨 - 설정 화면으로 안내
            permissionsDenied.value = true
        } else {
            // 모든 권한 허용됨
            permissionsDenied.value = false
            allPermissionsGranted.value = true
        }
    }

    companion object {
        var allPermissionsGranted = mutableStateOf(false)
        var permissionsDenied = mutableStateOf(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check permissions on app start
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        allPermissionsGranted.value = allGranted

        // If permissions not granted, request immediately
        if (!allGranted) {
            permissionsDenied.value = true
            requestPermissions()
        }

        // Auto-start service if configured
        val serviceManager by inject<pe.brice.smsreplay.service.ServiceManager>()
        val permissionManager by inject<pe.brice.smsreplay.service.PermissionManager>()
        val canStartMonitoringUseCase by inject<pe.brice.smsreplay.domain.usecase.CanStartMonitoringUseCase>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isConfigured = canStartMonitoringUseCase()
                val hasPermissions = permissionManager.checkAllPermissions()

                if (isConfigured && hasPermissions && !serviceManager.isServiceRunning.value) {
                    Timber.i("Auto-starting service on app launch")
                    serviceManager.startMonitoring()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to auto-start service")
            }
        }

        setContent {
            KoinContext {
                SmsReplayApp(
                    onRequestPermissions = { requestPermissions() },
                    onOpenAppSettings = { openAppSettings() },
                    onOpenBatteryOptimization = { openBatteryOptimization() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 그려질 때 배터리 최적화 상태 및 시스템 상태 체크
        val batteryOptimizationManager by inject<pe.brice.smsreplay.service.BatteryOptimizationManager>()
        batteryOptimizationManager.checkBatteryOptimization()
        
        // Refresh ViewModel state
        val mainViewModel by inject<pe.brice.smsreplay.presentation.main.MainViewModel>()
        mainViewModel.refreshPermissions()
    }

    private fun requestPermissions() {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun openBatteryOptimization() {
        // 에뮬레이터는 배터리 최적화가 없으므로 앱 설정 화면으로 바로 이동
        if (isEmulator()) {
            Timber.d("Emulator detected, opening app settings instead")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // 먼저 배터리 최적화 예외 요청 다이얼로그 시도
                val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(batteryIntent)
                Timber.d("Battery optimization request dialog opened")
            } catch (e: Exception) {
                Timber.e(e, "Failed to open ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, trying ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
                // 실패 시 배터리 최적화 설정 화면으로 이동 (fallback)
                try {
                    val batterySettingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        data = "package:$packageName".toUri()
                    }
                    startActivity(batterySettingsIntent)
                    Timber.d("Battery optimization settings opened as fallback")
                } catch (e2: Exception) {
                    Timber.e(e2, "Failed to open battery optimization settings, trying app settings")
                    // 그래도 실패하면 앱 설정 화면으로 이동
                    try {
                        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:$packageName".toUri()
                        }
                        startActivity(appSettingsIntent)
                        Timber.d("App settings opened as last fallback")
                    } catch (e3: Exception) {
                        Timber.e(e3, "Failed to open app settings, trying general settings")
                        // 최후의 수단으로 일반 설정 화면 열기
                        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                        startActivity(settingsIntent)
                        Timber.d("General settings opened as final resort")
                    }
                }
            }
        } else {
            // Android 6.0 미만은 일반 설정 화면
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
            Timber.d("General settings opened for Android < 6.0")
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))
    }
}

@Composable
fun SmsReplayApp(
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatteryOptimization: () -> Unit
) {
    val navController = rememberNavController()
    val allPermissionsGranted by MainActivity.allPermissionsGranted
    val permissionsDenied by MainActivity.permissionsDenied

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToSmtpSettings = {
                    navController.navigate("smtp_settings")
                },
                onNavigateToFilterSettings = {
                    navController.navigate("filter_settings")
                },
                onNavigateToSentHistory = {
                    navController.navigate("sent_history")
                },
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatteryOptimization = onOpenBatteryOptimization,
                allPermissionsGranted = allPermissionsGranted,
                permissionsDenied = permissionsDenied
            )
        }

        composable("smtp_settings") {
            SmtpSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("filter_settings") {
            FilterSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("sent_history") {
            SentHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
