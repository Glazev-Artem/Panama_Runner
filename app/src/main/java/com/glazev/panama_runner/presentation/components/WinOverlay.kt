package com.glazev.panama_runner.presentation.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.panama_runner.domain.models.GameState
import com.glazev.panama_runner.presentation.game.GameViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun WinOverlay(
    state: GameState,
    viewModel: GameViewModel
) {
    val context = LocalContext.current
    val animProgress = remember { Animatable(0f) }
    
    // ПРЯМАЯ СВЯЗЬ: Берем код из состояния сессии
    val promoToDisplay = state.sessionPromoCode ?: "ПРОВЕРКА..."
    
    // Логируем для отладки
    LaunchedEffect(promoToDisplay) {
        Log.d("AR_DEBUG", "WinOverlay display code: $promoToDisplay")
    }

    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(1000, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .alpha(animProgress.value),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ПОБЕДА!",
                    fontSize = 36.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Вы собрали ${GameState.MAX_SCORE} панам!",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (state.isOnlineSession) {
                    ScratchCard(
                        promoCode = promoToDisplay
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wildberries.ru/catalog/253765354/detail.aspx?targetUrl=GP"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Text("ПЕРЕЙТИ НА WILDBERRIES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = "Промокод доступен только в онлайн-режиме.\nПодключите интернет в меню!",
                        fontSize = 15.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.restartGame() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text("В ГЛАВНОЕ МЕНЮ", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ScratchCard(
    promoCode: String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val rows = 15
    val cols = 10
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }
    val totalPoints = rows * cols
    val threshold = 0.4f 
    
    var isFullyRevealed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 240.dp, height = 100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFCE4EC))
            .pointerInput(isFullyRevealed, promoCode) {
                if (isFullyRevealed) {
                    detectTapGestures(
                        onLongPress = {
                            if (promoCode != "ПРОВЕРКА..." && !promoCode.startsWith("ОШИБКА")) {
                                clipboardManager.setText(AnnotatedString(promoCode))
                                Toast.makeText(context, "Промокод скопирован!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = promoCode,
            fontSize = if (promoCode.length > 15) 16.sp else 24.sp,
            color = Color(0xFFE91E63),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (promoCode.length > 15) 0.sp else 1.sp,
            textAlign = TextAlign.Center
        )

        if (!isFullyRevealed) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val xIdx = (offset.x / size.width * cols).toInt().coerceIn(0, cols - 1)
                            val yIdx = (offset.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                            revealed[yIdx * cols + xIdx] = true
                            if (revealed.size > totalPoints * threshold) isFullyRevealed = true
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val offset = change.position
                            val xIdx = (offset.x / size.width * cols).toInt().coerceIn(0, cols - 1)
                            val yIdx = (offset.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                            revealed[yIdx * cols + xIdx] = true
                            if (revealed.size > totalPoints * threshold) isFullyRevealed = true
                        }
                    }
            ) {
                drawRect(color = Color.LightGray)
                for (i in 0 until rows) {
                    for (j in 0 until cols) {
                        if (revealed[i * cols + j] == true) {
                            drawCircle(
                                color = Color.Transparent,
                                radius = size.width / cols,
                                center = Offset(x = (j + 0.5f) * (size.width / cols), y = (i + 0.5f) * (size.height / rows)),
                                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                            )
                        }
                    }
                }
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText("СОТРИ МЕНЯ", size.width / 2, size.height / 2 + 10f, paint)
                }
            }
        }
    }
}
