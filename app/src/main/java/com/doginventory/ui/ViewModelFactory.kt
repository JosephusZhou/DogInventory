package com.doginventory.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.doginventory.backup.BackupArchiveService
import com.doginventory.backup.BackupRestoreCoordinator
import com.doginventory.backup.BackupStorageBridge
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.permission.StoragePermissionCoordinator
import com.doginventory.settings.PreferencesService
import com.doginventory.ui.inventory.CategoryEditorViewModel
import com.doginventory.ui.inventory.InventoryCategoriesViewModel
import com.doginventory.ui.inventory.InventoryDetailViewModel
import com.doginventory.ui.inventory.InventoryEditorViewModel
import com.doginventory.ui.inventory.InventoryViewModel
import com.doginventory.ui.shopping.ShoppingEditorViewModel
import com.doginventory.ui.shopping.ShoppingViewModel
import com.doginventory.ui.settings.SettingsBackupViewModel
import com.doginventory.ui.settings.SettingsWebdavSyncViewModel
import com.doginventory.webdav.WebDavAutoSyncScheduler
import com.doginventory.webdav.WebDavSyncService

class ViewModelFactory(
    private val repository: InventoryRepository,
    private val applicationContext: Context? = null,
    private val storagePermissionCoordinator: StoragePermissionCoordinator? = null,
    private val itemId: String? = null,
    private val category: InventoryCategoryEntity? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                InventoryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ShoppingViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ShoppingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ShoppingEditorViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ShoppingEditorViewModel(repository, itemId) as T
            }
            modelClass.isAssignableFrom(InventoryEditorViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                InventoryEditorViewModel(repository, itemId) as T
            }
            modelClass.isAssignableFrom(InventoryDetailViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                InventoryDetailViewModel(repository, itemId!!) as T
            }
            modelClass.isAssignableFrom(InventoryCategoriesViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                InventoryCategoriesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CategoryEditorViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                CategoryEditorViewModel(repository, category) as T
            }
            modelClass.isAssignableFrom(SettingsBackupViewModel::class.java) -> {
                val context = requireNotNull(applicationContext) { "applicationContext is required for SettingsBackupViewModel" }
                val coordinator = requireNotNull(storagePermissionCoordinator) { "storagePermissionCoordinator is required for SettingsBackupViewModel" }
                val preferencesService = PreferencesService(context)
                val webDavSyncService = WebDavSyncService(
                    context = context,
                    repository = repository,
                    preferencesService = preferencesService,
                    archiveService = BackupArchiveService(context, preferencesService)
                )
                preferencesService.webDavAutoSyncTrigger = WebDavAutoSyncScheduler(preferencesService, webDavSyncService)
                @Suppress("UNCHECKED_CAST")
                SettingsBackupViewModel(
                    resources = context.resources,
                    BackupRestoreCoordinator(
                        context = context,
                        repository = repository,
                        preferencesService = preferencesService,
                        archiveService = BackupArchiveService(context, preferencesService),
                        storageBridge = BackupStorageBridge(context)
                    ),
                    coordinator,
                    preferencesService
                ) as T
            }
            modelClass.isAssignableFrom(SettingsWebdavSyncViewModel::class.java) -> {
                val context = requireNotNull(applicationContext) { "applicationContext is required for SettingsWebdavSyncViewModel" }
                val preferencesService = PreferencesService(context)
                val webDavSyncService = WebDavSyncService(
                    context = context,
                    repository = repository,
                    preferencesService = preferencesService,
                    archiveService = BackupArchiveService(context, preferencesService)
                )
                preferencesService.webDavAutoSyncTrigger = WebDavAutoSyncScheduler(preferencesService, webDavSyncService)
                @Suppress("UNCHECKED_CAST")
                SettingsWebdavSyncViewModel(
                    resources = context.resources,
                    preferencesService = preferencesService,
                    webDavSyncService = webDavSyncService
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
