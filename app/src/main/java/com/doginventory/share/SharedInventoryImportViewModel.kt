package com.doginventory.share

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.R
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import java.net.HttpURLConnection

data class SharedInventoryImportState(
    val isLoading: Boolean = true,
    val list: SharedList? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val importReminderRules: Boolean = true,
    val isImporting: Boolean = false,
    val result: ShareImportResult? = null,
    val error: String? = null
)

class SharedInventoryImportViewModel(
    private val shareService: ShareService,
    private val repository: InventoryRepository,
    private val context: Context
) : ViewModel() {
    private var loadedShareId: String? = null

    private val _state = mutableStateOf(SharedInventoryImportState())
    val state: State<SharedInventoryImportState> = _state

    private var current: SharedInventoryImportState
        get() = _state.value
        set(value) { _state.value = value }

    fun loadIfNeeded(shareId: String) {
        if (loadedShareId == shareId && current.list != null) return
        loadedShareId = shareId
        load(shareId)
    }

    fun load(shareId: String) {
        viewModelScope.launch {
            current = current.copy(isLoading = true, error = null, result = null)
            try {
                val list = shareService.fetchShare(shareId)
                if (list.items.isEmpty()) {
                    current = current.copy(
                        isLoading = false,
                        list = list,
                        error = context.getString(R.string.share_import_error_empty)
                    )
                } else {
                    current = current.copy(
                        isLoading = false,
                        list = list,
                        selectedItemIds = list.items.map { it.id }.toSet()
                    )
                }
            } catch (e: ShareApiException) {
                current = current.copy(isLoading = false, error = mapError(e))
            } catch (e: Exception) {
                current = current.copy(isLoading = false, error = context.getString(R.string.share_import_error_network))
            }
        }
    }

    fun toggleItem(itemId: String, selected: Boolean) {
        current = current.copy(
            selectedItemIds = if (selected) {
                current.selectedItemIds + itemId
            } else {
                current.selectedItemIds - itemId
            }
        )
    }

    fun setImportReminderRules(value: Boolean) {
        current = current.copy(importReminderRules = value)
    }

    fun selectAll() {
        val list = current.list ?: return
        current = current.copy(selectedItemIds = list.items.map { it.id }.toSet())
    }

    fun deselectAll() {
        current = current.copy(selectedItemIds = emptySet())
    }

    fun import() {
        val list = current.list ?: return
        val toImport = list.items.filter { it.id in current.selectedItemIds }
        if (toImport.isEmpty()) return
        viewModelScope.launch {
            current = current.copy(isImporting = true, error = null)
            try {
                val result = repository.insertItemsFromShare(
                    sharedItems = toImport,
                    sharedCategories = list.categories,
                    importRules = current.importReminderRules
                )
                current = current.copy(isImporting = false, result = result)
            } catch (e: Exception) {
                current = current.copy(
                    isImporting = false,
                    error = e.message ?: context.getString(R.string.share_import_error_network)
                )
            }
        }
    }

    private fun mapError(e: ShareApiException): String = when (e) {
        is ShareApiException.Http -> {
            if (e.code == HttpURLConnection.HTTP_NOT_FOUND) {
                context.getString(R.string.share_import_error_not_found)
            } else {
                context.getString(R.string.share_error_http, e.code)
            }
        }
        is ShareApiException.Network -> context.getString(R.string.share_import_error_network)
        is ShareApiException.Parse -> context.getString(R.string.share_import_error_network)
    }
}
