package com.glazev.panama_runner.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления прогрессом игрока (рекордами).
 */
interface ProgressRepository {
    /**
     * Сохраняет новый рекорд игрока.
     */
    suspend fun saveHighScore(score: Int)

    /**
     * Получает текущий рекорд игрока.
     */
    fun getHighScore(): Flow<Int>

    /**
     * Синхронизирует локальный прогресс с облаком (Firebase).
     */
    suspend fun syncWithCloud()
}
