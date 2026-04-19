package com.glazev.panama_runner.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.panama_runner.R
import com.glazev.panama_runner.domain.models.GameState

/**
 * Интерфейс пользователя во время игры (HUD).
 * Отображает прогресс, очки, рекорд и кнопку паузы.
 */
@Composable
fun GameHUD(
    state: GameState,
    onPause: () -> Unit
) {
    val progress = (state.score.toFloat() / GameState.MAX_SCORE).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnim")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Брак (слева)
        Text(
            text = "Брак: ${state.missedCount}/16",
            color = if (state.missedCount >= 14) Color.Red else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .height(42.dp)
                .background(Color.Black.copy(0.4f), RoundedCornerShape(21.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .wrapContentHeight()
        )

        // 2. Прогресс и Очки (шкала расширена до кубка)
        var boxSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .padding(vertical = 4.dp)
                .onSizeChanged { boxSize = it }
        ) {
            // Пустая полоса (фон)
            Image(
                bitmap = ImageBitmap.imageResource(id = R.drawable.polosa_dostizheniy_pustaya),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Заполненная полоса (проявляется слева направо)
            Image(
                bitmap = ImageBitmap.imageResource(id = R.drawable.polosa_dostizheniy_zapolnenie),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        // Отрисовываем только ту часть картинки, которая соответствует прогрессу
                        clipRect(right = size.width * animatedProgress) {
                            this@drawWithContent.drawContent()
                        }
                    },
                contentScale = ContentScale.FillBounds
            )

            // Бегунок (двигается по краю прогресса)
            if (boxSize.width > 0) {
                val thumbWidth = with(density) { 34.dp.toPx() }
                val xOffset = (boxSize.width * animatedProgress) - (thumbWidth / 2f)
                
                Image(
                    bitmap = ImageBitmap.imageResource(id = R.drawable.prokrutka_peremeshenie),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = with(density) { xOffset.toDp() })
                        .size(34.dp)
                )
            }
            
            // Очки поверх шкалы
            Text(
                text = "${state.score}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 4. Рекорд (🏆)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(42.dp)
                .background(Color.Black.copy(0.4f), CircleShape)
                .padding(horizontal = 12.dp)
        ) {
            Text("🏆", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${state.bestScore}",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // 5. Пауза (||)
        IconButton(
            onClick = onPause,
            modifier = Modifier
                .size(42.dp)
                .background(Color.Black.copy(0.4f), CircleShape)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(width = 4.dp, height = 16.dp).background(Color.White))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(width = 4.dp, height = 16.dp).background(Color.White))
            }
        }
    }
}
