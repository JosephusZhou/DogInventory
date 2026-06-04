package com.doginventory.share

import org.json.JSONArray
import org.json.JSONObject

data class SharedReminderRuleDto(
    val kind: String,
    val enabled: Boolean,
    val daysBefore: Int? = null,
    val remindAt: Long? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("kind", kind)
        put("enabled", enabled)
        daysBefore?.let { put("daysBefore", it) }
        remindAt?.let { put("remindAt", it) }
    }

    companion object {
        fun fromJson(json: JSONObject): SharedReminderRuleDto = SharedReminderRuleDto(
            kind = json.optString("kind", "expire_offset"),
            enabled = json.optBoolean("enabled", true),
            daysBefore = if (json.has("daysBefore") && !json.isNull("daysBefore")) json.optInt("daysBefore") else null,
            remindAt = if (json.has("remindAt") && !json.isNull("remindAt")) json.optLong("remindAt") else null
        )
    }
}

data class SharedItemDto(
    val id: String,
    val name: String,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val quantityCurrent: Double?,
    val quantityUnit: String,
    val quantityLowThreshold: Double?,
    val expireAt: Long?,
    val note: String,
    val sortOrder: Int,
    val rules: List<SharedReminderRuleDto> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("categoryName", categoryName ?: JSONObject.NULL)
        put("categoryColor", categoryColor ?: JSONObject.NULL)
        put("categoryIcon", categoryIcon ?: JSONObject.NULL)
        if (quantityCurrent == null) put("quantityCurrent", JSONObject.NULL) else put("quantityCurrent", quantityCurrent)
        put("quantityUnit", quantityUnit)
        if (quantityLowThreshold == null) put("quantityLowThreshold", JSONObject.NULL) else put("quantityLowThreshold", quantityLowThreshold)
        if (expireAt == null) put("expireAt", JSONObject.NULL) else put("expireAt", expireAt)
        put("note", note)
        put("sortOrder", sortOrder)
        put("rules", JSONArray().also { arr -> rules.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): SharedItemDto = SharedItemDto(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            categoryName = json.optStringOrNull("categoryName"),
            categoryColor = json.optStringOrNull("categoryColor"),
            categoryIcon = json.optStringOrNull("categoryIcon"),
            quantityCurrent = if (json.has("quantityCurrent") && !json.isNull("quantityCurrent")) json.optDouble("quantityCurrent") else null,
            quantityUnit = json.optString("quantityUnit", ""),
            quantityLowThreshold = if (json.has("quantityLowThreshold") && !json.isNull("quantityLowThreshold")) json.optDouble("quantityLowThreshold") else null,
            expireAt = if (json.has("expireAt") && !json.isNull("expireAt")) json.optLong("expireAt") else null,
            note = json.optString("note", ""),
            sortOrder = json.optInt("sortOrder", 0),
            rules = parseRules(json.optJSONArray("rules"))
        )

        private fun parseRules(array: JSONArray?): List<SharedReminderRuleDto> {
            if (array == null) return emptyList()
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(SharedReminderRuleDto.fromJson(obj))
                }
            }
        }
    }
}

data class SharedCategoryDto(
    val id: String,
    val name: String,
    val color: String?,
    val icon: String?,
    val sortOrder: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("color", color ?: JSONObject.NULL)
        put("icon", icon ?: JSONObject.NULL)
        put("sortOrder", sortOrder)
    }

    companion object {
        fun fromJson(json: JSONObject): SharedCategoryDto = SharedCategoryDto(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            color = json.optStringOrNull("color"),
            icon = json.optStringOrNull("icon"),
            sortOrder = json.optInt("sortOrder", 0)
        )
    }
}

data class ShareCreateRequest(
    val title: String,
    val items: List<SharedItemDto>,
    val categories: List<SharedCategoryDto>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("items", JSONArray().also { arr -> items.forEach { arr.put(it.toJson()) } })
        put("categories", JSONArray().also { arr -> categories.forEach { arr.put(it.toJson()) } })
    }
}

data class ShareCreateResult(
    val shareId: String,
    val url: String,
    val expiresAt: Long
)

data class SharedList(
    val title: String,
    val createdAt: Long,
    val expiresAt: Long,
    val items: List<SharedItemDto>,
    val categories: List<SharedCategoryDto>
) {
    companion object {
        fun fromJson(json: JSONObject): SharedList = SharedList(
            title = json.optString("title", ""),
            createdAt = json.optLong("createdAt", 0L),
            expiresAt = json.optLong("expiresAt", 0L),
            items = parseItems(json.optJSONArray("items")),
            categories = parseCategories(json.optJSONArray("categories"))
        )

        private fun parseItems(array: JSONArray?): List<SharedItemDto> {
            if (array == null) return emptyList()
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(SharedItemDto.fromJson(obj))
                }
            }
        }

        private fun parseCategories(array: JSONArray?): List<SharedCategoryDto> {
            if (array == null) return emptyList()
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(SharedCategoryDto.fromJson(obj))
                }
            }
        }
    }
}

data class ShareImportResult(
    val importedItemCount: Int,
    val newCategoryCount: Int
)

private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null
