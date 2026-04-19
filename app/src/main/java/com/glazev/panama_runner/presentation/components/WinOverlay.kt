package com.glazev.panama_runner.presentation.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun WinOverlay(
    state: GameState,
    viewModel: GameViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val animProgress = remember { Animatable(0f) }
    
    val promoCode = "PANAMA2024"
    
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
                .padding(16.dp) // Уменьшили отступ снаружи
                .alpha(animProgress.value),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(20.dp) // Уменьшили внутренний отступ
                    .verticalScroll(scrollState), // Добавили прокрутку на случай малых экранов
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ПОБЕДА!",
                    fontSize = 36.sp, // Немного уменьшили заголовок
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
                        promoCode = promoCode,
                        onRevealed = { viewModel.onWin(promoCode) }
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
    promoCode: String,
    onRevealed: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Сетка для стирания (10x10)
    val rows = 15
    val cols = 10
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }
    val totalPoints = rows * cols
    val threshold = 0.4f // Стереть 40% чтобы открыть полностью
    
    var isFullyRevealed by remember { mutableStateOf(false) }

    // Вызываем коллбэк, когда карточка полностью открыта
    LaunchedEffect(isFullyRevealed) {
        if (isFullyRevealed) {
            onRevealed()
        }
    }

    Box(
        modifier = Modifier
            .size(width = 240.dp, height = 100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFCE4EC))
            .pointerInput(isFullyRevealed) {
                if (isFullyRevealed) {
                    detectTapGestures(
                        onLongPress = {
                            clipboardManager.setText(AnnotatedString(promoCode))
                            Toast.makeText(context, "Промокод скопирован!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Промокод под защитным слоем
        Text(
            text = promoCode,
            fontSize = 32.sp,
            color = Color(0xFFE91E63),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
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
                            
                            if (revealed.size > totalPoints * threshold) {
                                isFullyRevealed = true
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        // Для свайпа (стирания пальцем)
                        detectDragGestures { change, _ ->
                            val offset = change.position
                            val xIdx = (offset.x / size.width * cols).toInt().coerceIn(0, cols - 1)
                            val yIdx = (offset.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                            revealed[yIdx * cols + xIdx] = true
                            
                            if (revealed.size > totalPoints * threshold) {
                                isFullyRevealed = true
                            }
                        }
                    }
            ) {
                // Рисуем серый защитный слой
                drawRect(color = Color.LightGray)
                
                // "Стираем" ячейки
                for (i in 0 until rows) {
                    for (j in 0 until cols) {
                        if (revealed[i * cols + j] == true) {
                            // Прозрачные круги для "стертого" эффекта
                            drawCircle(
                                color = Color.Transparent,
                                radius = size.width / cols,
                                center = Offset(
                                    x = (j + 0.5f) * (size.width / cols),
                                    y = (i + 0.5f) * (size.height / rows)
                                ),
                                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                            )
                        }
                    }
                }
                
                // Текст-подсказка сверху слоя
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
