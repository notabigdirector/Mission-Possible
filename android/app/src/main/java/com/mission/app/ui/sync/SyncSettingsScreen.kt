package com.mission.app.ui.sync

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mission.app.sync.SyncConfig
import com.mission.app.ui.RegisterState
import com.mission.app.ui.SyncSettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(viewModel: SyncSettingsViewModel, onClose: () -> Unit) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()

    var serverUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    LaunchedEffect(config) {
        config?.let {
            serverUrl = it.serverUrl
            token = it.token
            userName = it.userName
        }
    }

    val scope = rememberCoroutineScope()
    var certImported by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val certPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { r -> r.readBytes() }
            if (bytes != null) {
                viewModel.importCert(bytes)
                certImported = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("同步设置") },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("完成") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "通过同步服务在多设备间共享任务数据。填同一个 token 即共享同一份数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("服务器地址") },
                placeholder = { Text("https://服务器IP:9443") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (userName.isNotBlank()) {
                            viewModel.register(serverUrl, userName)
                        }
                    },
                    enabled = userName.isNotBlank() && registerState !is RegisterState.Loading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (registerState is RegisterState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("注册并获取 token")
                    }
                }
            }
            when (val rs = registerState) {
                is RegisterState.Success -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "注册成功，用户：${rs.response.user.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is RegisterState.Error -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        rs.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("同步 token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("证书", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "默认使用内置证书。若服务器证书不同，可导入新的 cert.pem。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { certPicker.launch(arrayOf("application/x-x509-ca-cert", "text/plain", "*/*")) }) {
                Text(if (certImported) "已导入，点击重新导入证书" else "导入证书文件")
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        viewModel.save(
                            SyncConfig(
                                serverUrl = serverUrl.trim(),
                                token = token.trim(),
                                userName = userName.trim()
                            )
                        )
                    }
                },
                enabled = serverUrl.isNotBlank() && token.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存并立即同步")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            SyncStatusBlock(status)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SyncStatusBlock(status: com.mission.app.sync.SyncStatus) {
    val (text, color) = when (status.state) {
        "ok" -> "已同步" to MaterialTheme.colorScheme.primary
        "syncing" -> "同步中…" to MaterialTheme.colorScheme.primary
        "error" -> "同步出错：${status.message}" to MaterialTheme.colorScheme.error
        "offline" -> "离线" to MaterialTheme.colorScheme.error
        else -> "未同步" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    status.lastSyncAt?.let {
        Text(
            "上次同步：${com.mission.app.ui.util.Formatters.dateTime(it)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}