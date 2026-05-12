package com.doginventory.ui.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryDetailState(
    val item: InventoryItemEntity? = null,
    val category: InventoryCategoryEntity? = null,
    val rules: List<InventoryReminderRuleEntity> = emptyList(),
    val isExpired: Boolean = false,
    val isSoon: Boolean = false
)

class InventoryDetailViewModel(
    private val repository: InventoryRepository,
    val itemId: String
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryDetailState())
    val state: StateFlow<InventoryDetailState> = _state.asStateFlow()

    var confirmDelete by mutableStateOf(false)

    init {
        viewModelScope.launch {
            combine(
                repository.watchItemById(itemId),
                repository.allCategories,
                repository.watchRulesByItemId(itemId)
            ) { item, categories, rules ->
                val category = item?.categoryId?.let { id -> categories.find { it.id == id } }
                InventoryDetailState(
                    item = item,
                    category = category,
                    rules = rules,
                    isExpired = item?.expireAt?.let { it <= System.currentTimeMillis() } ?: false,
                    isSoon = item?.expireAt?.let {
                        val now = System.currentTimeMillis()
                        val soonThreshold = now + 30L * 24 * 60 * 60 * 1000
                        it in (now + 1)..soonThreshold
                    } ?: false
                )
            }.collect { _state.value = it }
        }
    }

    fun deleteItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            onDeleted()
        }
    }
}
