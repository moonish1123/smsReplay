package pe.brice.smsreplay.presentation.filter

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import pe.brice.smsreplay.presentation.filter.FilterSettingsViewModel

/**
 * Filter Settings Screen
 * AND condition filters: sender number AND body keyword
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilterSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("필터 설정") },
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
            FilterInfoCard()

            // Sender Number Filter
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "발신자 번호 필터",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = uiState.senderNumberEnabled,
                            onCheckedChange = { viewModel.toggleSenderNumberEnabled(it) }
                        )
                    }

                    if (uiState.senderNumberEnabled) {
                        OutlinedTextField(
                            value = uiState.senderNumber,
                            onValueChange = { viewModel.onSenderNumberChange(it) },
                            label = { Text("발신자 번호") },
                            placeholder = { Text("01012345678") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "특정 번호에서 온 SMS만 전송합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Body Keyword Filter
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "본문 키워드 필터",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = uiState.bodyKeywordEnabled,
                            onCheckedChange = { viewModel.toggleBodyKeywordEnabled(it) }
                        )
                    }

                    if (uiState.bodyKeywordEnabled) {
                        OutlinedTextField(
                            value = uiState.bodyKeyword,
                            onValueChange = { viewModel.onBodyKeywordChange(it) },
                            label = { Text("본문 키워드") },
                            placeholder = { Text("인증번호") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )
                    }

                    Text(
                        text = "본문에 특정 문자열이 포함된 SMS만 전송합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AND Condition Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ 필터 조건",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "두 필터는 AND 조건으로 동작합니다.\n즉, 발신자 번호와 본문 키워드가 모두 일치해야 전송합니다.\n\n필터를 비활성화하면 해당 조건이 적용되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

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
                    Text("저장 중...")
                } else {
                    Text("저장")
                }
            }

            // Success/Error Messages
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.isSuccess) {
                Text(
                    text = "저장되었습니다",
                    color = Color.Green,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun FilterInfoCard() {
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
                text = "ℹ️ 필터 설정 정보",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "필터를 설정하면 원하는 SMS만 이메일로 전송할 수 있습니다.\n\n모든 필터를 비활성화하면 모든 SMS가 전송됩니다.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
