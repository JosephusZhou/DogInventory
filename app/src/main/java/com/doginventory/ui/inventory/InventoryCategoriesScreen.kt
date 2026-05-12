package com.doginventory.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.ui.components.AppCard
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.components.SectionTitle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun InventoryCategoriesScreen(
    categoriesState: StateFlow<InventoryCategoriesState>,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryEditor: (InventoryCategoryEntity?) -> Unit,
    onMoveUp: (InventoryCategoryEntity) -> Unit,
    onMoveDown: (InventoryCategoryEntity) -> Unit,
    onDeleteCategory: (InventoryCategoryEntity) -> Unit
) {
    val state by categoriesState.collectAsState()
    val presetCategories = state.categories.filter { it.isPreset }
    val customCategories = state.categories.filter { !it.isPreset }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.inventory_categories_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCategoryEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.inventory_categories_add))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle(stringResource(R.string.inventory_categories_preset))
            Spacer(modifier = Modifier.height(8.dp))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    presetCategories.forEach { category ->
                        CategoryRow(
                            category = category,
                            compactLayout = true,
                            showEdit = false,
                            showDelete = false,
                            onMoveUp = { onMoveUp(category) },
                            onMoveDown = { onMoveDown(category) },
                            onEdit = {},
                            onDelete = {}
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.inventory_categories_custom), modifier = Modifier.weight(1f))
                TextButton(onClick = { onNavigateToCategoryEditor(null) }) {
                    Text(
                        text = "+  ${stringResource(R.string.inventory_categories_add)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (customCategories.isEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🧺", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.inventory_categories_empty_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.inventory_categories_empty_body),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            } else {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        customCategories.forEach { category ->
                            CategoryRow(
                                category = category,
                                compactLayout = false,
                                showEdit = true,
                                showDelete = true,
                                onMoveUp = { onMoveUp(category) },
                                onMoveDown = { onMoveDown(category) },
                                onEdit = { onNavigateToCategoryEditor(category) },
                                onDelete = { onDeleteCategory(category) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: InventoryCategoryEntity,
    compactLayout: Boolean,
    showEdit: Boolean,
    showDelete: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val avatarSize = if (compactLayout) 32.dp else 36.dp
    val rowVerticalPadding = if (compactLayout) 8.dp else 10.dp
    val actionButtonSize = if (compactLayout) 32.dp else 36.dp
    val actionIconSize = 18.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(avatarSize),
            color = parseInventoryCategoryColor(category.color).copy(alpha = 0.22f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = category.icon)
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            fontSize = if (compactLayout) 14.sp else 16.sp,
            fontWeight = if (compactLayout) FontWeight.Normal else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (showEdit || showDelete) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactIconButton(onClick = onMoveUp) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.inventory_category_move_up), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(actionIconSize))
                }
                CompactIconButton(onClick = onMoveDown) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.inventory_category_move_down), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(actionIconSize))
                }
                CompactIconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(actionIconSize))
                }
                CompactIconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(actionIconSize))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactIconButton(onClick = onMoveUp) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.inventory_category_move_up), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(actionIconSize))
                }
                CompactIconButton(onClick = onMoveDown) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.inventory_category_move_down), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(actionIconSize))
                }
            }
        }
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        content()
    }
}
