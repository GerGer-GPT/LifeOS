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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferencesRepository(application)
    val updateTestValue = preferences.updateTestValue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val profile = preferences.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())
    val dayPlan = preferences.dayPlan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun incrementUpdateTestValue() = viewModelScope.launch { preferences.incrementUpdateTestValue() }
    fun saveProfile(profile: UserProfile) = viewModelScope.launch { preferences.saveProfile(profile) }
    fun savePlan(items: List<DayPlanItem>) = viewModelScope.launch { preferences.savePlan(items) }
}
