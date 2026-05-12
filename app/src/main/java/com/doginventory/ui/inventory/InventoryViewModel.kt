package com.doginventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryHomeState(
    val items: List<InventoryItemUiModel> = emptyList(),
    val allItems: List<InventoryItemUiModel> = emptyList(),
    val categories: List<InventoryCategoryEntity> = emptyList(),
    val filter: InventoryFilter = InventoryFilter.All,
    val isLoading: Boolean = true
) {
    val hasItems: Boolean
        get() = allItems.isNotEmpty()

    val isFilterEmpty: Boolean
        get() = hasItems && items.isEmpty()
}

data class InventoryItemUiModel(
    val item: InventoryItemEntity,
    val category: InventoryCategoryEntity?,
    val activeRuleCount: Int = 0
)

enum class InventoryFilter {
    All, Soon, Expired
}

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val _filter = MutableStateFlow(InventoryFilter.All)
    
    val state: StateFlow<InventoryHomeState> = combine(
        repository.activeItems,
        repository.allCategories,
        repository.watchAllRules(),
        _filter
    ) { items, categories, rules, filter ->
        val categoryMap = categories.associateBy { it.id }
        val activeRuleCounts = rules.filter { it.enabled }.groupingBy { it.itemId }.eachCount()
        val uiModels = items.map { 
            InventoryItemUiModel(it, categoryMap[it.categoryId], activeRuleCounts[it.id] ?: 0)
        }
        val visibleModels = uiModels.filter {
            when (filter) {
                InventoryFilter.All -> true
                InventoryFilter.Soon -> isInventorySoon(it.item.expireAt)
                InventoryFilter.Expired -> isInventoryExpired(it.item.expireAt)
            }
        }
        InventoryHomeState(
            items = visibleModels,
            allItems = uiModels,
            categories = categories,
            filter = filter,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryHomeState())

    fun setFilter(filter: InventoryFilter) {
        _filter.value = filter
    }
}
