package com.doginventory.ui.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.theme.SystemBarsStyle

@Composable
fun InventorySearchScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    SystemBarsStyle(
        navigationBarColor = MaterialTheme.colorScheme.background,
        statusBarColor = MaterialTheme.colorScheme.background
    )

    val searchState by viewModel.searchState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val navigateBackWithKeyboardDismiss = {
        keyboardController?.hide()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            InventorySearchTopBar(
                query = searchState.query,
                onQueryChange = viewModel::updateSearchQuery,
                onNavigateBack = navigateBackWithKeyboardDismiss
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchState.items.isEmpty()) {
                InventorySearchEmptyState(hasQuery = searchState.query.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchState.items, key = { it.item.id }) { uiModel ->
                        InventoryCard(
                            uiModel = uiModel,
                            onClick = { onNavigateToDetail(uiModel.item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back)
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(color = MaterialTheme.colorScheme.onSurface)
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.inventory_search_placeholder),
                            color = secondaryTextColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(
            onClick = onNavigateBack,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.common_cancel),
                color = secondaryTextColor
            )
        }
    }
}

@Composable
private fun InventorySearchEmptyState(hasQuery: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (hasQuery) "🔎" else "📦", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasQuery) {
                    stringResource(R.string.inventory_search_empty_title)
                } else {
                    stringResource(R.string.inventory_empty_message)
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasQuery) {
                    stringResource(R.string.inventory_search_empty_body)
                } else {
                    stringResource(R.string.inventory_empty_action)
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
