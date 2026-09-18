package com.example.gains.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("gains_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _hiddenMetricNames = MutableStateFlow(getSavedHiddenMetricNames())
    val hiddenMetricNames: StateFlow<Set<String>> = _hiddenMetricNames.asStateFlow()

    private val _autoLoadPreviousPerformance = MutableStateFlow(getSavedAutoLoadPreviousPerformance())
    val autoLoadPreviousPerformance: StateFlow<Boolean> = _autoLoadPreviousPerformance.asStateFlow()

    fun getSavedThemeMode(): String {
        return sharedPreferences.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(mode: String) {
        sharedPreferences.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun getSavedHiddenMetricNames(): Set<String> {
        return sharedPreferences.getStringSet("hidden_metric_names", emptySet()) ?: emptySet()
    }

    fun toggleMetricVisibility(metricName: String, isVisible: Boolean) {
        val current = getSavedHiddenMetricNames().toMutableSet()
        if (isVisible) {
            current.remove(metricName)
        } else {
            current.add(metricName)
        }
        sharedPreferences.edit().putStringSet("hidden_metric_names", current).apply()
        _hiddenMetricNames.value = current
    }

    fun getSavedAutoLoadPreviousPerformance(): Boolean {
        return sharedPreferences.getBoolean("auto_load_previous_performance", true)
    }

    fun setAutoLoadPreviousPerformance(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("auto_load_previous_performance", enabled).apply()
        _autoLoadPreviousPerformance.value = enabled
    }
}
