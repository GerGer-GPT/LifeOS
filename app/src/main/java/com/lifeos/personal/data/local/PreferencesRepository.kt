package com.lifeos.personal.data.local

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeos_preferences")

class PreferencesRepository(private val context: Context) {
    val updateTestValue: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_TEST_VALUE] ?: 0
    }

    suspend fun incrementUpdateTestValue() {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_TEST_VALUE] = (preferences[UPDATE_TEST_VALUE] ?: 0) + 1
        }
    }

    private companion object {
        val UPDATE_TEST_VALUE = intPreferencesKey("update_test_value")
    }
}
