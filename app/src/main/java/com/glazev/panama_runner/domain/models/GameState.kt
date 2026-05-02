package com.glazev.panama_runner.domain.models

/** Статусы игры */
enum class GameStatus {
    IDLE,       // Стартовый экран (Splash/Menu)
    TUTORIAL,   // Режим обучения
    COUNTDOWN,  // Обратный отсчет перед стартом
    PLAYING,    // В процессе игры
    PAUSED,     // Пауза
    BURIAL,     // Анимация заваливания панамами
    GAME_OVER,  // Проигрыш (экран с кнопками)
    WON         // Победа (1000 очков)
}

/** Эмоции персонажа */
enum class PlayerEmotion {
    NORMAL,    // Радостный с открытыми глазами (стартовая)
    BREATHING, // Отдышка (вдох/выдох)
    HAPPY      // Довольный с закрытыми глазами (кайфанул)
}

/**
 * Состояние игры в конкретный момент времени.
 */
data class GameState(
    val score: Int = 0,
    val missedCount: Int = 0,
    val combo: Int = 0, 
    val playerX: Float = 0.5f,
    val playerDirection: PlayerDirection = PlayerDirection.LEFT,
    val objects: List<GameObject.FallingObject> = emptyList(),
    val conveyorObjects: List<GameObject.ConveyorObject> = emptyList(),
    val conveyorBoxCount: Int = 0,
    val pileCount: Int = 0, // Количество панам в куче на фоне
    val pileItems: List<GameObject.PileItem> = emptyList(), // Объекты панам в куче
    val burialTimer: Float = 0f, // Таймер анимации засыпания
    val status: GameStatus = GameStatus.IDLE,
    val countdownSeconds: Int = 3,
    val bestScore: Int = 0,
    
    // Туториал
    val tutorialStep: Int = 0, // 0 - не начат, 1 - движение, 2 - ловля панамы, 3 - пропуск брака, 4 - цель
    
    // Эмоции и дыхание
    val emotion: PlayerEmotion = PlayerEmotion.NORMAL,
    val happyTimer: Int = 0,          // Тики для состояния HAPPY
    val breathingCycle: Int = 0,      // Текущий цикл отдыхашки (0-вдох, 1-выдох)
    val breathingTick: Int = 0,       // Время внутри одной фазы вдоха/выдоха
    val breathingCount: Int = 0,      // Сколько циклов осталось продышаться
    val nextBreathingThreshold: Int = 15, // Через сколько панам наступит отдышка
    val goodSeriesCount: Int = 0,     // Счетчик серии из 4 хороших панам
    
    // Очереди конвейера
    val boxesInTrainLeft: Int = 0, // Сколько коробок осталось заспавнить в текущем "паровозе"
    val boxesSinceLastQR: Int = 0, // Счетчик обычных коробок после последнего QR
    
    // Сессия (онлайн/оффлайн)
    val isOnlineSession: Boolean = true,
    val floatingTexts: List<FloatingText> = emptyList(),
    val particles: List<GameObject.Particle> = emptyList(),
    val bubbleText: String? = null,
    val bubbleTimer: Int = 0
) {
    companion object {
        const val MAX_SCORE = 1000 // БОЕВОЙ РЕЖИМ
        const val MAX_MISSED = 16
    }
}

enum class PlayerDirection {
    LEFT, RIGHT
}

data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val life: Float = 1f,
    val type: FloatingTextType = FloatingTextType.NORMAL,
    val color: Long = 0xFFFFFFFF // White by default
)

enum class FloatingTextType {
    NORMAL, COMBO, RECOVER, ERROR
}
