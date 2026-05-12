package com.doginventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Reuse inventory_categories_screen state
data class InventoryCategoriesState(
    val categories: List<InventoryCategoryEntity> = emptyList()
)

class InventoryCategoriesViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    val state: StateFlow<InventoryCategoriesState> = repository.allCategories
        .map { categories ->
            InventoryCategoriesState(
                categories = categories.sortedWith(
                    compareBy<InventoryCategoryEntity> { !it.isPreset }
                        .thenBy { it.sortOrder }
                        .thenBy { it.createdAt }
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryCategoriesState())

    fun moveUp(category: InventoryCategoryEntity) {
        viewModelScope.launch {
            val currentList = state.value.categories
                .filter { it.isPreset == category.isPreset }
                .sortedWith(
                compareBy<InventoryCategoryEntity> { it.sortOrder }.thenBy { it.createdAt }
            )
            val index = currentList.indexOfFirst { it.id == category.id }
            if (index > 0) {
                val target = currentList[index - 1]
                repository.updateCategories(
                    categories = listOf(
                        category.copy(sortOrder = target.sortOrder),
                        target.copy(sortOrder = category.sortOrder)
                    ),
                    syncReason = "inventory_categories_reordered"
                )
            }
        }
    }

    fun moveDown(category: InventoryCategoryEntity) {
        viewModelScope.launch {
            val currentList = state.value.categories
                .filter { it.isPreset == category.isPreset }
                .sortedWith(
                compareBy<InventoryCategoryEntity> { it.sortOrder }.thenBy { it.createdAt }
            )
            val index = currentList.indexOfFirst { it.id == category.id }
            if (index >= 0 && index < currentList.size - 1) {
                val target = currentList[index + 1]
                repository.updateCategories(
                    categories = listOf(
                        category.copy(sortOrder = target.sortOrder),
                        target.copy(sortOrder = category.sortOrder)
                    ),
                    syncReason = "inventory_categories_reordered"
                )
            }
        }
    }

    fun deleteCategory(category: InventoryCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
