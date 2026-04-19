package com.glazev.panama_runner.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для хранения настроек приложения.
 */
interface SettingsRepository {
    /**
     * Возвращает поток состояния звука (включен/выключен).
     */
    fun isSoundEnabled(): Flow<Boolean>

    /**
     * Переключает состояние звука.
     */
    suspend fun toggleSound()

    /**
     * Возвращает поток состояния вибрации.
     */
    fun isVibrationEnabled(): Flow<Boolean>

    /**
     * Переключает состояние вибрации.
     */
    suspend fun toggleVibration()

    /**
     * Проверяет, было ли показано приветственное окно.
     */
    fun isWelcomeShown(): Flow<Boolean>

    /**
     * Помечает приветственное окно как показанное.
     */
    suspend fun setWelcomeShown()

    /**
     * Проверяет, было ли показано обучение.
     */
    fun isTutorialShown(): Flow<Boolean>

    /**
     * Помечает обучение как показанное.
     */
    suspend fun setTutorialShown()

    /**
     * Возвращает промокод, если он был разблокирован.
     */
    fun getUnlockedPromoCode(): Flow<String?>

    /**
     * Сохраняет разблокированный промокод.
     */
    suspend fun saveUnlockedPromoCode(promoCode: String)
}
