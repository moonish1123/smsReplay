package pe.brice.smsreplay.presentation.smtp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import pe.brice.smsreplay.presentation.smtp.SmtpSettingsViewModel
import android.widget.Toast

/**
 * SMTP Settings Screen
 * Clean, intuitive design (bank app style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmtpSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmtpSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Show error dialog when connection fails
    if (uiState.errorMessage != null && !uiState.isSuccess) {
        var showErrorDialog by remember { mutableStateOf(true) }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = {
                    showErrorDialog = false
                    // Clear error message when dismissed
                    viewModel.onServerAddressChange(uiState.serverAddress)
                },
                title = { Text("연결 실패") },
                text = { Text(uiState.errorMessage ?: "알 수 없는 오류가 발생했습니다") },
                confirmButton = {
                    TextButton(onClick = {
                        showErrorDialog = false
                        // Clear error message
                        viewModel.onServerAddressChange(uiState.serverAddress)
                    }) {
                        Text("확인")
                    }
                }
            )
        }
    }

    // Navigate back and show toast on success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "SMTP 설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMTP 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            InfoCard()

            // Server Address
            OutlinedTextField(
                value = uiState.serverAddress,
                onValueChange = { viewModel.onServerAddressChange(it) },
                label = { Text("SMTP 서버 주소") },
                placeholder = { Text("smtp.gmail.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.showErrors && uiState.serverAddress.isBlank(),
                supportingText = {
                    if (uiState.showErrors && uiState.serverAddress.isBlank()) {
                        Text("서버 주소를 입력하세요", color = Color.Red)
                    } else {
                        Text("예: smtp.gmail.com")
                    }
                }
            )

            // Port
            OutlinedTextField(
                value = uiState.port,
                onValueChange = { viewModel.onPortChange(it) },
                label = { Text("포트") },
                placeholder = { Text("587") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = uiState.showErrors && uiState.port.isBlank()
            )

            // Username
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("SMTP ID / 이메일") },
                placeholder = { Text("your-email@gmail.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.showErrors && uiState.username.isBlank()
            )

            // Password
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("SMTP 비밀번호") },
                placeholder = { Text("••••••••") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Close else Icons.Default.Lock,
                            contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                        )
                    }
                },
                isError = uiState.showErrors && uiState.password.isBlank()
            )

            // Sender Email (자동 생성됨)
            val senderEmail = viewModel.getSenderEmail()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "발신자 이메일 (자동 생성)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (senderEmail.isNotBlank()) {
                            senderEmail
                        } else {
                            "SMTP ID와 서버 주소를 입력하면 자동으로 생성됩니다"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (senderEmail.isNotBlank()) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            // Recipient Email
            OutlinedTextField(
                value = uiState.recipientEmail,
                onValueChange = { viewModel.onRecipientEmailChange(it) },
                label = { Text("수신자 이메일") },
                placeholder = { Text("recipient@example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.showErrors && uiState.recipientEmail.isBlank()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("연결 테스트 중...")
                } else {
                    Text("저장")
                }
            }
        }
    }
}

@Composable
fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ℹ️ SMTP 설정 정보",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Gmail: smtp.gmail.com:587 (TLS)\nNaver: smtp.naver.com:587 (TLS)\nDaum: smtp.daum.net:465 (SSL)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "⚠️ Naver/Daum은 앱 비밀번호를 생성해야 합니다\n(일반 비밀번호 사용 불가)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
