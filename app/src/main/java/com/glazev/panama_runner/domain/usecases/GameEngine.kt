package com.glazev.panama_runner.domain.usecases

import com.glazev.panama_runner.domain.models.GameObject
import com.glazev.panama_runner.domain.models.GameObjectType
import com.glazev.panama_runner.domain.models.GameState
import com.glazev.panama_runner.domain.models.GameStatus
import com.glazev.panama_runner.domain.models.PlayerEmotion
import kotlin.math.abs
import kotlin.random.Random

class GameEngine {

    fun nextTick(currentState: GameState): GameState {
        return when (currentState.status) {
            GameStatus.PLAYING -> handlePlaying(currentState)
            GameStatus.BURIAL -> handleBurial(currentState)
            GameStatus.GAME_OVER -> handleGameOver(currentState)
            else -> currentState
        }
    }

    fun rebuildPile(missedCount: Int): List<GameObject.PileItem> {
        val items = mutableListOf<GameObject.PileItem>()
        // На каждую ошибку у нас по 2 панамы
        repeat(missedCount * 2) {
            items.add(generatePileItem(items))
        }
        return items
    }

    private fun handleBurial(state: GameState): GameState {
        val newTimer = state.burialTimer + 0.008f // Замедлили в 2 раза (было 0.016)
        if (newTimer >= 1.0f) {
            return state.copy(status = GameStatus.GAME_OVER, burialTimer = 0f)
        }

        // Анимация "заваливания": панамы летят на игрока и перекрывают экран
        val updatedPile = state.pileItems.mapIndexed { index, it ->
            // Распределяем панамы по всему экрану для эффекта заваливания
            val targetY = 0.1f + (index.toFloat() / state.pileItems.size.toFloat()) * 0.85f
            val targetX = 0.1f + (index % 5) * 0.2f + (Random.nextFloat() - 0.5f) * 0.1f
            
            it.copy(
                scale = (it.scale + 0.08f).coerceAtMost(5.0f), // Замедлили рост (было 0.15)
                y = if (it.y < targetY) it.y + 0.018f else it.y, // Замедлили падение (было 0.035)
                x = it.x + (targetX - it.x) * 0.05f // Плавное смещение по X
            )
        }
        return state.copy(pileItems = updatedPile, burialTimer = newTimer)
    }

    private fun handleGameOver(state: GameState): GameState {
        // Оставляем как есть или можно добавить легкое покачивание
        return state
    }

    private fun updateEmotions(state: GameState): GameState {
        var emotion = state.emotion
        var happyTimer = state.happyTimer
        var breathingTick = state.breathingTick
        var breathingCycle = state.breathingCycle
        var breathingCount = state.breathingCount
        var bubbleText = state.bubbleText
        var bubbleTimer = state.bubbleTimer

        val updatedFloating = state.floatingTexts.mapNotNull {
            val nextLife = it.life - 0.02f
            if (nextLife <= 0) null else it.copy(life = nextLife, y = it.y - 0.003f)
        }

        val updatedParticles = state.particles.mapNotNull {
            val nextLife = it.life - 0.03f
            if (nextLife <= 0) null 
            else it.copy(
                x = it.x + it.vx, 
                y = it.y + it.vy, 
                vy = it.vy + 0.0005f, // Гравитация
                life = nextLife
            )
        }

        if (bubbleTimer > 0) {
            bubbleTimer--
            if (bubbleTimer <= 0) bubbleText = null
        }

        when (emotion) {
            PlayerEmotion.HAPPY -> {
                happyTimer--
                if (happyTimer <= 0) emotion = PlayerEmotion.NORMAL
            }
            PlayerEmotion.BREATHING -> {
                breathingTick++
                if (breathingTick >= 14) { // Ускорили фазу (было 20)
                    breathingTick = 0
                    breathingCycle = if (breathingCycle == 0) 1 else 0
                    if (breathingCycle == 0) {
                        breathingCount--
                        if (breathingCount <= 0) emotion = PlayerEmotion.NORMAL
                    }
                }
            }
            else -> {}
        }

        return state.copy(
            emotion = emotion,
            happyTimer = happyTimer,
            breathingTick = breathingTick,
            breathingCycle = breathingCycle,
            breathingCount = breathingCount,
            floatingTexts = updatedFloating,
            particles = updatedParticles,
            bubbleText = bubbleText,
            bubbleTimer = bubbleTimer
        )
    }

    private val goodPhrases = listOf("Круто!", "Йоу!", "Чётко!", "В яблочко!", "Мастер!")
    private val badPhrases = listOf("Ой!", "Мимо!", "Эхх..", "Ну как так?", "Где панама?")
    private val catchBadPhrases = listOf("Фуу!", "Брак!", "Выкинь это!", "Не то!", "Ужас!")

    private fun handlePlaying(state: GameState): GameState {
        val tempState = updateEmotions(state)
        var newScore = tempState.score
        var newMissedCount = tempState.missedCount
        var newCombo = tempState.combo
        var newPileCount = tempState.pileCount
        var newPileItems = tempState.pileItems.toMutableList()
        val newFloatingTexts = tempState.floatingTexts.toMutableList()
        val newParticles = tempState.particles.toMutableList()
        var newBubbleText = tempState.bubbleText
        var newBubbleTimer = tempState.bubbleTimer

        var currentEmotion = tempState.emotion
        var currentHappyTimer = tempState.happyTimer
        var currentBreathingCount = tempState.breathingCount
        var currentNextThreshold = tempState.nextBreathingThreshold
        var currentSeriesCount = tempState.goodSeriesCount
        
        val step = 200 // Градация сложности каждые 200 панам (Боевой режим)
        val diff = (newScore / step).coerceIn(0, 4)

        val maxObjects = 3 + (diff * 1.5f).toInt() // Быстрее растет кол-во (3, 4, 6, 7, 9)
        val baseSpeed = 0.0055f + (diff * 0.0022f) // Резко увеличиваем скорость (было 0.0012)
        val spawnChance = 0.955f - (diff * 0.012f) // Заметно чаще спавним (было 0.965)
        val badChance = 0.12f + (diff * 0.12f) // Больше брака

        val updatedFalling = mutableListOf<GameObject.FallingObject>()
        val boxCenterX = state.playerX + (if (state.playerDirection == com.glazev.panama_runner.domain.models.PlayerDirection.RIGHT) 0.069f else -0.062f)
        val boxTopY = 0.625f 
        val boxBottomY = 0.85f // Конец коробки по вертикали
        val boxWidth = 0.187f 
        val itemWidth = 0.12f

        state.objects.forEach { obj ->
            val nextY = obj.y + (if (obj.isCaught) obj.speed * 4.5f else obj.speed)
            val nextX = if (obj.isCaught) obj.x + (boxCenterX - obj.x) * 0.25f else obj.x + obj.vx
            
            if (obj.isCaught) {
                if (nextY > boxTopY + 0.12f) {
                    if (isGood(obj.type)) {
                        newScore++; newCombo++
                        
                        // Фраза при хорошем улове
                        if (Random.nextFloat() > 0.7f && newBubbleTimer <= 0) {
                            newBubbleText = goodPhrases.random()
                            newBubbleTimer = 60
                        }

                        // Эффект искр при поимке
                        repeat(8) {
                            newParticles.add(GameObject.Particle(
                                id = System.nanoTime() + it,
                                x = boxCenterX,
                                y = boxTopY + 0.05f,
                                vx = (Random.nextFloat() - 0.5f) * 0.02f,
                                vy = -Random.nextFloat() * 0.02f,
                                color = 0xFFFFFF00, // Yellow sparks
                                size = 4f + Random.nextFloat() * 4f
                            ))
                        }

                        // Текст комбо или просто +1
                        if (newCombo % 5 == 0) {
                            newFloatingTexts.add(com.glazev.panama_runner.domain.models.FloatingText(
                                id = System.nanoTime(), text = "COMBO $newCombo!", x = boxCenterX, y = boxTopY - 0.05f, 
                                type = com.glazev.panama_runner.domain.models.FloatingTextType.COMBO, color = 0xFFFFD700
                            ))
                        } else {
                            newFloatingTexts.add(com.glazev.panama_runner.domain.models.FloatingText(
                                id = System.nanoTime(), text = "+1", x = boxCenterX, y = boxTopY - 0.02f, 
                                type = com.glazev.panama_runner.domain.models.FloatingTextType.NORMAL, color = 0xFFFFFFFF
                            ))
                        }

                        // Логика эмоций
                        currentSeriesCount++
                        currentNextThreshold--

                        if (currentSeriesCount >= 4) {
                            currentEmotion = PlayerEmotion.HAPPY
                            currentHappyTimer = 45 // ~0.75 сек
                            currentSeriesCount = 0
                        }

                        if (currentNextThreshold <= 0 && currentEmotion == PlayerEmotion.NORMAL) {
                            currentEmotion = PlayerEmotion.BREATHING
                            currentBreathingCount = 3 
                            currentNextThreshold = Random.nextInt(10, 16)
                        }

                        if (newCombo % 15 == 0) {
                            newMissedCount = (newMissedCount - 1).coerceAtLeast(0)
                            newFloatingTexts.add(com.glazev.panama_runner.domain.models.FloatingText(
                                id = System.nanoTime(), text = "-1 БРАК", x = 0.2f, y = 0.15f, 
                                type = com.glazev.panama_runner.domain.models.FloatingTextType.RECOVER, color = 0xFF00FF00
                            ))
                            repeat(2) {
                                if (newPileItems.isNotEmpty()) {
                                    newPileItems.removeAt(newPileItems.size - 1)
                                }
                            }
                            newPileCount = newPileItems.size
                        }
                    } else {
                        newMissedCount++; newCombo = 0
                        currentSeriesCount = 0 // Сброс серии при ошибке
                        
                        // Фраза при поимке брака
                        newBubbleText = catchBadPhrases.random()
                        newBubbleTimer = 60

                        newFloatingTexts.add(com.glazev.panama_runner.domain.models.FloatingText(
                            id = System.nanoTime(), text = "БРАК!", x = boxCenterX, y = boxTopY - 0.05f, 
                            type = com.glazev.panama_runner.domain.models.FloatingTextType.ERROR, color = 0xFFFF0000
                        ))
                        repeat(2) { newPileItems.add(generatePileItem(newPileItems)) }
                        newPileCount = newPileItems.size
                    }
                } else {
                    updatedFalling.add(obj.copy(x = nextX, y = nextY, rotation = obj.rotation + obj.vRotation))
                }
                return@forEach
            }

            val distToCenter = abs(obj.x - boxCenterX)
            val halfBox = boxWidth / 2
            val halfItem = itemWidth / 2

            // 1. ПРОВЕРКА НА ВХОД СВЕРХУ (ЛОВЛЯ)
            if (obj.y < boxTopY && nextY >= boxTopY) {
                val overlap = (halfBox + halfItem) - distToCenter
                if (overlap / itemWidth >= 0.45f) {
                    updatedFalling.add(obj.copy(y = boxTopY + 0.01f, vx = 0f, isCaught = true))
                    return@forEach
                }
            }

            // 2. ПРОВЕРКА НА УДАР О БОКОВЫЕ СТЕНКИ (ФИЗИКА РЕБЕР)
            // Если панама находится на уровне коробки по высоте
            if (nextY > boxTopY && nextY < boxBottomY) {
                if (distToCenter < (halfBox + halfItem)) {
                    // Произошло столкновение с боком
                    val side = if (obj.x > boxCenterX) 1f else -1f
                    updatedFalling.add(obj.copy(
                        x = obj.x + side * 0.03f, 
                        y = obj.y - 0.01f, 
                        vx = side * 0.025f, 
                        speed = 0.003f, 
                        vRotation = side * 25f
                    ))
                    return@forEach
                }
            }

            // 3. ОБЫЧНОЕ ПАДЕНИЕ ИЛИ ПРОМАХ
            if (nextY > 1.1f) {
                if (isGood(obj.type)) { 
                    newMissedCount++
                    newCombo = 0 
                    currentSeriesCount = 0 // Сброс серии при промахе
                    
                    // Фраза при промахе
                    newBubbleText = badPhrases.random()
                    newBubbleTimer = 60

                    newFloatingTexts.add(com.glazev.panama_runner.domain.models.FloatingText(
                        id = System.nanoTime(), text = "ПРОМАХ!", x = obj.x, y = 0.9f, 
                        type = com.glazev.panama_runner.domain.models.FloatingTextType.ERROR, color = 0xFFFF0000
                    ))
                    repeat(2) { newPileItems.add(generatePileItem(newPileItems)) }
                    newPileCount = newPileItems.size
                }
            } else {
                updatedFalling.add(obj.copy(x = nextX, y = nextY, rotation = obj.rotation + obj.vRotation))
            }
        }

        // Спавн
        if (updatedFalling.count { !it.isCaught } < maxObjects && Random.nextFloat() > spawnChance) {
            val isGood = Random.nextFloat() > badChance
            val type = if (isGood) GameObjectType.values().filter { it.name.startsWith("PANAMA") }.random()
                       else GameObjectType.values().filter { it.name.startsWith("BAD_PANAMA") }.random()
            val newX = 0.12f + Random.nextFloat() * 0.76f
            val minXDist = if (diff >= 3) 0.04f else 0.18f
            if (updatedFalling.none { !it.isCaught && abs(it.x - newX) < minXDist && it.y < 0.2f }) {
                updatedFalling.add(GameObject.FallingObject(
                    id = System.nanoTime(), x = newX, y = -0.1f, 
                    speed = baseSpeed + Random.nextFloat() * 0.0015f,
                    type = type, vRotation = Random.nextFloat() * 4f - 2f
                ))
            }
        }

        // --- КОНВЕЙЕР (ЛОГИКА 8.2) ---
        val updatedConveyor = mutableListOf<GameObject.ConveyorObject>()
        val boxWidthOnConveyor = 0.145f // Уменьшили расстояние (почти впритык)
        
        // Сортируем по X, чтобы обрабатывать от правой стороны к левой
        val sortedBoxes = state.conveyorObjects.sortedByDescending { it.x }
        
        sortedBoxes.forEach { obj ->
            var nx = obj.x + 0.0035f // Скорость конвейера
            var shouldStop = false
            
            // 1. Остановка QR-кода в конце ленты
            if (obj.type == GameObjectType.KOROB_QR && nx >= 0.76f && obj.x < 0.76f && !obj.isWaiting) {
                nx = 0.76f
                shouldStop = true
            }
            
            // 2. Проверка на впереди идущую коробку (очередь)
            if (updatedConveyor.isNotEmpty()) {
                val frontBox = updatedConveyor.last()
                val dist = frontBox.x - nx
                if (dist < boxWidthOnConveyor) {
                    nx = frontBox.x - boxWidthOnConveyor
                    if (frontBox.isWaiting) shouldStop = true
                }
            }

            if (obj.isWaiting || shouldStop) {
                val nt = if (obj.isWaiting) obj.waitTimer - 0.016f else 4.0f
                // Если мы в режиме ожидания, используем nx (который уже зафиксирован на 0.76 или прижат к передней коробке)
                // Но важно НЕ прибавлять скорость в следующем кадре. 
                // В коде выше nx вычисляется от obj.x. Если мы запишем nx обратно в объект, 
                // то в следующем кадре obj.x будет 0.76, и nx станет 0.7635.
                // Поэтому при ожидании мы должны сохранять СТАТИЧНУЮ координату.
                
                val stopX = if (shouldStop) nx else obj.x
                
                if (nt <= 0) {
                    // Время ожидания вышло - продолжаем движение
                    updatedConveyor.add(obj.copy(x = obj.x + 0.0035f, isWaiting = false, waitTimer = 0f))
                } else {
                    updatedConveyor.add(obj.copy(x = stopX, isWaiting = true, waitTimer = nt))
                }
            } else if (nx < 1.3f) {
                updatedConveyor.add(obj.copy(x = nx))
            }
        }

        // --- РАНДОМНЫЙ СПАВН ГРУППАМИ (8.2) ---
        var newBoxesInTrain = state.boxesInTrainLeft
        var newBoxesSinceLastQR = state.boxesSinceLastQR
        val leftmostBoxX = updatedConveyor.minByOrNull { it.x }?.x ?: 2f
        
        // Уменьшили дистанцию между коробками в группе при спавне
        val spawnThreshold = if (newBoxesInTrain > 0) -0.05f else 0.4f
        
        if (leftmostBoxX > spawnThreshold) {
            if (newBoxesInTrain > 0) {
                // Продолжаем текущий паровозик
                val canSpawnQR = newBoxesSinceLastQR >= 20
                // Шанс растет от 20 до 25 коробок, на 25-й коробке шанс 100%
                val bType = if (canSpawnQR && (Random.nextInt(26 - newBoxesSinceLastQR.coerceIn(20, 25)) == 0)) {
                    newBoxesSinceLastQR = 0
                    GameObjectType.KOROB_QR 
                } else {
                    newBoxesSinceLastQR++
                    GameObjectType.values().filter { it.name.startsWith("KOROB_") && it != GameObjectType.KOROB_QR }.random()
                }
                updatedConveyor.add(GameObject.ConveyorObject(id = System.nanoTime(), x = -0.15f, y = 0.95f, speed = 0.0035f, type = bType))
                newBoxesInTrain--
            } else if (Random.nextFloat() > 0.985f) { 
                // Начинаем новую группу (от 1 до 4 коробок)
                newBoxesInTrain = Random.nextInt(1, 5)
                // Сразу спавним первую коробку группы
                val canSpawnQR = newBoxesSinceLastQR >= 20
                val bType = if (canSpawnQR && (Random.nextInt(26 - newBoxesSinceLastQR.coerceIn(20, 25)) == 0)) {
                    newBoxesSinceLastQR = 0
                    GameObjectType.KOROB_QR 
                } else {
                    newBoxesSinceLastQR++
                    GameObjectType.values().filter { it.name.startsWith("KOROB_") && it != GameObjectType.KOROB_QR }.random()
                }
                updatedConveyor.add(GameObject.ConveyorObject(id = System.nanoTime(), x = -0.15f, y = 0.95f, speed = 0.0035f, type = bType))
                newBoxesInTrain--
            }
        }

        val nextStatus = if (newMissedCount >= GameState.MAX_MISSED) GameStatus.BURIAL
                         else if (newScore >= GameState.MAX_SCORE) GameStatus.WON 
                         else GameStatus.PLAYING

        return tempState.copy(
            score = newScore, missedCount = newMissedCount, combo = newCombo,
            pileCount = newPileCount, pileItems = newPileItems,
            objects = updatedFalling, conveyorObjects = updatedConveyor,
            boxesInTrainLeft = newBoxesInTrain,
            boxesSinceLastQR = newBoxesSinceLastQR,
            status = nextStatus,
            emotion = currentEmotion,
            happyTimer = currentHappyTimer,
            breathingCount = currentBreathingCount,
            nextBreathingThreshold = currentNextThreshold,
            goodSeriesCount = currentSeriesCount,
            floatingTexts = newFloatingTexts,
            particles = newParticles,
            bubbleText = newBubbleText,
            bubbleTimer = newBubbleTimer
        )
    }

    private fun generatePileItem(currentItems: List<GameObject.PileItem>): GameObject.PileItem {
        val n = currentItems.size
        var layer = 0
        var itemsInBottomLayer = 8 // Увеличено основание для более высокой пирамиды
        var tempSum = 0
        var currentLayerCapacity = itemsInBottomLayer
        
        // Определяем слой и индекс в слое для пирамидальной структуры
        while (n >= tempSum + currentLayerCapacity && currentLayerCapacity > 1) {
            tempSum += currentLayerCapacity
            currentLayerCapacity--
            layer++
        }
        
        val indexInLayer = n - tempSum
        val actualItemsInLayer = currentLayerCapacity
        
        // Базовые параметры пирамиды (центрировано, ширина 40%)
        val centerY = 0.81f // Поднято к верхнему краю конвейера с небольшим нахлестом
        val verticalStep = 0.09f // Увеличен шаг, чтобы куча была в 2 раза выше
        val y = centerY - (layer * verticalStep)
        
        val baseWidth = 0.42f // Чуть шире основание
        val layerWidth = baseWidth * (actualItemsInLayer.toFloat() / itemsInBottomLayer.toFloat())
        val startX = 0.5f - (layerWidth / 2f)
        val stepX = if (actualItemsInLayer > 1) layerWidth / (actualItemsInLayer - 1) else 0f
        
        // Добавляем хаотичность (jitter)
        val jitterX = (Random.nextFloat() - 0.5f) * 0.07f // Разброс по X
        val jitterY = (Random.nextFloat() - 0.5f) * 0.04f // Увеличен разброс по Y для хаоса
        val rotation = (Random.nextFloat() * 360f) // Полный хаос в поворотах
        
        val x = if (actualItemsInLayer > 1) startX + (indexInLayer * stepX) + jitterX else 0.5f + jitterX
        
        val type = GameObjectType.values().filter { it.name.startsWith("PANAMA") }.random()
        return GameObject.PileItem(
            x = x,
            y = y + jitterY,
            rotation = rotation,
            type = type
        )
    }

    private fun isGood(type: GameObjectType) = type.name.startsWith("PANAMA")
}
