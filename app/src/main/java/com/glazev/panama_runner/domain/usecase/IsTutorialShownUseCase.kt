package com.glazev.panama_runner.domain.usecase

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsTutorialShownUseCase @Inject constructor(
    private val repository: com.glazev.panama_runner.domain.repository.SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isTutorialShown()
}
