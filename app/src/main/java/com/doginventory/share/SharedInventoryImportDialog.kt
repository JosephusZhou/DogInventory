package com.doginventory.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.inventory.formatInventoryExpireText
import com.doginventory.ui.inventory.isInventoryExpired
import com.doginventory.ui.inventory.isInventorySoon
import com.doginventory.ui.inventory.parseInventoryCategoryColor

@Composable
fun SharedInventoryImportDialog(
    viewModel: SharedInventoryImportViewModel,
    shareId: String,
    onDismiss: () -> Unit
) {
    val state by viewModel.state

    LaunchedEffect(shareId) {
        viewModel.loadIfNeeded(shareId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_import_dialog_title)) },
        text = {
            ImportDialogBody(state = state, viewModel = viewModel)
        },
        confirmButton = {
            when {
                state.isImporting -> {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                state.result != null -> {
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
                state.error != null && state.list == null -> {
                    Button(onClick = { viewModel.load(shareId) }) {
                        Text(stringResource(R.string.share_import_retry))
                    }
                }
                state.list != null -> {
                    Button(
                        enabled = state.selectedItemIds.isNotEmpty() && !state.isImporting,
                        onClick = { viewModel.import() }
                    ) {
                        Text(stringResource(R.string.share_import_button, state.selectedItemIds.size))
                    }
                }
            }
        },
        dismissButton = {
            when {
                state.result != null || (state.error != null && state.list == null) -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.share_import_close))
                    }
                }
                state.list != null -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
                else -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        }
    )
}

@Composable
private fun ImportDialogBody(
    state: SharedInventoryImportState,
    viewModel: SharedInventoryImportViewModel
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            state.error != null && state.list == null -> {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            state.result != null -> {
                Text(
                    text = stringResource(R.string.share_import_success_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.share_import_success_message,
                        state.result!!.newCategoryCount,
                        state.result!!.importedItemCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            state.list != null -> {
                val list = state.list!!
                Text(
                    text = stringResource(R.string.share_import_summary, list.title, list.items.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.selectAll() }) {
                        Text(stringResource(R.string.share_import_select_all))
                    }
                    TextButton(onClick = { viewModel.deselectAll() }) {
                        Text(stringResource(R.string.share_import_deselect_all))
                    }
                }
                val hasAnyRules = list.items.any { it.rules.isNotEmpty() }
                if (hasAnyRules) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.share_import_rules_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = state.importReminderRules, onCheckedChange = viewModel::setImportReminderRules)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    list.items.forEach { item ->
                        ImportItemRow(
                            item = item,
                            selected = item.id in state.selectedItemIds,
                            onToggle = { selected -> viewModel.toggleItem(item.id, selected) }
                        )
                    }
                }
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.isImporting) {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.share_import_progress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportItemRow(
    item: SharedItemDto,
    selected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val accentColor = parseInventoryCategoryColor(item.categoryColor)
    val isExpired = isInventoryExpired(item.expireAt)
    val isSoon = isInventorySoon(item.expireAt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = onToggle)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.categoryIcon ?: "📦", fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (!item.categoryName.isNullOrBlank()) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            val expireText = if (item.expireAt == null) {
                context.getString(R.string.inventory_never_expires)
            } else {
                formatInventoryExpireText(context, item.expireAt)
            }
            Text(
                text = expireText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isExpired -> MaterialTheme.colorScheme.error
                    isSoon -> com.doginventory.ui.theme.DogInventoryTheme.semanticColors.warning
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
        }
    }
}
