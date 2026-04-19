package com.glazev.panama_runner.domain.usecase

import com.glazev.panama_runner.domain.repository.SettingsRepository
import javax.inject.Inject

class SetWelcomeShownUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke() = repository.setWelcomeShown()
}
