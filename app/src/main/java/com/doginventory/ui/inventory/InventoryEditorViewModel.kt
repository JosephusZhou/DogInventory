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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.*

class InventoryEditorViewModel(
    private val repository: InventoryRepository,
    val itemId: String?
) : ViewModel() {
    var name by mutableStateOf("")
    var note by mutableStateOf("")
    var selectedCategoryId by mutableStateOf<String?>(null)
    var expireAt by mutableStateOf<Long?>(null)
    var rules by mutableStateOf<List<InventoryReminderDraft>>(emptyList())
    
    private val _categories = MutableStateFlow<List<InventoryCategoryEntity>>(emptyList())
    val categories: StateFlow<List<InventoryCategoryEntity>> = _categories.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allCategories.collect {
                _categories.value = it
            }
        }
        
        if (itemId != null) {
            viewModelScope.launch {
                val item = repository.watchItemById(itemId).first() ?: return@launch
                name = item.name
                note = item.note
                selectedCategoryId = item.categoryId
                expireAt = item.expireAt
                
                repository.watchRulesByItemId(itemId).collect {
                    rules = it.map(InventoryReminderDraft::fromEntity).sortedForDisplay()
                }
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val id = itemId ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val item = InventoryItemEntity(
                id = id,
                name = name,
                categoryId = selectedCategoryId,
                quantityCurrent = null,
                quantityLowThreshold = null,
                expireAt = expireAt,
                note = note,
                createdAt = if (itemId == null) now else now, // Simplified
                updatedAt = now
            )
            val entities = if (expireAt == null) emptyList() else rules.map { it.toEntity(id, now) }
            
            if (itemId == null) {
                repository.insertItem(item, entities)
            } else {
                repository.updateItem(item, entities)
            }
            onSuccess()
        }
    }

    fun setExpireAtAndDefaults(value: Long?) {
        val shouldCreateDefaults = expireAt == null && value != null && rules.isEmpty()
        expireAt = value
        rules = if (value == null) {
            emptyList()
        } else if (shouldCreateDefaults) {
            INVENTORY_DEFAULT_OFFSET_DAYS.map { InventoryReminderDraft.defaultOffset(it, value) }
        } else {
            rules
        }.sortedForDisplay()
    }

    fun addOffsetRule(daysBefore: Int) {
        if (rules.any { it.kind == "expire_offset" && it.daysBefore == daysBefore }) return
        rules = (rules + InventoryReminderDraft.defaultOffset(daysBefore, expireAt)).sortedForDisplay()
    }

    fun addAbsoluteRule(remindAt: Long) {
        rules = (rules + InventoryReminderDraft.absolute(remindAt)).sortedForDisplay()
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        rules = rules.map { if (it.id == ruleId) it.copy(enabled = enabled) else it }.sortedForDisplay()
    }

    fun removeRule(ruleId: String) {
        rules = rules.filterNot { it.id == ruleId }.sortedForDisplay()
    }
}

private fun List<InventoryReminderDraft>.sortedForDisplay(): List<InventoryReminderDraft> = sortedWith(
    compareBy<InventoryReminderDraft> { if (it.kind == "expire_offset") 0 else 1 }
        .thenByDescending { it.daysBefore ?: 0 }
        .thenBy { it.remindAt ?: Long.MAX_VALUE }
)
