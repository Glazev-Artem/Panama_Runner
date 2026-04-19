package com.glazev.panama_runner.domain.usecase

import com.glazev.panama_runner.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveUnlockedPromoCodeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(promoCode: String) {
        repository.saveUnlockedPromoCode(promoCode)
    }
}
