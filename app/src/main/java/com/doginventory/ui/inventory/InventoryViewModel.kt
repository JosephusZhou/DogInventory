package com.doginventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*

data class InventoryHomeState(
    val items: List<InventoryItemUiModel> = emptyList(),
    val allItems: List<InventoryItemUiModel> = emptyList(),
    val categories: List<InventoryCategoryEntity> = emptyList(),
    val filter: InventoryFilter = InventoryFilter.All,
    val soonCount: Int = 0,
    val expiredCount: Int = 0,
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
    val activeRuleCount: Int = 0,
    val isExpired: Boolean = false,
    val isSoon: Boolean = false,
    val expireDateText: String? = null
)

enum class InventoryFilter {
    All, Soon, Expired
}

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val _filter = MutableStateFlow(InventoryFilter.All)

    private val allUiModels: Flow<List<InventoryItemUiModel>> = combine(
        repository.activeItems,
        repository.allCategories,
        repository.watchAllRules()
    ) { items, categories, rules ->
        val now = System.currentTimeMillis()
        val categoryMap = categories.associateBy { it.id }
        val activeRuleCounts = rules.asSequence()
            .filter { it.enabled }
            .groupingBy { it.itemId }
            .eachCount()
        items.map { item ->
            val isExpired = isInventoryExpired(item.expireAt, now)
            val isSoon = isInventorySoon(item.expireAt, now)
            InventoryItemUiModel(
                item = item,
                category = categoryMap[item.categoryId],
                activeRuleCount = activeRuleCounts[item.id] ?: 0,
                isExpired = isExpired,
                isSoon = isSoon,
                expireDateText = item.expireAt?.let(::formatInventoryDate)
            )
        }
    }

    val state: StateFlow<InventoryHomeState> = combine(
        repository.allCategories,
        allUiModels,
        _filter
    ) { categories, uiModels, filter ->
        val visibleModels = uiModels.filter {
            when (filter) {
                InventoryFilter.All -> true
                InventoryFilter.Soon -> it.isSoon
                InventoryFilter.Expired -> it.isExpired
            }
        }
        InventoryHomeState(
            items = visibleModels,
            allItems = uiModels,
            categories = categories,
            filter = filter,
            soonCount = uiModels.count { it.isSoon },
            expiredCount = uiModels.count { it.isExpired },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryHomeState())

    fun setFilter(filter: InventoryFilter) {
        _filter.value = filter
    }
}
