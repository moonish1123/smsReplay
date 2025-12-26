package pe.brice.smsreplay.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import pe.brice.smsreplay.presentation.main.MainViewModel

/**
 * Main Screen
 * Service control, settings navigation, status display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSmtpSettings: () -> Unit,
    onNavigateToFilterSettings: () -> Unit,
    onNavigateToSentHistory: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    allPermissionsGranted: Boolean = false,
    permissionsDenied: Boolean = false,
    viewModel: MainViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Security Confirmation Dialog
    if (uiState.showSecurityDialog) {
        var userInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.cancelStartService() },
            title = {
                Text(
                    text = "⚠️ 보안 확인",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "악의적인 목적으로 문자를 전송할 수 있음을 인지하고 있고 자의로 문자 전달 기능을 사용합니다. 사용 중 발생한 책임은 본인에게 있습니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "계속하려면 '확인'을 입력하세요:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it.trim() },  // 자동으로 공백 제거
                        placeholder = { Text("확인") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (userInput == "확인") {
                            viewModel.confirmStartService()
                        }
                    },
                    enabled = userInput == "확인"
                ) {
                    Text("시작하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelStartService() }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Replay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Request Card
            if (!allPermissionsGranted) {
                item {
                    PermissionRequestCard(
                        onRequestPermissions = onRequestPermissions,
                        onOpenAppSettings = onOpenAppSettings
                    )
                }
            }

            // Service Status Card
            item {
                ServiceStatusCard(
                    isServiceRunning = uiState.isServiceRunning,
                    isConfigured = uiState.isConfigured,
                    queueSize = uiState.queueSize,
                    onStartService = { viewModel.startService() },
                    onStopService = { viewModel.stopService() },
                    onOpenBatteryOptimization = onOpenBatteryOptimization
                )
            }

            // Settings Section
            item {
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // SMTP Settings Card
            item {
                SettingsCard(
                    title = "SMTP 설정",
                    description = if (uiState.isConfigured) "설정됨" else "미설정",
                    icon = Icons.Default.Email,
                    onClick = onNavigateToSmtpSettings
                )
            }

            // Filter Settings Card
            item {
                SettingsCard(
                    title = "필터 설정",
                    description = "발신자 번호, 본문 키워드",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToFilterSettings
                )
            }

            // Sent History Card
            item {
                SettingsCard(
                    title = "발송 내역",
                    description = "지난 30일 전송 내역",
                    icon = Icons.Default.Email,
                    onClick = onNavigateToSentHistory
                )
            }

            // Queue Status Card
            item {
                QueueStatusCard(
                    queueSize = uiState.queueSize
                )
            }
        }
    }
}

@Composable
fun PermissionRequestCard(
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "권한 필요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "SMS 수신 및 전송을 위해 다음 권한이 필요합니다:\n• SMS 수신/읽기\n• 알림 표시 (Android 13+)",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("권한 요청")
                }

                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("설정 열기")
                }
            }
        }
    }
}

@Composable
fun ServiceStatusCard(
    isServiceRunning: Boolean,
    isConfigured: Boolean,
    queueSize: Int,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenBatteryOptimization: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Service Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "서비스 상태",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isServiceRunning) "실행 중" else "중지됨",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isServiceRunning) Color.Green else Color.Gray
                )
            }

            // SMTP Configuration Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SMTP 설정")
                Text(
                    text = if (isConfigured) "✅ 설정됨" else "❌ 미설정",
                    color = if (isConfigured) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Battery Optimization Warning
            if (isServiceRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "배터리 최적화를 끄면 서비스가 안정적으로 동작합니다\n(앱 설정 → 배터리 → 제한없음)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenBatteryOptimization) {
                        Text("앱 설정")
                    }
                }
            }

            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartService,
                    enabled = !isServiceRunning && isConfigured,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isServiceRunning) "실행 중" else "시작")
                }

                Button(
                    onClick = onStopService,
                    enabled = isServiceRunning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("중지")
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QueueStatusCard(queueSize: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "대기열 상태",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("대기 중인 SMS")
                Text(
                    text = "$queueSize 개",
                    fontWeight = FontWeight.Bold,
                    color = if (queueSize > 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            if (queueSize > 0) {
                Text(
                    text = "네트워크 연결 시 자동 전송됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
