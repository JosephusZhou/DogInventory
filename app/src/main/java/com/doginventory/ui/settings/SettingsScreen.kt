package com.doginventory.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doginventory.R
import com.doginventory.ui.components.AppTopBar
import com.doginventory.ui.components.PageBackground
import com.doginventory.ui.components.SectionTitle
import com.doginventory.ui.components.cardContainerColor
import com.doginventory.ui.theme.AppThemeMode
import com.doginventory.ui.theme.DogInventoryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    isNotificationPermissionGranted: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToWebdav: () -> Unit
) {
    PageBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppTopBar(title = stringResource(R.string.settings_title), containerColor = Color.Transparent)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.settings_section_reminder))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardContainerColor(),
                    shadowElevation = 2.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column {
                        SettingsItem(
                            icon = "🔔",
                            title = stringResource(R.string.settings_notification_permission_title),
                            subtitle = if (isNotificationPermissionGranted) {
                                stringResource(R.string.settings_notification_permission_granted)
                            } else {
                                stringResource(R.string.settings_notification_permission_not_granted)
                            },
                            trailingText = if (isNotificationPermissionGranted) {
                                stringResource(R.string.settings_enabled)
                            } else {
                                stringResource(R.string.settings_go_enable)
                            },
                            trailingColor = if (isNotificationPermissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                DogInventoryTheme.semanticColors.warning
                            },
                            onClick = {
                                if (isNotificationPermissionGranted) {
                                    onOpenAppPermissionSettings()
                                } else {
                                    onRequestNotificationPermission()
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        SettingsItem(
                            icon = "⏰",
                            title = stringResource(R.string.settings_exact_alarm_permission_title),
                            subtitle = if (canScheduleExactAlarms) {
                                stringResource(R.string.settings_exact_alarm_permission_granted)
                            } else {
                                stringResource(R.string.settings_exact_alarm_permission_not_granted)
                            },
                            trailingText = if (canScheduleExactAlarms) {
                                stringResource(R.string.settings_enabled)
                            } else {
                                stringResource(R.string.settings_go_enable)
                            },
                            trailingColor = if (canScheduleExactAlarms) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                DogInventoryTheme.semanticColors.warning
                            },
                            onClick = onOpenExactAlarmSettings
                        )
                    }
                }

                SectionTitle(stringResource(R.string.settings_section_appearance))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardContainerColor(),
                    shadowElevation = 2.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    ThemeModeSettingItem(
                        currentMode = themeMode,
                        onModeChange = onThemeModeChange
                    )
                }

                SectionTitle(stringResource(R.string.settings_section_more))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardContainerColor(),
                    shadowElevation = 2.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column {
                        SettingsItem(
                            icon = "🏷️",
                            title = stringResource(R.string.inventory_categories_title),
                            subtitle = stringResource(R.string.settings_manage_categories_subtitle),
                            onClick = onNavigateToCategories
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        SettingsItem(
                            icon = "📦",
                            title = stringResource(R.string.settings_backup_title),
                            subtitle = stringResource(R.string.settings_backup_subtitle),
                            onClick = onNavigateToBackup
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        SettingsItem(
                            icon = "☁️",
                            title = stringResource(R.string.settings_webdav_title),
                            subtitle = stringResource(R.string.settings_webdav_subtitle),
                            onClick = onNavigateToWebdav
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSettingItem(
    currentMode: AppThemeMode,
    onModeChange: (AppThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text("🌙", fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        SingleChoiceSegmentedButtonRow {
            listOf(
                AppThemeMode.System to stringResource(R.string.settings_theme_system),
                AppThemeMode.Light to stringResource(R.string.settings_theme_light),
                AppThemeMode.Dark to stringResource(R.string.settings_theme_dark)
            ).forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        activeContentColor = MaterialTheme.colorScheme.onSurface,
                        inactiveContainerColor = cardContainerColor(),
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        activeBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        inactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    ),
                    icon = {},
                    label = {
                        Text(label, fontSize = 11.sp)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: String,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    trailingColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (trailingText != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = trailingColor
                )
            }
        }
    }
}
