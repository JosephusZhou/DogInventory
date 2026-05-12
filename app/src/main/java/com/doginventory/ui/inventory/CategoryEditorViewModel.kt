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
    private val category: InventoryCategoryEntity? = null
) : ViewModel() {

    val isNew: Boolean = category == null
    var name by mutableStateOf(category?.name ?: "")
    var icon by mutableStateOf(category?.icon ?: "📦")
    var color by mutableStateOf(category?.color ?: InventoryCategoryDefaults.FOOD_COLOR_HEX)

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val id = category?.id ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val newCategory = InventoryCategoryEntity(
                id = id,
                name = name,
                color = color,
                icon = icon,
                sortOrder = category?.sortOrder ?: (System.currentTimeMillis() / 1000).toInt(),
                isPreset = false,
                isDeleted = false,
                createdAt = category?.createdAt ?: now,
                updatedAt = now
            )
            if (isNew) {
                repository.insertCategory(newCategory)
            } else {
                repository.updateCategory(newCategory)
            }
            onSuccess()
        }
    }
}
