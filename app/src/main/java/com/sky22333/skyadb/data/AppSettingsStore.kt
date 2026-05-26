package com.sky22333.skyadb.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val defaultPort: Int = 5555,
    val connectionTimeoutSeconds: Int = 10,
    val commandTimeoutSeconds: Int = 30,
    val scanRanges: String = "",
    val themeMode: ThemeMode = ThemeMode.System,
)

enum class ThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色"),
}

class AppSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            AppSettings(
                defaultPort = preferences[Keys.DefaultPort] ?: 5555,
                connectionTimeoutSeconds = preferences[Keys.ConnectionTimeoutSeconds] ?: 10,
                commandTimeoutSeconds = preferences[Keys.CommandTimeoutSeconds] ?: 30,
                scanRanges = preferences[Keys.ScanRanges].orEmpty(),
                themeMode = ThemeMode.entries.firstOrNull {
                    it.name == preferences[Keys.ThemeMode]
                } ?: ThemeMode.System,
            )
        }

    suspend fun updateDefaultPort(port: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.DefaultPort] = port
        }
    }

    suspend fun updateConnectionTimeoutSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.ConnectionTimeoutSeconds] = seconds
        }
    }

    suspend fun updateCommandTimeoutSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.CommandTimeoutSeconds] = seconds
        }
    }

    suspend fun updateScanRanges(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ScanRanges] = value
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.ThemeMode] = themeMode.name
        }
    }

    private object Keys {
        val DefaultPort = intPreferencesKey("default_port")
        val ConnectionTimeoutSeconds = intPreferencesKey("connection_timeout_seconds")
        val CommandTimeoutSeconds = intPreferencesKey("command_timeout_seconds")
        val ScanRanges = stringPreferencesKey("scan_ranges")
        val ThemeMode = stringPreferencesKey("theme_mode")
    }
}
