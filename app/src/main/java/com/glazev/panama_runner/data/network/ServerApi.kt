package com.glazev.panama_runner.data.network

import com.glazev.panama_runner.domain.models.GameTelemetry
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ServerApi {
    
    /**
     * Отправляет телеметрию игры и получает промокод в случае успеха.
     */
    @POST("panama-runner/api/verify-victory")
    suspend fun verifyVictory(
        @Body telemetry: GameTelemetry
    ): Response<PromoResponse>
}

data class PromoResponse(
    val success: Boolean,
    val promoCode: String?,
    val message: String?
)
