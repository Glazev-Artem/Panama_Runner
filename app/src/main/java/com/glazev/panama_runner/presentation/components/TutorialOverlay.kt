package com.glazev.panama_runner.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialOverlay(
    step: Int,
    onNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tutorial")
    val handOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handAnim"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (step) {
                    1 -> "Двигай пальцем влево и вправо,\nчтобы управлять коробкой!"
                    2 -> "Лови ПРАВИЛЬНЫЕ панамы,\nчтобы набирать очки!"
                    3 -> "НЕ ЛОВИ брак!\nСледи за счетчиком слева."
                    4 -> "Набери 5000 очков,\nчтобы получить ПРОМОКОД!"
                    else -> ""
                },
                color = Color.White.copy(alpha = 1f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (step == 1) {
                // Анимация руки (виртуальная)
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        color = Color.Yellow,
                        radius = 20f,
                        center = Offset(size.width / 2 + handOffset, size.height / 2),
                        alpha = 0.8f
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = 30f,
                        center = Offset(size.width / 2 + handOffset, size.height / 2),
                        style = Stroke(width = 4f),
                        alpha = alpha
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (step < 4) "Далее" else "Поехали!",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
        
        // Стрелки-указатели в зависимости от шага
        when(step) {
            3 -> { // Указатель на Брак
                ArrowPointer(Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 60.dp))
            }
            4 -> { // Указатель на Шкалу
                ArrowPointer(Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
            }
        }
    }
}

@Composable
fun ArrowPointer(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAnim"
    )

    Text(
        text = "↓",
        color = Color.Yellow,
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.offset(y = yOffset.dp)
    )
}
