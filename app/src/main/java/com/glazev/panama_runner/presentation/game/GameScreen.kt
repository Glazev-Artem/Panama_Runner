package com.glazev.panama_runner.presentation.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.glazev.panama_runner.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.glazev.panama_runner.domain.models.GameStatus
import com.glazev.panama_runner.domain.models.GameState
import com.glazev.panama_runner.presentation.auth.AuthViewModel
import androidx.compose.ui.text.font.FontWeight
import com.glazev.panama_runner.presentation.components.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToAR: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isUserSignedIn by authViewModel.isUserSignedIn.collectAsState()
    val context = LocalContext.current
    val adsManager = remember { AdsManager(context).apply { loadRewardedVideo() } }
    
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("360013361206-5r07qh78484c5n5sfbulv8l592b5h230.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account?.idToken?.let { authViewModel.onSignInResult(it) }
        } catch (e: Exception) { }
    }

    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val unlockedPromoCode by viewModel.unlockedPromoCode.collectAsState()
    val showWelcomeDialog by viewModel.showWelcomeDialog.collectAsState()

    // Интеграция звука и вибрации
    val soundManager = remember { SoundManager(context) }
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (isSoundEnabled) {
                when (effect) {
                    GameEffect.PlaySoundCatch -> soundManager.playCatch()
                    GameEffect.PlaySoundError -> soundManager.playError()
                    GameEffect.PlaySoundGameOver -> soundManager.playGameOver()
                    else -> {}
                }
            }
            
            // Вибрация (работает независимо от звука, если есть разрешение)
            when (effect) {
                GameEffect.VibrateCatch -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }
                GameEffect.VibrateError -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                }
                else -> {}
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Анимированный фон для всей игры
        VideoBackground(
            videoResId = R.raw.animate_bg,
            isSoundEnabled = isSoundEnabled
        )

        when (state.status) {
            GameStatus.IDLE -> MenuScreen(
                onStart = { isOnline -> viewModel.startGame(isOnline) },
                isUserSignedIn = isUserSignedIn,
                userEmail = authViewModel.userEmail,
                isSoundEnabled = isSoundEnabled,
                isVibrationEnabled = isVibrationEnabled,
                unlockedPromoCode = unlockedPromoCode,
                onSignIn = { launcher.launch(googleSignInClient.signInIntent) },
                onSignOut = { authViewModel.signOut() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibration = { viewModel.toggleVibration() },
                onArTryOn = onNavigateToAR,
                onShowTutorial = { viewModel.startTutorial() },
                isNetworkAvailable = adsManager.isNetworkAvailable()
            )
            GameStatus.TUTORIAL -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    GameCanvas(state = state, viewModel = viewModel)
                    TutorialOverlay(
                        step = state.tutorialStep,
                        onNext = { viewModel.nextTutorialStep() }
                    )
                }
            }
            else -> {
                GamePlayContent(
                    state = state,
                    viewModel = viewModel,
                    adsManager = adsManager,
                    isSoundEnabled = isSoundEnabled,
                    isVibrationEnabled = isVibrationEnabled,
                    unlockedPromoCode = unlockedPromoCode,
                    onNavigateToAR = onNavigateToAR,
                    isUserSignedIn = isUserSignedIn,
                    userEmail = authViewModel.userEmail,
                    onSignIn = { launcher.launch(googleSignInClient.signInIntent) },
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }

        if (showWelcomeDialog && state.status == GameStatus.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                WelcomeDialogContent(
                    onDismiss = { viewModel.onWelcomeDismissed() },
                    onTryOn = {
                        // Не закрываем диалог навсегда при нажатии "Примерить",
                        // чтобы он остался, если пользователь вернется из AR или возникнет ошибка.
                        // Только кнопка "Понятно" (onDismiss) помечает его как просмотренный.
                        onNavigateToAR()
                    }
                )
            }
        }
    }
}

@Composable
fun GamePlayContent(
    state: GameState,
    viewModel: GameViewModel,
    adsManager: AdsManager,
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    unlockedPromoCode: String? = null,
    onNavigateToAR: () -> Unit,
    isUserSignedIn: Boolean,
    userEmail: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Основной игровой холст
        GameCanvas(state = state, viewModel = viewModel)

        // HUD (интерфейс)
        GameHUD(
            state = state,
            onPause = {
                viewModel.togglePause()
                showSettings = true
            }
        )

        // Оверлеи в зависимости от состояния
        when (state.status) {
            GameStatus.COUNTDOWN -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.countdownSeconds.toString(),
                        fontSize = 120.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            GameStatus.GAME_OVER -> {
                GameOverOverlay(
                    state = state,
                    viewModel = viewModel,
                    adsManager = adsManager
                )
            }
            GameStatus.WON -> {
                WinOverlay(
                    state = state,
                    viewModel = viewModel
                )
            }
            GameStatus.PAUSED -> {
                if (showSettings) {
                    SettingsOverlay(
                        isUserSignedIn = isUserSignedIn,
                        userEmail = userEmail,
                        isSoundEnabled = isSoundEnabled,
                        isVibrationEnabled = isVibrationEnabled,
                        unlockedPromoCode = unlockedPromoCode,
                        onSignIn = onSignIn,
                        onSignOut = onSignOut,
                        onToggleSound = { viewModel.toggleSound() },
                        onToggleVibration = { viewModel.toggleVibration() },
                        onArTryOn = onNavigateToAR,
                        onShowTutorial = {
                            showSettings = false
                            viewModel.startTutorial()
                        },
                        onClose = {
                            showSettings = false
                            viewModel.togglePause()
                        }
                    )
                }
            }
            else -> {}
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoBackground(videoResId: Int, isSoundEnabled: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/$videoResId")
            setMediaItem(mediaItem)
            prepare()
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }

    // Управление звуком видео в реальном времени
    LaunchedEffect(isSoundEnabled) {
        exoPlayer.volume = if (isSoundEnabled) 1f else 0f
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun WelcomeDialogContent(onDismiss: () -> Unit, onTryOn: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Добро пожаловать!",
                fontSize = 24.sp,
                color = Color.Yellow,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Примерь нашу фирменную панаму прямо сейчас! \n\nА если пройдешь игру до конца, то получишь секретный промокод на покупку в нашем магазине! \n\nВы всегда сможете вернуться к примерке через настройки игры.",
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onTryOn,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow),
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Примерить панаму", color = Color.Black, fontSize = 18.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Понятно", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            }
        }
    }
}
