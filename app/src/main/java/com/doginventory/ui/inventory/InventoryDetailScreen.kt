package com.doginventory.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.components.AppCard
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.theme.DogInventoryTheme
import com.doginventory.ui.theme.SystemBarsStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    viewModel: InventoryDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SystemBarsStyle(
        navigationBarColor = MaterialTheme.colorScheme.background,
        statusBarColor = MaterialTheme.colorScheme.background
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.inventory_detail_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.common_more))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                val currentItem = state.item
                                if (currentItem != null) {
                                    onNavigateToEdit(currentItem.id)
                                }
                            },
                            text = { Text(stringResource(R.string.common_edit)) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_delete), color = DogInventoryTheme.semanticColors.inventoryExpired) },
                            onClick = {
                                showMenu = false
                                viewModel.confirmDelete = true
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        val item = state.item
        val category = state.category
        val rules = state.rules
        val isExpired = state.isExpired
        val isSoon = state.isSoon

        if (item == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(92.dp),
                        color = parseInventoryCategoryColor(category?.color).copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = category?.icon ?: "📦",
                                fontSize = 36.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = category?.name ?: stringResource(R.string.inventory_uncategorized),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatInventoryExpireText(context, item.expireAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isExpired -> DogInventoryTheme.semanticColors.inventoryExpired
                                isSoon -> DogInventoryTheme.semanticColors.warning
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
            }

            // Status Section
            if (isExpired || isSoon) {
                item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpired)
                            DogInventoryTheme.semanticColors.inventoryExpiredContainer
                        else
                            DogInventoryTheme.semanticColors.warningContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpired) "⚠️" else "⏰",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = if (isExpired) stringResource(R.string.inventory_status_expired) else stringResource(R.string.inventory_status_soon),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpired) {
                                    DogInventoryTheme.semanticColors.inventoryExpired
                                } else {
                                    DogInventoryTheme.semanticColors.warningOnContainer
                                }
                            )
                            if (isExpired) {
                                Text(
                                    text = stringResource(R.string.inventory_expired_message),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                }
            }

            // Reminder Rules Section
            if (rules.isNotEmpty()) {
                item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.inventory_reminder_rules_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        rules.forEach { rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (rule.enabled) "✅" else "⬜",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                val draft = InventoryReminderDraft.fromEntity(rule)
                                Text(
                                    text = draft.label(context),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!rule.enabled) {
                                    Text(
                                        text = stringResource(R.string.inventory_rule_disabled),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }

            // Note Section
            if (item.note.isNotBlank()) {
                item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.inventory_detail_section_note), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                }
            }

            // Metadata Section
            item {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.inventory_detail_section_metadata), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataRow(stringResource(R.string.inventory_metadata_created_at), item.createdAt.let(::formatInventoryDateTime))
                    MetadataRow(stringResource(R.string.inventory_metadata_updated_at), item.updatedAt.let(::formatInventoryDateTime))
                }
            }
            }
        }
        }
    }

    // Delete Confirmation Dialog
    if (viewModel.confirmDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmDelete = false },
            title = { Text(stringResource(R.string.inventory_delete_confirm_title)) },
            text = { Text(stringResource(R.string.inventory_delete_confirm_message, state.item?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDelete = false
                        viewModel.deleteItem(onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DogInventoryTheme.semanticColors.danger,
                        contentColor = DogInventoryTheme.semanticColors.onDanger
                    )
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
