package com.doginventory.ui.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.ui.theme.InventoryCategoryDefaults
import kotlinx.coroutines.launch
import java.util.UUID

class CategoryEditorViewModel(
    private val repository: InventoryRepository,
    private val categoryId: String? = null
) : ViewModel() {

    private var category: InventoryCategoryEntity? by mutableStateOf(null)

    val isNew: Boolean
        get() = categoryId == null
    var isLoading by mutableStateOf(categoryId != null)
        private set
    var isMissing by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
    var icon by mutableStateOf("📦")
    var color by mutableStateOf(InventoryCategoryDefaults.FOOD_COLOR_HEX)

    init {
        if (categoryId != null) {
            loadCategory(categoryId)
        }
    }

    private fun loadCategory(id: String) {
        viewModelScope.launch {
            isLoading = true
            val loadedCategory = repository.getCategoryById(id)
            if (loadedCategory == null) {
                isMissing = true
            } else {
                applyCategory(loadedCategory)
            }
            isLoading = false
        }
    }

    private fun applyCategory(category: InventoryCategoryEntity) {
        this.category = category
        name = category.name
        icon = category.icon
        color = category.color
    }

    fun save(onSuccess: () -> Unit) {
        if (isLoading || isMissing || name.isBlank()) return

        viewModelScope.launch {
            val categoryToUpdate = category ?: categoryId?.let { repository.getCategoryById(it) }
            if (!isNew && categoryToUpdate == null) {
                isMissing = true
                return@launch
            }

            val now = System.currentTimeMillis()
            val savedCategory = InventoryCategoryEntity(
                id = categoryToUpdate?.id ?: UUID.randomUUID().toString(),
                name = name,
                color = color,
                icon = icon,
                sortOrder = categoryToUpdate?.sortOrder ?: (now / 1000).toInt(),
                isPreset = categoryToUpdate?.isPreset ?: false,
                isDeleted = categoryToUpdate?.isDeleted ?: false,
                createdAt = categoryToUpdate?.createdAt ?: now,
                updatedAt = now
            )
            if (isNew) {
                repository.insertCategory(savedCategory)
            } else {
                repository.updateCategory(savedCategory)
            }
            onSuccess()
        }
    }
}
