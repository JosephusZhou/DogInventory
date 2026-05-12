package com.doginventory.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.components.cardContainerColor
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.theme.InventoryCategoryDefaults
import com.doginventory.ui.theme.SystemBarsStyle

internal val kInventoryCategoryIcons = listOf("🥐", "🥤", "💊", "🧻", "🎫", "📦", "🐾", "🧴", "🍞", "☕", "🍼", "🫙")
internal val kInventoryColorChoices = InventoryCategoryDefaults.presetColorChoices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    viewModel: CategoryEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedPreviewColor = remember(viewModel.color) {
        parseInventoryCategoryColor(viewModel.color)
    }

    SystemBarsStyle(
        navigationBarColor = MaterialTheme.colorScheme.background,
        statusBarColor = MaterialTheme.colorScheme.background
    )
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.isNew) {
                    stringResource(R.string.inventory_category_create_title)
                } else {
                    stringResource(R.string.inventory_category_edit_title)
                },
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
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            EditorLabel(stringResource(R.string.inventory_category_name))

            // Name field
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                placeholder = { Text(stringResource(R.string.inventory_category_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            EditorLabel(stringResource(R.string.inventory_category_icon))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                kInventoryCategoryIcons.forEach { emoji ->
                    val isSelected = viewModel.icon == emoji
                    val interactionSource = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { viewModel.icon = emoji },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else
                            MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 0.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            }

            EditorLabel(stringResource(R.string.inventory_category_custom_emoji))
            OutlinedTextField(
                value = viewModel.icon,
                onValueChange = { newValue ->
                    viewModel.icon = newValue
                },
                placeholder = { Text(stringResource(R.string.inventory_category_custom_emoji_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            EditorLabel(stringResource(R.string.inventory_category_color))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                kInventoryColorChoices.forEach { colorHex ->
                    val isSelected = viewModel.color == colorHex
                    val interactionSource = remember { MutableInteractionSource() }
                    val color = parseInventoryCategoryColor(colorHex)
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { viewModel.color = colorHex },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = color,
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                        } else {
                            BorderStroke(0.dp, Color.Transparent)
                        }
                    ) {}
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardContainerColor(),
                shape = MaterialTheme.shapes.small
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.inventory_category_preview), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    Surface(
                        modifier = Modifier.size(36.dp),
                        color = selectedPreviewColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(viewModel.icon, fontSize = 22.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save(onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.name.isNotBlank()
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
}
