package com.glazev.panama_runner.data.repository

import com.glazev.panama_runner.data.network.ServerApi
import com.glazev.panama_runner.domain.models.GameTelemetry
import com.glazev.panama_runner.domain.repository.PromoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerPromoRepository @Inject constructor(
    private val api: ServerApi
) : PromoRepository {

    override suspend fun verifyAndGetPromo(telemetry: GameTelemetry): Result<String> {
        return try {
            val response = api.verifyVictory(telemetry)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.promoCode != null) {
                    Result.success(body.promoCode)
                } else {
                    Result.failure(Exception(body?.message ?: "Проверка не пройдена"))
                }
            } else {
                Result.failure(Exception("Ошибка сервера: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
