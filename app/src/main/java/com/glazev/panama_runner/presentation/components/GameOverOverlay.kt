package com.glazev.panama_runner.presentation.components

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.panama_runner.domain.models.GameState
import com.glazev.panama_runner.presentation.game.AdsManager
import com.glazev.panama_runner.presentation.game.GameViewModel

/**
 * Оверлей окончания игры.
 * Теперь появляется С ЗАДЕРЖКОЙ, чтобы игрок увидел "завал" из панам.
 */
@Composable
fun GameOverOverlay(
    state: GameState,
    viewModel: GameViewModel,
    adsManager: AdsManager
) {
    val context = LocalContext.current
    
    // Задержка появления черного экрана, чтобы успеть увидеть анимацию BrakPile
    val showOverlay = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000) // 1 секунда завала, потом кнопки
        showOverlay.value = true
    }

    if (showOverlay.value) {
        val animAlpha = animateFloatAsState(
            targetValue = 0.85f,
            animationSpec = tween(1200),
            label = "overlayFade"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = animAlpha.value))
                .alpha(animAlpha.value)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ИГРА ОКОНЧЕНА",
                    fontSize = 42.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { viewModel.restartGame() },
                    modifier = Modifier.width(260.dp).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("В МЕНЮ", color = Color.Black, fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                if (state.isOnlineSession && adsManager.isAdLoaded()) {
                    Button(
                        onClick = {
                            adsManager.showRewardedVideo(
                                activity = context as Activity,
                                onAdStarted = {},
                                onRewardGranted = {
                                    viewModel.onAdRewarded()
                                }
                            )
                        },
                        modifier = Modifier.width(260.dp).height(80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.CenterStart)
                            )
                            Text(
                                text = "ПРОДОЛЖИТЬ\n-6 БРАКА",
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                } else {
                    val message = when {
                        !state.isOnlineSession -> "Продолжение недоступно\nв оффлайн-режиме"
                        else -> "Реклама загружается или\nнет связи..."
                    }
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
