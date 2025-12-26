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
import pe.brice.smsreplay.presentation.filter.FilterSettingsScreen
import pe.brice.smsreplay.presentation.history.SentHistoryScreen
import pe.brice.smsreplay.presentation.main.MainScreen
import pe.brice.smsreplay.presentation.smtp.SmtpSettingsScreen
import pe.brice.smsreplay.ui.theme.SmsReplayTheme

class MainActivity : ComponentActivity() {

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

        setContent {
            SmsReplayApp(
                onRequestPermissions = { requestPermissions() },
                onOpenAppSettings = { openAppSettings() }
            )
        }
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
}

@Composable
fun SmsReplayApp(
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
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
