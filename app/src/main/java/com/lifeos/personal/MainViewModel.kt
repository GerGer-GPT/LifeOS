package com.lifeos.personal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.personal.data.local.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferencesRepository(application)

    val updateTestValue = preferences.updateTestValue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
    )

    fun incrementUpdateTestValue() {
        viewModelScope.launch { preferences.incrementUpdateTestValue() }
    }
}
