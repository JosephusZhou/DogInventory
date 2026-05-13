package com.doginventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.doginventory.R
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.permission.StoragePermissionCoordinator
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.ui.inventory.CategoryEditorScreen
import com.doginventory.ui.inventory.InventoryCategoriesScreen
import com.doginventory.ui.inventory.InventoryCategoriesViewModel
import com.doginventory.ui.inventory.InventoryDetailScreen
import com.doginventory.ui.inventory.InventoryEditorScreen
import com.doginventory.ui.inventory.InventoryHomeScreen
import com.doginventory.ui.inventory.InventorySearchScreen
import com.doginventory.ui.settings.SettingsBackupScreen
import com.doginventory.ui.settings.SettingsBackupViewModel
import com.doginventory.ui.settings.SettingsScreen
import com.doginventory.ui.settings.SettingsWebdavSyncScreen
import com.doginventory.ui.settings.SettingsWebdavSyncViewModel
import com.doginventory.ui.shopping.ShoppingEditorScreen
import com.doginventory.ui.shopping.ShoppingHomeScreen
import com.doginventory.ui.shopping.ShoppingSearchScreen
import com.doginventory.ui.theme.AppThemeMode
import com.doginventory.ui.theme.SystemBarsStyle

sealed class Screen(val route: String, val labelRes: Int, val iconRes: Int, val selectedIconRes: Int) {
    object Main : Screen("main", 0, 0, 0)
    object Shopping : Screen("shopping", R.string.tab_shopping, R.mipmap.ic_tab_shopping_normal, R.mipmap.ic_tab_shopping_selected)
    object Inventory : Screen("inventory", R.string.tab_inventory, R.mipmap.ic_tab_inventory_normal, R.mipmap.ic_tab_inventory_selected)
    object Settings : Screen("settings", R.string.tab_settings, R.mipmap.ic_tab_settings_normal, R.mipmap.ic_tab_settings_selected)
    object ShoppingEditor : Screen("shopping_editor?itemId={itemId}", 0, 0, 0) {
        fun createRoute(itemId: String?) = if (itemId != null) "shopping_editor?itemId=$itemId" else "shopping_editor"
    }
    object ShoppingSearch : Screen("shopping_search", 0, 0, 0)
    object InventorySearch : Screen("inventory_search", 0, 0, 0)
    object Editor : Screen("editor?itemId={itemId}", 0, 0, 0) {
        fun createRoute(itemId: String?) = if (itemId != null) "editor?itemId=$itemId" else "editor"
    }
    object Detail : Screen("detail?itemId={itemId}", 0, 0, 0) {
        fun createRoute(itemId: String) = "detail?itemId=$itemId"
    }
    object Categories : Screen("categories", 0, 0, 0)
    object CategoryEditor : Screen("category_editor?categoryId={categoryId}", 0, 0, 0) {
        fun createRoute(categoryId: String?) = if (categoryId != null) "category_editor?categoryId=$categoryId" else "category_editor"
    }
    object SettingsBackup : Screen("settings_backup", 0, 0, 0)
    object SettingsWebdav : Screen("settings_webdav", 0, 0, 0)
}

@Composable
fun MainScreen(
    repository: InventoryRepository,
    reminderScheduler: InventoryReminderScheduler,
    storagePermissionCoordinator: StoragePermissionCoordinator,
    themeMode: AppThemeMode,
    isNotificationPermissionGranted: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestAppReminderPermissions: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onRestartApp: () -> Unit
) {
    SystemBarsStyle(
        navigationBarColor = MaterialTheme.colorScheme.background,
        statusBarColor = MaterialTheme.colorScheme.background
    )
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainShellScreen(
                repository = repository,
                reminderScheduler = reminderScheduler,
                storagePermissionCoordinator = storagePermissionCoordinator,
                themeMode = themeMode,
                isNotificationPermissionGranted = isNotificationPermissionGranted,
                canScheduleExactAlarms = canScheduleExactAlarms,
                onRequestAppReminderPermissions = onRequestAppReminderPermissions,
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                onThemeModeChange = onThemeModeChange,
                onRestartApp = onRestartApp,
                onNavigateToEditor = { itemId -> navController.navigate(Screen.Editor.createRoute(itemId)) },
                onNavigateToShoppingEditor = { itemId -> navController.navigate(Screen.ShoppingEditor.createRoute(itemId)) },
                onNavigateToShoppingSearch = { navController.navigate(Screen.ShoppingSearch.route) },
                onNavigateToInventorySearch = { navController.navigate(Screen.InventorySearch.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToDetail = { itemId -> navController.navigate(Screen.Detail.createRoute(itemId)) },
                onNavigateToBackup = { navController.navigate(Screen.SettingsBackup.route) },
                onNavigateToWebdav = { navController.navigate(Screen.SettingsWebdav.route) }
            )
        }
            // Editor (new/edit)
        composable(
            route = Screen.ShoppingEditor.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            ShoppingEditorScreen(
                viewModel = viewModel(factory = ViewModelFactory(repository, itemId = itemId)),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ShoppingSearch.route) {
            ShoppingSearchScreen(
                viewModel = viewModel(factory = ViewModelFactory(repository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { itemId -> navController.navigate(Screen.ShoppingEditor.createRoute(itemId)) }
            )
        }
        composable(Screen.InventorySearch.route) {
            InventorySearchScreen(
                viewModel = viewModel(factory = ViewModelFactory(repository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { itemId -> navController.navigate(Screen.Detail.createRoute(itemId)) }
            )
        }
        composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
        ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                InventoryEditorScreen(
                    viewModel = viewModel(factory = ViewModelFactory(repository, itemId = itemId)),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) }
                )
        }
            // Detail
        composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.StringType
                        nullable = false
                    }
                )
        ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")!!
                InventoryDetailScreen(
                    viewModel = viewModel(factory = ViewModelFactory(repository, itemId = itemId)),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.Editor.createRoute(id))
                    }
                )
        }
            // Categories
        composable(Screen.Categories.route) {
                val vm = viewModel<InventoryCategoriesViewModel>(factory = ViewModelFactory(repository))
                InventoryCategoriesScreen(
                    categoriesState = vm.state,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategoryEditor = { category ->
                        navController.navigate(Screen.CategoryEditor.createRoute(category?.id))
                    },
                    onMoveUp = { category -> vm.moveUp(category) },
                    onMoveDown = { category -> vm.moveDown(category) },
                    onDeleteCategory = { category -> vm.deleteCategory(category) }
                )
        }
        composable(
                route = Screen.CategoryEditor.route,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
        ) { backStackEntry ->
                // For simplicity, editing a category needs to look up the category first.
                // We use a ViewModel that handles loading the category by id.
                CategoryEditorScreen(
                    viewModel = viewModel(factory = ViewModelFactory(repository, itemId = null, category = null)),
                    onNavigateBack = { navController.popBackStack() }
                )
        }
            // Settings Backup
        composable(Screen.SettingsBackup.route) {
                val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
                val vm = viewModel<SettingsBackupViewModel>(factory = ViewModelFactory(repository, applicationContext = context, storagePermissionCoordinator = storagePermissionCoordinator))
                SettingsBackupScreen(
                    viewModel = vm,
                    onRestartApp = onRestartApp,
                    onNavigateBack = { navController.popBackStack() }
                )
        }
            // Settings WebDAV
        composable(Screen.SettingsWebdav.route) {
                val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
                val vm = viewModel<SettingsWebdavSyncViewModel>(factory = ViewModelFactory(repository, applicationContext = context))
                SettingsWebdavSyncScreen(
                    viewModel = vm,
                    onRestartApp = onRestartApp,
                    onNavigateBack = { navController.popBackStack() }
                )
        }
    }
}

@Composable
private fun MainShellScreen(
    repository: InventoryRepository,
    reminderScheduler: InventoryReminderScheduler,
    storagePermissionCoordinator: StoragePermissionCoordinator,
    themeMode: AppThemeMode,
    isNotificationPermissionGranted: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestAppReminderPermissions: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onRestartApp: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToShoppingEditor: (String?) -> Unit,
    onNavigateToShoppingSearch: () -> Unit,
    onNavigateToInventorySearch: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToWebdav: () -> Unit
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarScreens = listOf(Screen.Inventory, Screen.Shopping, Screen.Settings)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomTabBar(
                screens = bottomBarScreens,
                currentDestination = currentDestination,
                onNavigate = { route ->
                    tabNavController.navigate(route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Screen.Inventory.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Inventory.route) {
                InventoryHomeScreen(
                    viewModel = viewModel(factory = ViewModelFactory(repository)),
                    onNavigateToEditor = onNavigateToEditor,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToSearch = onNavigateToInventorySearch
                )
            }
            composable(Screen.Shopping.route) {
                ShoppingHomeScreen(
                    viewModel = viewModel(factory = ViewModelFactory(repository)),
                    onNavigateToEditor = onNavigateToShoppingEditor,
                    onNavigateToSearch = onNavigateToShoppingSearch
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    themeMode = themeMode,
                    isNotificationPermissionGranted = isNotificationPermissionGranted,
                    canScheduleExactAlarms = canScheduleExactAlarms,
                    onRequestAppReminderPermissions = onRequestAppReminderPermissions,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToBackup = onNavigateToBackup,
                    onNavigateToWebdav = onNavigateToWebdav
                )
            }
        }
    }
}

@Composable
private fun BottomTabBar(
    screens: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp + bottomInset)
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = bottomInset + 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                BottomTabItem(
                    modifier = Modifier.weight(1f),
                    screen = screen,
                    selected = selected,
                    onClick = { onNavigate(screen.route) }
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    modifier: Modifier = Modifier,
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(screen.labelRes)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = if (selected) screen.selectedIconRes else screen.iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}
