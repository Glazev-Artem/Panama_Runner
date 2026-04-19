package com.glazev.panama_runner.domain.usecase

import com.glazev.panama_runner.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase для получения текущего рекорда игрока.
 */
class GetHighScoreUseCase @Inject constructor(
    private val repository: ProgressRepository
) {
    operator fun invoke(): Flow<Int> {
        return repository.getHighScore()
    }
}
