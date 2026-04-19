package com.glazev.panama_runner.domain.usecase

import com.glazev.panama_runner.domain.repository.ProgressRepository
import javax.inject.Inject

/**
 * UseCase для сохранения рекорда игрока.
 */
class SaveHighScoreUseCase @Inject constructor(
    private val repository: ProgressRepository
) {
    suspend operator fun invoke(score: Int) {
        repository.saveHighScore(score)
    }
}
