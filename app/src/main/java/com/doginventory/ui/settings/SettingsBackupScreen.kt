package com.doginventory.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.components.AppCard
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.theme.SystemBarsStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(
    viewModel: SettingsBackupViewModel,
    onRestartApp: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showFlutterMigrationConfirmDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createBackup(uri)
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }
    val migrateFlutterBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.migrateFromFlutterBackup(uri)
        }
    }

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
            title = { Text(stringResource(R.string.settings_backup_restore_dialog_title)) },
            text = { Text(stringResource(R.string.settings_backup_restore_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        restoreBackupLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
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

    if (showFlutterMigrationConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFlutterMigrationConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_flutter_migration_dialog_title)) },
            text = { Text(stringResource(R.string.settings_flutter_migration_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFlutterMigrationConfirmDialog = false
                        migrateFlutterBackupLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFlutterMigrationConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    viewModel.migrationResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::consumeMigrationResult,
            title = { Text(stringResource(R.string.settings_flutter_migration_result_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_flutter_migration_result_message,
                        result.categoriesImported,
                        result.inventoryItemsImported,
                        result.reminderRulesImported,
                        result.shoppingItemsImported,
                        if (result.warnings.isEmpty()) {
                            stringResource(R.string.settings_flutter_migration_no_warning)
                        } else {
                            result.warnings.joinToString(separator = "\n")
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumeMigrationResult) {
                    Text(stringResource(R.string.common_confirm))
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
                title = stringResource(R.string.settings_backup_title),
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
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !viewModel.isBusy) {
                            if (viewModel.requiresLegacyStoragePermission && !viewModel.hasLegacyStoragePermission) {
                                viewModel.requestLegacyStoragePermission { granted ->
                                    if (granted) {
                                        createBackupLauncher.launch("dog_inventory_backup.zip")
                                    }
                                }
                            } else {
                                createBackupLauncher.launch("dog_inventory_backup.zip")
                            }
                        },
                    headlineContent = { Text(stringResource(R.string.settings_backup_create_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_backup_create_body)) },
                    leadingContent = { Text("🗂️", fontSize = 24.sp) },
                    trailingContent = {
                        if (viewModel.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !viewModel.isBusy) {
                            showRestoreConfirmDialog = true
                        },
                    headlineContent = { Text(stringResource(R.string.settings_backup_restore_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_backup_restore_body)) },
                    leadingContent = { Text("♻️", fontSize = 24.sp) }
                )
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !viewModel.isBusy) {
                            showFlutterMigrationConfirmDialog = true
                        },
                    headlineContent = { Text(stringResource(R.string.settings_flutter_migration_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_flutter_migration_body)) },
                    leadingContent = { Text("🚚", fontSize = 24.sp) },
                    trailingContent = {
                        if (viewModel.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_tips_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_backup_tip_regular),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Text(
                        stringResource(R.string.settings_backup_tip_restart),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_backup_last_auto, viewModel.lastAutoBackupTimeText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    if (viewModel.requiresLegacyStoragePermission && !viewModel.hasLegacyStoragePermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.settings_backup_legacy_permission_tip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}
