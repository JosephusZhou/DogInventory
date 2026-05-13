package com.doginventory.ui.shopping

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.ShoppingItemEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ShoppingHomeState(
    val pendingItems: List<ShoppingItemEntity> = emptyList(),
    val doneItems: List<ShoppingItemEntity> = emptyList()
)

data class ShoppingSearchState(
    val query: String = "",
    val pendingItems: List<ShoppingItemEntity> = emptyList(),
    val doneItems: List<ShoppingItemEntity> = emptyList()
)

@OptIn(FlowPreview::class)
class ShoppingViewModel(
    private val repository: InventoryRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<ShoppingHomeState> = repository.shoppingItems
        .map { items ->
            ShoppingHomeState(
                pendingItems = items.filter { !it.isDone },
                doneItems = items.filter { it.isDone }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingHomeState())

    val searchState: StateFlow<ShoppingSearchState> = combine(
        searchQuery,
        searchQuery.debounce(250),
        repository.shoppingItems
    ) { immediateQuery, debouncedQuery, items ->
        val keyword = debouncedQuery.trim()
        val filteredItems = if (keyword.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.name.contains(keyword, ignoreCase = true) ||
                    item.note.contains(keyword, ignoreCase = true)
            }
        }
        ShoppingSearchState(
            query = immediateQuery,
            pendingItems = filteredItems.filter { !it.isDone },
            doneItems = filteredItems.filter { it.isDone }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingSearchState())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleDone(item: ShoppingItemEntity, done: Boolean) {
        viewModelScope.launch {
            repository.updateShoppingItem(
                item.copy(
                    isDone = done,
                    doneAt = if (done) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteShoppingItem(id) }
    }

    fun deleteDoneItems() {
        viewModelScope.launch { repository.deleteDoneShoppingItems() }
    }
}

class ShoppingEditorViewModel(
    private val repository: InventoryRepository,
    val itemId: String?
) : ViewModel() {
    var name by mutableStateOf("")
    var note by mutableStateOf("")

    init {
        if (itemId != null) {
            viewModelScope.launch {
                val item = repository.getShoppingItemById(itemId) ?: return@launch
                name = item.name
                note = item.note
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (itemId == null) {
                repository.insertShoppingItem(
                    ShoppingItemEntity(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        note = note.trim(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                val current = repository.getShoppingItemById(itemId) ?: return@launch
                repository.updateShoppingItem(
                    current.copy(
                        name = name.trim(),
                        note = note.trim(),
                        updatedAt = now
                    )
                )
            }
            onSuccess()
        }
    }
}
