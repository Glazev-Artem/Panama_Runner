package com.glazev.panama_runner.domain.models

/**
 * Данные о сессии игры для проверки на сервере (анти-чит).
 */
data class GameTelemetry(
    val userId: String,
    val score: Int,
    val missedCount: Int,
    val durationSeconds: Long,
    val totalDistanceMoved: Float,
    val totalTicks: Int,
    val caughtCount: Int,
    val errorCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val platform: String = "android"
)
