package com.lifeos.personal.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeos_preferences")

data class UserProfile(
    val age: Int = 29,
    val heightCm: Int = 170,
    val weightKg: Int = 98,
    val goal: String = "Снижать жир и сохранять мышцы",
)

data class DayPlanItem(
    val id: Long,
    val time: String,
    val title: String,
    val done: Boolean = false,
)

class PreferencesRepository(private val context: Context) {
    val updateTestValue: Flow<Int> = context.dataStore.data.map { it[UPDATE_TEST_VALUE] ?: 0 }

    val profile: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        UserProfile(
            age = preferences[AGE] ?: 29,
            heightCm = preferences[HEIGHT] ?: 170,
            weightKg = preferences[WEIGHT] ?: 98,
            goal = preferences[GOAL] ?: "Снижать жир и сохранять мышцы",
        )
    }

    val dayPlan: Flow<List<DayPlanItem>> = context.dataStore.data.map { preferences ->
        decodePlan(preferences[DAY_PLAN]).ifEmpty { defaultPlan() }
    }

    suspend fun incrementUpdateTestValue() {
        context.dataStore.edit { it[UPDATE_TEST_VALUE] = (it[UPDATE_TEST_VALUE] ?: 0) + 1 }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit {
            it[AGE] = profile.age
            it[HEIGHT] = profile.heightCm
            it[WEIGHT] = profile.weightKg
            it[GOAL] = profile.goal
        }
    }

    suspend fun savePlan(items: List<DayPlanItem>) {
        context.dataStore.edit { it[DAY_PLAN] = encodePlan(items) }
    }

    private fun encodePlan(items: List<DayPlanItem>): String = items.joinToString("\n") { item ->
        listOf(item.id, item.time, item.title.replace("|", " ").replace("\n", " "), item.done).joinToString("|")
    }

    private fun decodePlan(value: String?): List<DayPlanItem> = value.orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 4) return@mapNotNull null
        DayPlanItem(parts[0].toLongOrNull() ?: return@mapNotNull null, parts[1], parts[2], parts[3].toBoolean())
    }.toList()

    private fun defaultPlan() = listOf(
        DayPlanItem(1, "08:00", "Завтрак и утреннее самочувствие"),
        DayPlanItem(2, "08:30", "Щадящая зарядка — 10 минут"),
        DayPlanItem(3, "09:00", "Основная работа"),
        DayPlanItem(4, "13:00", "Обед и короткий отдых"),
        DayPlanItem(5, "18:30", "Прогулка или лёгкая тренировка"),
        DayPlanItem(6, "22:30", "Подготовка ко сну"),
    )

    private companion object {
        val UPDATE_TEST_VALUE = intPreferencesKey("update_test_value")
        val AGE = intPreferencesKey("profile_age")
        val HEIGHT = intPreferencesKey("profile_height")
        val WEIGHT = intPreferencesKey("profile_weight")
        val GOAL = stringPreferencesKey("profile_goal")
        val DAY_PLAN = stringPreferencesKey("day_plan")
    }
}
