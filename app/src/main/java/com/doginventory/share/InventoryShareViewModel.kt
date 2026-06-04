package com.doginventory.share

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

data class InventoryShareState(
    val title: String = "",
    val includeReminderRules: Boolean = true,
    val isSharing: Boolean = false,
    val shareUrl: String? = null,
    val shareError: String? = null,
    val availableItemIds: List<String> = emptyList(),
    val selectedItemIds: Set<String> = emptySet()
) {
    val selectedCount: Int get() = selectedItemIds.size
    val totalCount: Int get() = availableItemIds.size
}

class InventoryShareViewModel(
    private val shareService: ShareService,
    private val repository: InventoryRepository
) : ViewModel() {
    private var allItems: List<InventoryItemEntity> = emptyList()
    private var allCategories: List<InventoryCategoryEntity> = emptyList()

    private val _state = mutableStateOf(InventoryShareState())
    val state: State<InventoryShareState> = _state

    private var current: InventoryShareState
        get() = _state.value
        set(value) { _state.value = value }

    fun setTitle(value: String) {
        current = current.copy(title = value)
    }

    fun setIncludeReminderRules(value: Boolean) {
        current = current.copy(includeReminderRules = value)
    }

    fun updateContext(
        filteredItems: List<InventoryItemEntity>,
        allItems: List<InventoryItemEntity>,
        allCategories: List<InventoryCategoryEntity>
    ) {
        this.allItems = allItems
        this.allCategories = allCategories
        val ids = allItems.map { it.id }
        current = current.copy(
            availableItemIds = ids,
            selectedItemIds = ids.toSet()
        )
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

    fun selectAll() {
        current = current.copy(selectedItemIds = current.availableItemIds.toSet())
    }

    fun deselectAll() {
        current = current.copy(selectedItemIds = emptySet())
    }

    fun reset() {
        _state.value = InventoryShareState()
    }

    fun createShare() {
        viewModelScope.launch {
            current = current.copy(isSharing = true, shareError = null)
            try {
                val selectedIds = current.selectedItemIds
                val sourceItems = allItems.filter { it.id in selectedIds }
                if (sourceItems.isEmpty()) {
                    current = current.copy(isSharing = false, shareError = "empty")
                    return@launch
                }
                val rulesByItemId = if (current.includeReminderRules) {
                    val allRules = repository.watchAllRules().first()
                    allRules.groupBy { it.itemId }
                } else {
                    emptyMap()
                }
                val categoryMap = allCategories.associateBy { it.id }
                val touchedCategoryIds = sourceItems.mapNotNull { it.categoryId }.toSet()
                val sharedCategories = allCategories
                    .filter { it.id in touchedCategoryIds }
                    .map { c ->
                        SharedCategoryDto(
                            id = c.id,
                            name = c.name,
                            color = c.color,
                            icon = c.icon,
                            sortOrder = c.sortOrder
                        )
                    }
                val sharedItems = sourceItems.mapIndexed { index, item ->
                    val category = item.categoryId?.let { categoryMap[it] }
                    val rules = (rulesByItemId[item.id] ?: emptyList()).map { it.toDto() }
                    SharedItemDto(
                        id = item.id,
                        name = item.name,
                        categoryName = category?.name,
                        categoryColor = category?.color,
                        categoryIcon = category?.icon,
                        quantityCurrent = item.quantityCurrent,
                        quantityUnit = item.quantityUnit,
                        quantityLowThreshold = item.quantityLowThreshold,
                        expireAt = item.expireAt,
                        note = item.note,
                        sortOrder = index,
                        rules = rules
                    )
                }
                val finalTitle = current.title.ifBlank { "我的存货" }
                val request = ShareCreateRequest(
                    title = finalTitle,
                    items = sharedItems,
                    categories = sharedCategories
                )
                val result = shareService.createShare(request)
                current = current.copy(isSharing = false, shareUrl = result.url)
            } catch (e: ShareApiException) {
                current = current.copy(isSharing = false, shareError = mapError(e))
            } catch (e: Exception) {
                current = current.copy(isSharing = false, shareError = "network")
            }
        }
    }

    private fun mapError(e: ShareApiException): String = when (e) {
        is ShareApiException.Http -> "http:${e.code}"
        is ShareApiException.Network -> "network"
        is ShareApiException.Parse -> "network"
    }

    private fun InventoryReminderRuleEntity.toDto(): SharedReminderRuleDto {
        val payload = runCatching { JSONObject(payloadJson) }.getOrElse { JSONObject() }
        return SharedReminderRuleDto(
            kind = kind,
            enabled = enabled,
            daysBefore = if (payload.has("daysBefore") && !payload.isNull("daysBefore")) payload.optInt("daysBefore") else null,
            remindAt = if (payload.has("remindAt") && !payload.isNull("remindAt")) payload.optLong("remindAt") else null
        )
    }
}
