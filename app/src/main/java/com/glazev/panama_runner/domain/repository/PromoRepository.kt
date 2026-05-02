package com.glazev.panama_runner.domain.repository

import com.glazev.panama_runner.domain.models.GameTelemetry

interface PromoRepository {
    /**
     * Отправляет данные игры на сервер и возвращает промокод, если проверка пройдена.
     */
    suspend fun verifyAndGetPromo(telemetry: GameTelemetry): Result<String>
}
