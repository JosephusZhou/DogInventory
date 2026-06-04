package com.doginventory.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.doginventory.R
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.ui.inventory.formatInventoryExpireText
import com.doginventory.ui.inventory.isInventoryExpired
import com.doginventory.ui.inventory.isInventorySoon
import com.doginventory.ui.inventory.parseInventoryCategoryColor

@Composable
fun InventoryShareDialog(
    viewModel: InventoryShareViewModel,
    filteredItems: List<InventoryItemEntity>,
    allItems: List<InventoryItemEntity>,
    allCategories: List<InventoryCategoryEntity>,
    onDismiss: () -> Unit
) {
    val state by viewModel.state
    val context = LocalContext.current

    LaunchedEffect(filteredItems, allItems, allCategories) {
        viewModel.updateContext(filteredItems, allItems, allCategories)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_dialog_title)) },
        text = {
            when {
                state.isSharing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.share_in_progress),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                state.shareUrl != null -> {
                    Column {
                        Text(
                            text = stringResource(R.string.share_url_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.shareUrl!!,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                state.shareError != null -> {
                    val err = state.shareError!!
                    val errorMessage = when (err) {
                        "empty" -> stringResource(R.string.share_error_empty)
                        "network" -> stringResource(R.string.share_error_network)
                        else -> {
                            val code = err.removePrefix("http:").toIntOrNull() ?: 0
                            stringResource(R.string.share_error_http, code)
                        }
                    }
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    ShareInputBody(
                        state = state,
                        viewModel = viewModel,
                        allItems = allItems,
                        allCategories = allCategories
                    )
                }
            }
        },
        confirmButton = {
            when {
                state.isSharing -> {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                state.shareUrl != null -> {
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
                state.shareError != null -> {
                    Button(onClick = { viewModel.createShare() }) {
                        Text(stringResource(R.string.share_import_retry))
                    }
                }
                else -> {
                    Button(
                        enabled = state.selectedItemIds.isNotEmpty() && !state.isSharing,
                        onClick = { viewModel.createShare() }
                    ) {
                        Text(stringResource(R.string.share_create_button))
                    }
                }
            }
        },
        dismissButton = {
            if (state.shareUrl == null) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            } else {
                Row {
                    TextButton(onClick = { copyToClipboard(context, state.shareUrl!!) }) {
                        Text(stringResource(R.string.share_copy_button))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        state.shareUrl?.let { url ->
                            ShareCompat.IntentBuilder(context)
                                .setType("text/plain")
                                .setChooserTitle(context.getString(R.string.share_chooser_title))
                                .setText(url)
                                .startChooser()
                        }
                    }) {
                        Text(stringResource(R.string.share_system_button))
                    }
                }
            }
        }
    )
}

@Composable
private fun ShareInputBody(
    state: InventoryShareState,
    viewModel: InventoryShareViewModel,
    allItems: List<InventoryItemEntity>,
    allCategories: List<InventoryCategoryEntity>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::setTitle,
            label = { Text(stringResource(R.string.share_title_label)) },
            placeholder = { Text(stringResource(R.string.share_title_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ShareOptionRow(
            label = stringResource(R.string.share_include_rules_label),
            checked = state.includeReminderRules,
            onCheckedChange = viewModel::setIncludeReminderRules
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { viewModel.selectAll() }) {
                Text(stringResource(R.string.share_select_all))
            }
            TextButton(onClick = { viewModel.deselectAll() }) {
                Text(stringResource(R.string.share_deselect_all))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.share_selection_count,
                state.selectedCount,
                state.totalCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))
        if (allItems.isEmpty()) {
            Text(
                text = stringResource(R.string.share_error_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val categoryMap = allCategories.associateBy { it.id }
                allItems.forEach { item ->
                    ShareItemRow(
                        item = item,
                        category = item.categoryId?.let { categoryMap[it] },
                        selected = item.id in state.selectedItemIds,
                        onToggle = { checked -> viewModel.toggleItem(item.id, checked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareItemRow(
    item: InventoryItemEntity,
    category: InventoryCategoryEntity?,
    selected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val accentColor = parseInventoryCategoryColor(category?.color)
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
            Text(text = category?.icon ?: "📦", fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (category != null) {
                Text(
                    text = category.name,
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

@Composable
private fun ShareOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("share_url", text))
    Toast.makeText(context, context.getString(R.string.share_copy_success), Toast.LENGTH_SHORT).show()
}
