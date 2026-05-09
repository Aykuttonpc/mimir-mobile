package com.aykutcincik.mimir.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "mimir_theme")
private val keyMode = stringPreferencesKey("mode")

class ThemePreference(private val context: Context) {
    val mode: Flow<ThemeMode> = context.themeDataStore.data.map { p ->
        when (p[keyMode]) {
            "Light" -> ThemeMode.Light
            "Dark" -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    suspend fun set(mode: ThemeMode) {
        context.themeDataStore.edit { it[keyMode] = mode.name }
    }
}
