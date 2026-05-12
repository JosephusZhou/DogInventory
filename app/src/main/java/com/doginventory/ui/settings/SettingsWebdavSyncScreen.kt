package com.doginventory.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.components.AppCard
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.theme.SystemBarsStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWebdavSyncScreen(
    viewModel: SettingsWebdavSyncViewModel,
    onRestartApp: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.errorMessage, viewModel.successMessage) {
        viewModel.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
        viewModel.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessages()
        }
    }

    LaunchedEffect(viewModel.restoreCompleted) {
        if (viewModel.restoreCompleted) {
            viewModel.consumeRestoreCompleted()
            onRestartApp()
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_webdav_restore_dialog_title)) },
            text = { Text(stringResource(R.string.settings_webdav_restore_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.restoreFromRemote()
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    SystemBarsStyle(
        navigationBarColor = MaterialTheme.colorScheme.background,
        statusBarColor = MaterialTheme.colorScheme.background
    )
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_webdav_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_webdav_server_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel(stringResource(R.string.settings_webdav_server_url))
                    OutlinedTextField(
                        value = viewModel.serverUrl,
                        onValueChange = viewModel::updateServerUrl,
                        placeholder = { Text(stringResource(R.string.settings_webdav_server_url_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldLabel(stringResource(R.string.settings_webdav_username))
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = viewModel::updateUsername,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldLabel(stringResource(R.string.settings_webdav_password))
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = viewModel::updatePassword,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldLabel(stringResource(R.string.settings_webdav_remote_path))
                    OutlinedTextField(
                        value = viewModel.remotePath,
                        onValueChange = viewModel::updateRemotePath,
                        placeholder = { Text(stringResource(R.string.settings_webdav_remote_path_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::saveConfig,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy
                    ) {
                        Text(stringResource(R.string.settings_webdav_save_config))
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_webdav_actions_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy
                    ) {
                        Text(stringResource(R.string.settings_webdav_test_connection))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = viewModel::syncToRemote,
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isBusy
                        ) {
                            if (viewModel.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.settings_webdav_sync_to_server))
                            }
                        }
                        FilledTonalButton(
                            onClick = { showRestoreConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isBusy
                        ) {
                            Text(stringResource(R.string.settings_webdav_restore_from_server))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = viewModel::checkConsistency,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isBusy
                    ) {
                        Text(stringResource(R.string.settings_webdav_check_consistency))
                    }
                    viewModel.consistencyReport?.let { report ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = report.formatText(context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_tips_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TipText(stringResource(R.string.settings_webdav_tip_credential))
                    TipText(stringResource(R.string.settings_webdav_tip_initial_sync))
                    TipText(stringResource(R.string.settings_webdav_tip_restore))
                    Spacer(modifier = Modifier.height(8.dp))
                    TipText(stringResource(R.string.settings_webdav_last_auto, viewModel.lastAutoSyncTimeText))
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun TipText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    )
}
