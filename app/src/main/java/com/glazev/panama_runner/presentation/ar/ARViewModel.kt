package com.glazev.panama_runner.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glazev.panama_runner.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ARViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _showWelcomeDialog = MutableStateFlow(false)
    val showWelcomeDialog = _showWelcomeDialog.asStateFlow()

    init {
        checkWelcomeStatus()
    }

    private fun checkWelcomeStatus() {
        viewModelScope.launch {
            val shown = settingsRepository.isWelcomeShown().first()
            if (!shown) {
                _showWelcomeDialog.value = true
            }
        }
    }

    fun onWelcomeDismissed() {
        _showWelcomeDialog.value = false
        viewModelScope.launch {
            settingsRepository.setWelcomeShown()
        }
    }
}
