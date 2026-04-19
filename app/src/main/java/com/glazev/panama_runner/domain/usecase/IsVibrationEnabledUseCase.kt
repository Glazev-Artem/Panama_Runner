package com.glazev.panama_runner.domain.usecase

import com.glazev.panama_runner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsVibrationEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isVibrationEnabled()
}
