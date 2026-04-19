package com.glazev.panama_runner.presentation.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glazev.panama_runner.domain.models.GameState
import com.glazev.panama_runner.domain.models.GameStatus
import com.glazev.panama_runner.domain.models.PlayerDirection
import com.glazev.panama_runner.domain.usecase.*
import com.glazev.panama_runner.domain.usecases.GameEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GameEffect {
    object PlaySoundCatch : GameEffect()
    object PlaySoundError : GameEffect()
    object PlaySoundGameOver : GameEffect()
    object VibrateCatch : GameEffect()
    object VibrateError : GameEffect()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getHighScoreUseCase: GetHighScoreUseCase,
    private val saveHighScoreUseCase: SaveHighScoreUseCase,
    private val isSoundEnabledUseCase: IsSoundEnabledUseCase,
    private val toggleSoundUseCase: ToggleSoundUseCase,
    private val isVibrationEnabledUseCase: IsVibrationEnabledUseCase,
    private val toggleVibrationUseCase: ToggleVibrationUseCase,
    private val isWelcomeShownUseCase: IsWelcomeShownUseCase,
    private val setWelcomeShownUseCase: SetWelcomeShownUseCase,
    private val isTutorialShownUseCase: com.glazev.panama_runner.domain.usecase.IsTutorialShownUseCase,
    private val setTutorialShownUseCase: com.glazev.panama_runner.domain.usecase.SetTutorialShownUseCase,
    private val saveUnlockedPromoCodeUseCase: SaveUnlockedPromoCodeUseCase,
    private val getUnlockedPromoCodeUseCase: GetUnlockedPromoCodeUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _showWelcomeDialog = MutableStateFlow(savedStateHandle["show_welcome"] ?: false)
    val showWelcomeDialog = _showWelcomeDialog.asStateFlow()

    private val _effect = MutableSharedFlow<GameEffect>()
    val effect: SharedFlow<GameEffect> = _effect.asSharedFlow()

    private val engine = GameEngine()
    private var gameJob: Job? = null

    // Текущий рекорд
    private var highScore: Int = 0

    // Состояние звука
    val isSoundEnabled: StateFlow<Boolean> = isSoundEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Состояние вибрации
    val isVibrationEnabled: StateFlow<Boolean> = isVibrationEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Разблокированный промокод для отображения в настройках
    val unlockedPromoCode: StateFlow<String?> = getUnlockedPromoCodeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Проверка приветственного окна
        viewModelScope.launch {
            isWelcomeShownUseCase().collect { shown ->
                // Если в SavedStateHandle уже есть true, значит мы его показываем.
                // Если в репозитории false, и мы еще не помечали его как скрытый в этой сессии.
                if (!shown && savedStateHandle.get<Boolean>("welcome_dismissed") != true) {
                    _showWelcomeDialog.value = true
                    savedStateHandle["show_welcome"] = true
                }
            }
        }
        
        // Подписываемся на обновление рекорда
        viewModelScope.launch {
            getHighScoreUseCase().collect { score ->
                highScore = score
                _state.update { it.copy(bestScore = score) }
            }
        }
    }

    fun onWelcomeDismissed() {
        _showWelcomeDialog.value = false
        savedStateHandle["show_welcome"] = false
        savedStateHandle["welcome_dismissed"] = true
        viewModelScope.launch {
            setWelcomeShownUseCase()
            // После приветствия проверяем, нужно ли обучение (берем только первое значение)
            val shown = isTutorialShownUseCase().first()
            if (!shown) {
                startTutorial()
            }
        }
    }

    fun startTutorial() {
        _state.update { it.copy(status = GameStatus.TUTORIAL, tutorialStep = 1) }
    }

    fun nextTutorialStep() {
        val currentStep = _state.value.tutorialStep
        if (currentStep < 4) {
            _state.update { it.copy(tutorialStep = currentStep + 1) }
        } else {
            // Завершение обучения
            viewModelScope.launch {
                setTutorialShownUseCase()
            }
            _state.update { it.copy(status = GameStatus.IDLE, tutorialStep = 0) }
        }
    }

    fun startGame(isOnline: Boolean = true) {
        if (_state.value.status != GameStatus.IDLE) return
        // Сбрасываем состояние перед началом, сохраняя рекорд и устанавливая статус сети
        _state.value = GameState(bestScore = highScore, isOnlineSession = isOnline)
        startCountdown()
    }

    private fun startCountdown() {
        _state.update { it.copy(status = GameStatus.COUNTDOWN, countdownSeconds = 3) }
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _state.update { it.copy(countdownSeconds = i) }
                delay(1000)
            }
            _state.update { it.copy(status = GameStatus.PLAYING) }
            runGameLoop()
        }
    }

    private fun runGameLoop() {
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            while (_state.value.status == GameStatus.PLAYING || 
                   _state.value.status == GameStatus.PAUSED || 
                   _state.value.status == GameStatus.BURIAL) {
                if (_state.value.status == GameStatus.PAUSED) {
                    delay(100)
                    continue
                }

                val prevState = _state.value
                var nextState = engine.nextTick(prevState)
                
                // Обновляем рекорд локально и в облаке
                if (nextState.score > highScore) {
                    highScore = nextState.score
                    viewModelScope.launch {
                        saveHighScoreUseCase(highScore)
                    }
                }
                nextState = nextState.copy(bestScore = highScore)

                // Проверка событий для звуков и вибрации
                if (nextState.score > prevState.score) {
                    _effect.emit(GameEffect.PlaySoundCatch)
                    _effect.emit(GameEffect.VibrateCatch)
                } else if (nextState.missedCount > prevState.missedCount) {
                    _effect.emit(GameEffect.PlaySoundError)
                    _effect.emit(GameEffect.VibrateError)
                }
                
                if (nextState.status == GameStatus.GAME_OVER && prevState.status != GameStatus.GAME_OVER) {
                    _effect.emit(GameEffect.PlaySoundGameOver)
                }

                _state.value = nextState
                delay(16)
            }
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            toggleSoundUseCase()
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            toggleVibrationUseCase()
        }
    }

    fun togglePause() {
        _state.update {
            when (it.status) {
                GameStatus.PLAYING -> it.copy(status = GameStatus.PAUSED)
                GameStatus.PAUSED -> it.copy(status = GameStatus.PLAYING)
                else -> it
            }
        }
    }

    fun movePlayer(newX: Float) {
        if (_state.value.status != GameStatus.PLAYING) return
        
        val clampedX = newX.coerceIn(0.05f, 0.95f)
        val currentX = _state.value.playerX
        val direction = if (clampedX > currentX + 0.001f) PlayerDirection.RIGHT 
                        else if (clampedX < currentX - 0.001f) PlayerDirection.LEFT 
                        else _state.value.playerDirection
        
        _state.update { it.copy(playerX = clampedX, playerDirection = direction) }
    }

    fun restartGame() {
        gameJob?.cancel()
        _state.value = GameState(status = GameStatus.IDLE, bestScore = highScore)
    }

    fun onWin(promoCode: String) {
        viewModelScope.launch {
            saveUnlockedPromoCodeUseCase(promoCode)
        }
    }

    fun onAdRewarded() {
        gameJob?.cancel()
        _state.update { 
            // Реклама снимает 6 очков брака (16 -> 10)
            val newMissedCount = (it.missedCount - 6).coerceAtLeast(0)
            // Пересобираем кучу заново, чтобы она вернулась на место и уменьшилась
            val newPileItems = engine.rebuildPile(newMissedCount)
            it.copy(
                status = GameStatus.COUNTDOWN, 
                score = it.score,
                missedCount = newMissedCount,
                pileItems = newPileItems,
                pileCount = newPileItems.size,
                objects = emptyList(),
                bestScore = highScore 
            )
        }
        startCountdown()
    }

    override fun onCleared() {
        gameJob?.cancel()
        super.onCleared()
    }
}
