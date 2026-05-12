package com.doginventory.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.data.entity.ShoppingItemEntity
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.components.PageBackground
import com.doginventory.ui.theme.DogInventoryTheme
import com.doginventory.ui.theme.DarkAccent
import com.doginventory.ui.theme.LightAccent
import com.doginventory.ui.theme.White

@Composable
fun ShoppingHomeScreen(
    viewModel: ShoppingViewModel,
    onNavigateToEditor: (String?) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    var showDone by remember { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var confirmClearDone by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.shopping_title),
                actions = {
                    TextButton(onClick = { confirmClearDone = true }) {
                        Text(stringResource(R.string.shopping_clear_done))
                    }
                }
            )
        }
    ) { padding ->
        PageBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(padding.calculateTopPadding()))
                    if (state.pendingItems.isEmpty() && state.doneItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🛒", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(stringResource(R.string.shopping_empty_title), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(stringResource(R.string.shopping_empty_body), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            item { ShoppingGroupHeader(stringResource(R.string.shopping_pending_group), state.pendingItems.size) }
                            items(state.pendingItems, key = { it.id }) { item ->
                                ShoppingItemCard(
                                    item = item,
                                    onClick = { onNavigateToEditor(item.id) },
                                    onToggleDone = { viewModel.toggleDone(item, true) },
                                    onDelete = { deleteTarget = item }
                                )
                            }
                            if (state.doneItems.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                                item {
                                    ShoppingGroupHeader(
                                        title = stringResource(R.string.shopping_done_group),
                                        count = state.doneItems.size,
                                        collapsible = true,
                                        expanded = showDone,
                                        onToggle = { showDone = !showDone }
                                    )
                                }
                                if (showDone) {
                                    items(state.doneItems, key = { it.id }) { item ->
                                        ShoppingItemCard(
                                            item = item,
                                            onClick = { onNavigateToEditor(item.id) },
                                            onToggleDone = { viewModel.toggleDone(item, false) },
                                            onDelete = { deleteTarget = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToEditor(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.shopping_add_button)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    containerColor = if (isDarkTheme) DarkAccent else LightAccent,
                    contentColor = White
                )
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.shopping_delete_title)) },
            text = { Text(stringResource(R.string.shopping_delete_message, deleteTarget?.name ?: "")) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteItem(deleteTarget!!.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (confirmClearDone) {
        AlertDialog(
            onDismissRequest = { confirmClearDone = false },
            title = { Text(stringResource(R.string.shopping_clear_confirm_title)) },
            text = { Text(stringResource(R.string.shopping_clear_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDoneItems()
                        confirmClearDone = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DogInventoryTheme.semanticColors.danger,
                        contentColor = DogInventoryTheme.semanticColors.onDanger
                    )
                ) {
                    Text(stringResource(R.string.shopping_clear_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDone = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ShoppingGroupHeader(
    title: String,
    count: Int,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = collapsible,
                interactionSource = interactionSource,
                indication = null
            ) { onToggle?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($count)",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (collapsible) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer(rotationZ = if (expanded) 0f else -90f),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItemEntity,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = item.isDone,
            onCheckedChange = { onToggleDone() }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                color = if (item.isDone) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (item.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.shopping_note_prefix, item.note),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (item.isDone) 0.4f else 0.5f),
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.common_delete))
        }
    }
}
