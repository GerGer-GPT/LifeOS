package com.lifeos.personal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.personal.data.local.DayPlanItem
import com.lifeos.personal.data.local.PreferencesRepository
import com.lifeos.personal.data.local.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lifeos.personal.data.health.HealthConnectRepository
import com.lifeos.personal.data.health.HealthSnapshot
import com.lifeos.personal.data.google.GoogleSyncRepository
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferencesRepository(application)
    val updateTestValue = preferences.updateTestValue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val profile = preferences.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())
    val dayPlan = preferences.dayPlan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val googleAccount = preferences.googleAccount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _health = MutableStateFlow(HealthSnapshot())
    val health = _health.asStateFlow()
    private val _healthMessage = MutableStateFlow("Health Connect не подключён")
    val healthMessage = _healthMessage.asStateFlow()
    private val _googleMessage = MutableStateFlow("Google-аккаунт не подключён")
    val googleMessage = _googleMessage.asStateFlow()

    fun incrementUpdateTestValue() = viewModelScope.launch { preferences.incrementUpdateTestValue() }
    fun saveProfile(profile: UserProfile) = viewModelScope.launch { preferences.saveProfile(profile) }
    fun savePlan(items: List<DayPlanItem>) = viewModelScope.launch { preferences.savePlan(items) }
    fun setGoogleAccount(name: String?) = viewModelScope.launch { preferences.saveGoogleAccount(name) }

    fun refreshHealth(repository: HealthConnectRepository) = viewModelScope.launch {
        runCatching {
            if (!repository.hasAllPermissions()) error("Нужно разрешить чтение данных")
            repository.readSnapshot()
        }.onSuccess {
            _health.value = it
            preferences.setHealthConnected(true)
            _healthMessage.value = "Данные Health Connect обновлены"
        }.onFailure { _healthMessage.value = it.message ?: "Не удалось прочитать Health Connect" }
    }

    fun syncGoogle(repository: GoogleSyncRepository, account: String, snapshot: HealthSnapshot) = viewModelScope.launch {
        _googleMessage.value = "Синхронизация…"
        repository.accountName = account
        val json = """{
  "schemaVersion": 1,
  "account": ${jsonString(account)},
  "health": {
    "steps": ${snapshot.steps},
    "distanceKm": ${"%.3f".format(Locale.US, snapshot.distanceKm)},
    "activeCalories": ${"%.1f".format(Locale.US, snapshot.activeCalories)},
    "sleepHours": ${"%.2f".format(Locale.US, snapshot.sleepHours)},
    "averageHeartRate": ${snapshot.averageHeartRate ?: "null"},
    "restingHeartRate": ${snapshot.restingHeartRate ?: "null"},
    "weightKg": ${snapshot.weightKg ?: "null"},
    "bodyFatPercent": ${snapshot.bodyFatPercent ?: "null"}
  }
}"""
        runCatching { repository.sync(json) }
            .onSuccess { _googleMessage.value = it.message }
            .onFailure { _googleMessage.value = it.message ?: "Ошибка синхронизации Google" }
    }

    private fun jsonString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
