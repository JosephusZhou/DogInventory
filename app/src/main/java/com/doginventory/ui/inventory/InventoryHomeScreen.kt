package com.doginventory.ui.inventory

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.components.PageBackground
import com.doginventory.ui.components.cardContainerColor
import com.doginventory.ui.theme.DogInventoryTheme
import com.doginventory.ui.theme.DarkAccent
import com.doginventory.ui.theme.LightAccent
import com.doginventory.ui.theme.LightPrimary
import com.doginventory.ui.theme.LightExpiredContainer
import com.doginventory.ui.theme.LightWarningContainer
import com.doginventory.ui.theme.LightWarningOnContainer
import com.doginventory.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryHomeScreen(
    viewModel: InventoryViewModel,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.inventory_title),
                actions = {
                    IconButton(onClick = onNavigateToCategories) {
                        Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.inventory_manage_categories))
                    }
                }
            )
        }
    ) { padding ->
        PageBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(padding.calculateTopPadding()))
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!state.hasItems) {
                    EmptyInventoryState(onAddClick = { onNavigateToEditor(null) })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 108.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            FilterRow(
                                selectedFilter = state.filter,
                                onFilterSelected = { viewModel.setFilter(it) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    title = stringResource(R.string.inventory_filter_soon),
                                    value = state.soonCount.toString(),
                                    subtitle = stringResource(R.string.inventory_soon_subtitle),
                                    accent = DogInventoryTheme.semanticColors.warningAccent,
                                    onClick = { viewModel.setFilter(InventoryFilter.Soon) }
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    title = stringResource(R.string.inventory_filter_expired),
                                    value = state.expiredCount.toString(),
                                    subtitle = stringResource(R.string.inventory_expired_subtitle),
                                    accent = DogInventoryTheme.semanticColors.inventoryExpired,
                                    onClick = { viewModel.setFilter(InventoryFilter.Expired) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (state.isFilterEmpty) {
                            item {
                                EmptyInventoryFilterState(
                                    onReset = { viewModel.setFilter(InventoryFilter.All) }
                                )
                            }
                        } else {
                            items(state.items, key = { it.item.id }) { uiModel ->
                                InventoryCard(
                                    uiModel = uiModel,
                                    onClick = { onNavigateToDetail(uiModel.item.id) }
                                )
                            }
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.inventory_add_button)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                containerColor = if (isDarkTheme) DarkAccent else LightAccent,
                contentColor = White
            )
        }
        }
    }
}

@Composable
fun FilterRow(
    selectedFilter: InventoryFilter,
    onFilterSelected: (InventoryFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InventoryFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = cardContainerColor(),
                    selectedContainerColor = LightPrimary.copy(alpha = 0.2f),
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                ),
                label = {
                    Text(
                        when (filter) {
                            InventoryFilter.All -> stringResource(R.string.inventory_filter_all)
                            InventoryFilter.Soon -> stringResource(R.string.inventory_filter_soon)
                            InventoryFilter.Expired -> stringResource(R.string.inventory_filter_expired)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = cardContainerColor(),
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1)
        }
    }
}

@Composable
fun InventoryCard(
    uiModel: InventoryItemUiModel,
    onClick: () -> Unit
) {
    val item = uiModel.item
    val category = uiModel.category
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = cardContainerColor(),
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                color = parseInventoryCategoryColor(category?.color).copy(alpha = 0.2f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = category?.icon ?: "📦",
                        fontSize = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1.0f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiModel.isExpired || uiModel.isSoon) {
                        Spacer(modifier = Modifier.width(8.dp))
                        InventoryStatusChip(
                            label = if (uiModel.isExpired) {
                                stringResource(R.string.inventory_filter_expired)
                            } else {
                                stringResource(R.string.inventory_filter_soon)
                            },
                            isExpired = uiModel.isExpired
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category?.name ?: stringResource(R.string.inventory_uncategorized),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiModel.expireDateText == null) {
                        stringResource(R.string.inventory_never_expires)
                    } else {
                        stringResource(R.string.inventory_expire_time, uiModel.expireDateText)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        uiModel.isExpired -> DogInventoryTheme.semanticColors.inventoryExpired
                        uiModel.isSoon -> DogInventoryTheme.semanticColors.warning
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun InventoryStatusChip(label: String, isExpired: Boolean) {
    val background = if (isExpired) LightExpiredContainer else LightWarningContainer
    val foreground = if (isExpired) DogInventoryTheme.semanticColors.inventoryExpired else LightWarningOnContainer
    Surface(
        color = background,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyInventoryState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📦", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.inventory_empty_message), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Text(stringResource(R.string.inventory_empty_action))
        }
    }
}

@Composable
fun EmptyInventoryFilterState(onReset: () -> Unit) {
    Surface(
        color = cardContainerColor(),
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧺", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.inventory_empty_filter_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.inventory_empty_filter_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onReset) {
                Text(stringResource(R.string.inventory_reset_filter))
            }
        }
    }
}
