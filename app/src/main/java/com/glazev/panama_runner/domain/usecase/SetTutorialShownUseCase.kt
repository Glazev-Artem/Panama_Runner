package com.glazev.panama_runner.domain.usecase

import javax.inject.Inject

class SetTutorialShownUseCase @Inject constructor(
    private val repository: com.glazev.panama_runner.domain.repository.SettingsRepository
) {
    suspend operator fun invoke() = repository.setTutorialShown()
}
