package com.doginventory.ui.inventory

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.components.AppCard
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.components.SectionTitle
import com.doginventory.ui.components.cardContainerColor
import com.doginventory.ui.theme.White
import com.doginventory.ui.theme.SystemBarsStyle
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryEditorScreen(
    viewModel: InventoryEditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    SystemBarsStyle(
        navigationBarColor = MaterialTheme.colorScheme.background,
        statusBarColor = MaterialTheme.colorScheme.background
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    val categories by viewModel.categories.collectAsState()
    var showCategoryMenu by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    viewModel.expireAt?.let { calendar.timeInMillis = it }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
            viewModel.setExpireAtAndDefaults(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.itemId == null) {
                    stringResource(R.string.inventory_create_title)
                } else {
                    stringResource(R.string.inventory_edit_title)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(stringResource(R.string.inventory_name_required))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.name = it },
                        placeholder = { Text(stringResource(R.string.inventory_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    SectionTitle(stringResource(R.string.inventory_category_label))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SelectionField(
                            value = categories.find { it.id == viewModel.selectedCategoryId }?.let { "${it.icon} ${it.name}" }
                                ?: stringResource(R.string.inventory_uncategorized),
                            onClick = { showCategoryMenu = true }
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.inventory_uncategorized)) },
                                onClick = {
                                    viewModel.selectedCategoryId = null
                                    showCategoryMenu = false
                                }
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text("${category.icon} ${category.name}") },
                                    onClick = {
                                        viewModel.selectedCategoryId = category.id
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToCategories, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.inventory_manage_category_button), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(stringResource(R.string.inventory_expire_label))
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionField(
                        value = viewModel.expireAt?.let(::formatInventoryDate) ?: stringResource(R.string.inventory_pick_expire_datetime),
                        onClick = { datePickerDialog.show() }
                    )
                    if (viewModel.expireAt != null) {
                        TextButton(onClick = { viewModel.setExpireAtAndDefaults(null) }) {
                            Text(stringResource(R.string.inventory_clear_expire_time))
                        }
                    }
                }
            }

            ReminderRulesCard(
                expireAt = viewModel.expireAt,
                rules = viewModel.rules,
                onAddOffset = viewModel::addOffsetRule,
                onAddAbsolute = viewModel::addAbsoluteRule,
                onToggleRule = viewModel::toggleRule,
                onRemoveRule = viewModel::removeRule
            )

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(stringResource(R.string.inventory_note_label))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.note,
                        onValueChange = { viewModel.note = it },
                        placeholder = { Text(stringResource(R.string.inventory_note_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Button(
                onClick = { viewModel.save(onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.name.isNotBlank()
            ) {
                Text(stringResource(R.string.common_save))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectionField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = cardContainerColor(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.common_expand),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReminderRulesCard(
    expireAt: Long?,
    rules: List<InventoryReminderDraft>,
    onAddOffset: (Int) -> Unit,
    onAddAbsolute: (Long) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onRemoveRule: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.inventory_reminder_rules_title))
            Spacer(modifier = Modifier.height(12.dp))
            if (expireAt == null) {
                Text(
                    text = stringResource(R.string.inventory_reminder_hint_requires_expire),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                return@Column
            }

            if (rules.isEmpty()) {
                Text(
                    text = stringResource(R.string.inventory_no_reminder_rules),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            rules.forEach { rule ->
                Surface(
                    color = cardContainerColor(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                        ) {
                        androidx.compose.material3.Checkbox(
                            checked = rule.enabled,
                            onCheckedChange = { onToggleRule(rule.id, it) }
                        )
                        Text(rule.label(context), modifier = Modifier.weight(1f), fontSize = 14.sp)
                        IconButton(onClick = { onRemoveRule(rule.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.inventory_delete_rule))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var showOffsetMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { showOffsetMenu = true }) { Text(stringResource(R.string.inventory_add_offset_reminder)) }
                    DropdownMenu(expanded = showOffsetMenu, onDismissRequest = { showOffsetMenu = false }) {
                        listOf(1, 3, 7, 14, 30).forEach { days ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.inventory_offset_reminder_days, days)) },
                                onClick = {
                                    showOffsetMenu = false
                                    onAddOffset(days)
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = {
                    val initial = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 20)
                        set(Calendar.MINUTE, 0)
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day, hour, minute, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    onAddAbsolute(selected.timeInMillis)
                                },
                                initial.get(Calendar.HOUR_OF_DAY),
                                initial.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        initial.get(Calendar.YEAR),
                        initial.get(Calendar.MONTH),
                        initial.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text(stringResource(R.string.inventory_add_absolute_reminder))
                }
            }
        }
    }
}
