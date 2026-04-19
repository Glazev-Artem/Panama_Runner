package com.glazev.panama_runner.domain.usecase

import com.glazev.panama_runner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUnlockedPromoCodeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<String?> {
        return repository.getUnlockedPromoCode()
    }
}
