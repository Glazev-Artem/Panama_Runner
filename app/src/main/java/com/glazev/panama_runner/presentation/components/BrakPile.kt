package com.glazev.panama_runner.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.glazev.panama_runner.domain.models.GameStatus
import kotlin.random.Random

/**
 * Простая хаотичная куча без пирамидальной логики.
 * Позиции фиксированы, чтобы не было прыжков.
 */
@Composable
fun BrakPile(count: Int, status: GameStatus, forceForeground: Boolean = false) {
    val context = LocalContext.current
    val isGameOver = status == GameStatus.GAME_OVER
    val visualCount = count.coerceAtMost(60)

    val panamas = remember {
        (1..7).map {
            val id = context.resources.getIdentifier("panama_$it", "drawable", context.packageName)
            BitmapFactory.decodeResource(context.resources, id).asImageBitmap()
        }
    }

    val fallProgress by animateFloatAsState(
        targetValue = if (isGameOver) 1f else 0f,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "pileFall"
    )

    if (forceForeground && !isGameOver) return

    // ГЕНЕРИРУЕМ ФИКСИРОВАННЫЕ ПОЗИЦИИ (один раз)
    // Просто набрасываем их в центр без всяких пирамид
    val positions = remember {
        val list = mutableListOf<StableItem>()
        val rnd = Random(1337)
        
        for (i in 0 until 100) {
            list.add(StableItem(
                pIdx = rnd.nextInt(7),
                xRel = (rnd.nextFloat() * 220f - 110f),
                yRel = -(rnd.nextFloat() * 150f + 60f),
                rot = rnd.nextFloat() * 360f,
                tX = (rnd.nextFloat() * 300f - 150f),
                tY = (rnd.nextFloat() * 400f + 200f),
                tRot = rnd.nextFloat() * 360f
            ))
        }
        list
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h * 0.85f 
        
        val itemSizePx = 95.dp.toPx()

        for (i in 0 until visualCount) {
            val item = positions[i]
            val p = fallProgress
            
            val sX = centerX + item.xRel.dp.toPx()
            val sY = centerY + item.yRel.dp.toPx()
            
            val eX = sX + item.tX.dp.toPx()
            val eY = sY + item.tY.dp.toPx()
            
            val curX = sX + (eX - sX) * p
            val curY = sY + (eY - sY) * p
            val curRot = item.rot + (item.tRot * p)
            
            val scale = if (forceForeground) (1f + p * 6.5f) else 1f
            val size = itemSizePx * scale
            val alpha = if (p > 0.9f) (1f - (p - 0.9f) * 10f).coerceIn(0f, 1f) else 1f

            withTransform({
                translate(curX, curY)
                rotate(curRot)
            }) {
                drawImage(
                    image = panamas[item.pIdx],
                    dstOffset = IntOffset(-(size / 2).toInt(), -(size / 2).toInt()),
                    dstSize = IntSize(size.toInt(), size.toInt()),
                    alpha = alpha
                )
            }
        }
    }
}

private data class StableItem(
    val pIdx: Int,
    val xRel: Float,
    val yRel: Float,
    val rot: Float,
    val tX: Float,
    val tY: Float,
    val tRot: Float
)
