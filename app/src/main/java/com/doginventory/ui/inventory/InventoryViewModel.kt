package com.doginventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

data class InventoryHomeState(
    val items: List<InventoryItemUiModel> = emptyList(),
    val allItems: List<InventoryItemUiModel> = emptyList(),
    val categories: List<InventoryCategoryEntity> = emptyList(),
    val filter: InventoryFilter = InventoryFilter.All,
    val sortOrder: InventorySortOrder = InventorySortOrder.Default,
    val selectedCategoryId: String? = null,
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

data class InventorySearchState(
    val query: String = "",
    val items: List<InventoryItemUiModel> = emptyList()
)

enum class InventoryFilter {
    All, Soon, Expired
}

enum class InventorySortOrder {
    Default,
    ExpireAtAscending,
    ExpireAtDescending
}

@OptIn(FlowPreview::class)
class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val _filter = MutableStateFlow(InventoryFilter.All)
    private val _sortOrder = MutableStateFlow(InventorySortOrder.Default)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

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
        _filter,
        _sortOrder,
        _selectedCategoryId
    ) { categories, uiModels, filter, sortOrder, selectedCategoryId ->
        val effectiveSelectedCategoryId = selectedCategoryId?.takeIf { id ->
            categories.any { it.id == id }
        }
        val sortedUiModels = uiModels.sortedForDisplay(sortOrder)
        val visibleModels = sortedUiModels.filter {
            val matchesFilter = when (filter) {
                InventoryFilter.All -> true
                InventoryFilter.Soon -> it.isSoon
                InventoryFilter.Expired -> it.isExpired
            }
            val matchesCategory = effectiveSelectedCategoryId == null || it.item.categoryId == effectiveSelectedCategoryId
            matchesFilter && matchesCategory
        }
        InventoryHomeState(
            items = visibleModels,
            allItems = sortedUiModels,
            categories = categories,
            filter = filter,
            sortOrder = sortOrder,
            selectedCategoryId = effectiveSelectedCategoryId,
            soonCount = uiModels.count { it.isSoon },
            expiredCount = uiModels.count { it.isExpired },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryHomeState())

    fun setFilter(filter: InventoryFilter) {
        _filter.value = filter
    }

    fun toggleSortOrder() {
        _sortOrder.value = when (_sortOrder.value) {
            InventorySortOrder.Default -> InventorySortOrder.ExpireAtAscending
            InventorySortOrder.ExpireAtAscending -> InventorySortOrder.ExpireAtDescending
            InventorySortOrder.ExpireAtDescending -> InventorySortOrder.Default
        }
    }

    fun setSelectedCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun resetFilters() {
        _filter.value = InventoryFilter.All
        _selectedCategoryId.value = null
    }

    val searchState: StateFlow<InventorySearchState> = combine(
        _searchQuery,
        _searchQuery.debounce(300),
        allUiModels
    ) { immediateQuery, debouncedQuery, uiModels ->
        val keyword = debouncedQuery.trim()
        val filteredItems = if (keyword.isBlank()) {
            uiModels
        } else {
            uiModels.filter { model ->
                model.item.name.contains(keyword, ignoreCase = true) ||
                    (model.category?.name?.contains(keyword, ignoreCase = true) == true)
            }
        }
        InventorySearchState(
            query = immediateQuery,
            items = filteredItems
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventorySearchState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

private fun List<InventoryItemUiModel>.sortedForDisplay(sortOrder: InventorySortOrder): List<InventoryItemUiModel> =
    when (sortOrder) {
        InventorySortOrder.Default -> this
        InventorySortOrder.ExpireAtAscending -> sortedWith(
            compareBy<InventoryItemUiModel> { it.item.expireAt == null }
                .thenBy { it.item.expireAt ?: Long.MAX_VALUE }
                .thenByDescending { it.item.updatedAt }
        )
        InventorySortOrder.ExpireAtDescending -> sortedWith(
            compareBy<InventoryItemUiModel> { it.item.expireAt == null }
                .thenByDescending { it.item.expireAt ?: Long.MIN_VALUE }
                .thenByDescending { it.item.updatedAt }
        )
    }
