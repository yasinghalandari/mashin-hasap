package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AngleUnit
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hesabgar_prefs")

data class HistoryEntry(
    val id: String,
    val expression: String,
    val resultText: String,
    val numericValue: Double,
    val timestampMillis: Long
)

data class SavedResultEntry(
    val id: String,
    val name: String,
    val expression: String,
    val resultText: String,
    val numericValue: Double,
    val createdAtMillis: Long
)

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val usePersianDigits: Boolean = true,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val hapticEnabled: Boolean = true,
    val history: List<HistoryEntry> = emptyList(),
    val savedResults: List<SavedResultEntry> = emptyList()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HISTORY_JSON = stringPreferencesKey("history_json")
        val SAVED_RESULTS_JSON = stringPreferencesKey("saved_results_json")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]?.let { name ->
            AppThemeMode.entries.firstOrNull { it.name == name }
        } ?: AppThemeMode.DARK

        val usePersianDigits = prefs[Keys.PERSIAN_DIGITS] ?: true

        val angleUnit = prefs[Keys.ANGLE_UNIT]?.let { name ->
            AngleUnit.entries.firstOrNull { it.name == name }
        } ?: AngleUnit.DEG

        val hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true

        val history = decodeHistory(prefs[Keys.HISTORY_JSON])
        val savedResults = decodeSavedResults(prefs[Keys.SAVED_RESULTS_JSON])

        UserPreferences(
            themeMode = themeMode,
            usePersianDigits = usePersianDigits,
            angleUnit = angleUnit,
            hapticEnabled = hapticEnabled,
            history = history,
            savedResults = savedResults
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUsePersianDigits(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PERSIAN_DIGITS] = enabled }
    }

    suspend fun setAngleUnit(unit: AngleUnit) {
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun addHistoryEntry(entry: HistoryEntry) {
        context.dataStore.edit { prefs ->
            val current = decodeHistory(prefs[Keys.HISTORY_JSON]).toMutableList()
            if (current.firstOrNull()?.expression == entry.expression &&
                current.firstOrNull()?.resultText == entry.resultText
            ) {
                return@edit
            }
            current.add(0, entry)
            val trimmed = current.take(100)
            prefs[Keys.HISTORY_JSON] = encodeHistory(trimmed)
        }
    }

    suspend fun deleteHistoryEntry(id: String) {
        context.dataStore.edit { prefs ->
            val filtered = decodeHistory(prefs[Keys.HISTORY_JSON]).filterNot { it.id == id }
            prefs[Keys.HISTORY_JSON] = encodeHistory(filtered)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HISTORY_JSON] = "[]"
        }
    }

    suspend fun addSavedResult(entry: SavedResultEntry) {
        context.dataStore.edit { prefs ->
            val current = decodeSavedResults(prefs[Keys.SAVED_RESULTS_JSON]).toMutableList()
            current.add(0, entry)
            prefs[Keys.SAVED_RESULTS_JSON] = encodeSavedResults(current)
        }
    }

    suspend fun renameSavedResult(id: String, newName: String) {
        context.dataStore.edit { prefs ->
            val updated = decodeSavedResults(prefs[Keys.SAVED_RESULTS_JSON]).map { item ->
                if (item.id == id) item.copy(name = newName) else item
            }
            prefs[Keys.SAVED_RESULTS_JSON] = encodeSavedResults(updated)
        }
    }

    suspend fun deleteSavedResult(id: String) {
        context.dataStore.edit { prefs ->
            val filtered = decodeSavedResults(prefs[Keys.SAVED_RESULTS_JSON]).filterNot { it.id == id }
            prefs[Keys.SAVED_RESULTS_JSON] = encodeSavedResults(filtered)
        }
    }

    private fun encodeHistory(list: List<HistoryEntry>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("expression", item.expression)
            obj.put("resultText", item.resultText)
            obj.put("numericValue", item.numericValue)
            obj.put("timestampMillis", item.timestampMillis)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeHistory(json: String?): List<HistoryEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        HistoryEntry(
                            id = obj.optString("id", "$i"),
                            expression = obj.optString("expression", ""),
                            resultText = obj.optString("resultText", ""),
                            numericValue = obj.optDouble("numericValue", 0.0),
                            timestampMillis = obj.optLong("timestampMillis", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeSavedResults(list: List<SavedResultEntry>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("expression", item.expression)
            obj.put("resultText", item.resultText)
            obj.put("numericValue", item.numericValue)
            obj.put("createdAtMillis", item.createdAtMillis)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeSavedResults(json: String?): List<SavedResultEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SavedResultEntry(
                            id = obj.optString("id", "$i"),
                            name = obj.optString("name", ""),
                            expression = obj.optString("expression", ""),
                            resultText = obj.optString("resultText", ""),
                            numericValue = obj.optDouble("numericValue", 0.0),
                            createdAtMillis = obj.optLong("createdAtMillis", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
