package com.doginventory.ui.theme

enum class AppThemeMode(val preferenceValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromPreferenceValue(value: String?): AppThemeMode = when (value) {
            Light.preferenceValue -> Light
            Dark.preferenceValue -> Dark
            else -> System
        }
    }
}
