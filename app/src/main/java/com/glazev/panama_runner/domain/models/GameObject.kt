package com.glazev.panama_runner.domain.models

enum class GameObjectType {
    PANAMA_1, PANAMA_2, PANAMA_3, PANAMA_4, PANAMA_5, PANAMA_6, PANAMA_7,
    BAD_PANAMA_1, BAD_PANAMA_2, BAD_PANAMA_3, BAD_PANAMA_4, BAD_PANAMA_5, BAD_PANAMA_6,
    KOROB_1, KOROB_2, KOROB_3, KOROB_4, KOROB_5, KOROB_QR
}

sealed class GameObject {
    abstract val id: Long
    abstract val x: Float
    abstract val y: Float
    abstract val speed: Float
    abstract val type: GameObjectType

    data class FallingObject(
        override val id: Long,
        override val x: Float,
        override val y: Float,
        override val speed: Float,
        override val type: GameObjectType,
        val vx: Float = 0f,
        val rotation: Float = 0f,
        val vRotation: Float = 0f,
        val isCaught: Boolean = false
    ) : GameObject()

    data class ConveyorObject(
        override val id: Long,
        override val x: Float,
        override val y: Float,
        override val speed: Float,
        override val type: GameObjectType,
        val isWaiting: Boolean = false,
        val waitTimer: Float = 0f
    ) : GameObject()

    /** Предмет в куче на заднем плане */
    data class PileItem(
        val x: Float,
        val y: Float,
        val rotation: Float,
        val type: GameObjectType,
        val scale: Float = 1f
    )

    /** Частица для эффекта взрыва/искр */
    data class Particle(
        val id: Long,
        val x: Float,
        val y: Float,
        val vx: Float,
        val vy: Float,
        val color: Long,
        val life: Float = 1.0f,
        val size: Float = 4f
    )
}
