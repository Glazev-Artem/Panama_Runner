package com.glazev.panama_runner.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import android.graphics.Paint
import android.graphics.Typeface
import com.glazev.panama_runner.domain.models.FloatingTextType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.glazev.panama_runner.R
import com.glazev.panama_runner.domain.models.GameObjectType
import com.glazev.panama_runner.domain.models.GameState
import com.glazev.panama_runner.domain.models.GameStatus
import com.glazev.panama_runner.domain.models.PlayerDirection
import com.glazev.panama_runner.domain.models.PlayerEmotion
import com.glazev.panama_runner.presentation.game.GameViewModel

@Composable
fun GameCanvas(state: GameState, viewModel: GameViewModel) {
    val context = LocalContext.current
    val currentState by rememberUpdatedState(state)

    // КЭШИРУЕМ ВСЕ БИТМАПЫ ОДИН РАЗ
    val panamas = remember { (1..7).map { ImageBitmap.imageResource(context.resources, context.resources.getIdentifier("panama_$it", "drawable", context.packageName)) } }
    val braks = remember { (1..6).map { ImageBitmap.imageResource(context.resources, context.resources.getIdentifier("brak_panama_$it", "drawable", context.packageName)) } }
    val korobs = remember { (1..5).map { ImageBitmap.imageResource(context.resources, context.resources.getIdentifier("korob_$it", "drawable", context.packageName)) } }
    val qrKorob = remember { ImageBitmap.imageResource(context.resources, R.drawable.korob_qr_code) }
    val conveyorEmpty = remember { ImageBitmap.imageResource(context.resources, R.drawable.konveer_pustoy) }
    val boxFrontLeft = remember { ImageBitmap.imageResource(context.resources, R.drawable.kprobka_pered_sloy_leviy) }
    val boxFrontRight = remember { ImageBitmap.imageResource(context.resources, R.drawable.kprobka_pered_sloy_praviy) }

    // Кэшируем персонажа
    val pIdleL = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_ulibka_leviy_otkr_glaza) }
    val pIdleR = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_ulibka_praviy_otkr_glaza) }
    val pComboL = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_ulibka_leviy_zakriti_glaza) }
    val pComboR = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_ulibka_praviy_zakriti_glaza) }
    val pAngryL = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_zlost_leviy) }
    val pAngryR = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_zlost_praviy) }
    val pInL = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_vdoh_leviy) }
    val pInR = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_vdoh_praviy) }
    val pOutL = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_vidoh_leviy) }
    val pOutR = remember { ImageBitmap.imageResource(context.resources, R.drawable.personazh_vidoh_praviy) }

    val playerBitmap = when (state.emotion) {
        PlayerEmotion.HAPPY -> if (state.playerDirection == PlayerDirection.LEFT) pComboL else pComboR
        PlayerEmotion.BREATHING -> {
            if (state.playerDirection == PlayerDirection.LEFT) {
                if (state.breathingCycle == 0) pInL else pOutL
            } else {
                if (state.breathingCycle == 0) pInR else pOutR
            }
        }
        else -> if (state.playerDirection == PlayerDirection.LEFT) pIdleL else pIdleR
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (currentState.status == GameStatus.PLAYING) {
                        change.consume()
                        viewModel.movePlayer(currentState.playerX + (dragAmount.x * 1.1f) / size.width)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Размеры и позиции
        val pw = (w * 0.35f).toInt(); val ph = (h * 0.85f).toInt()
        val playerBottomY = h * 0.95f
        val playerXPos = (state.playerX * w)
        val bw = (w * 0.187f).toInt(); val bh = (h * 0.225f).toInt()
        val boxX = playerXPos + (w * (if (state.playerDirection == PlayerDirection.RIGHT) 0.069f else -0.062f))
        val boxY = playerBottomY - bh - (h * 0.1f)
        val convH = (h * 0.22f).toInt(); val convY = h - convH + (h * 0.07f)
        val itemS = (w * 0.12f).toInt()

        // Функция отрисовки кучи (вынесена для управления слоями)
        val drawPile = {
            state.pileItems.forEach { item ->
                val bitmap = panamas[item.type.ordinal % 7]
                val s = (itemS * 0.8f * item.scale).toInt()
                withTransform({
                    rotate(degrees = item.rotation, pivot = Offset(item.x * w, item.y * h))
                }) {
                    drawImage(
                        bitmap,
                        dstOffset = IntOffset((item.x * w - s / 2).toInt(), (item.y * h - s / 2).toInt()),
                        dstSize = IntSize(s, s)
                    )
                }
            }
        }

        // 0. Куча на фоне (только во время игры)
        if (state.status != GameStatus.BURIAL && state.status != GameStatus.GAME_OVER) {
            drawPile()
        }

        // 1. Игрок
        drawImage(playerBitmap, dstOffset = IntOffset((playerXPos - pw/2).toInt(), (playerBottomY - ph).toInt()), dstSize = IntSize(pw, ph))

        // 1.1 Облачко с фразой
        state.bubbleText?.let { text ->
            drawIntoCanvas { canvas ->
                val bubblePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                val textPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 40f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                
                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(text, 0, text.length, textBounds)
                
                val padding = 20f
                val bubbleWidth = textBounds.width() + padding * 2
                val bubbleHeight = textBounds.height() + padding * 2
                
                val bx = playerXPos
                val by = playerBottomY - ph - 40f
                
                val rect = android.graphics.RectF(
                    bx - bubbleWidth / 2, 
                    by - bubbleHeight, 
                    bx + bubbleWidth / 2, 
                    by
                )
                
                // Рисуем облачко
                canvas.nativeCanvas.drawRoundRect(rect, 20f, 20f, bubblePaint)
                canvas.nativeCanvas.drawRoundRect(rect, 20f, 20f, borderPaint)
                
                // Хвостик облачка
                val path = android.graphics.Path().apply {
                    moveTo(bx - 15f, by)
                    lineTo(bx, by + 20f)
                    lineTo(bx + 15f, by)
                    close()
                }
                canvas.nativeCanvas.drawPath(path, bubblePaint)
                canvas.nativeCanvas.drawPath(path, borderPaint)
                
                // Текст
                canvas.nativeCanvas.drawText(text, bx, by - padding, textPaint)
            }
        }

        // 2. Падающие предметы
        state.objects.forEach { obj ->
            val bitmap = if (obj.type.name.startsWith("PANAMA")) panamas[obj.type.ordinal % 7] else braks[(obj.type.ordinal - 7).coerceAtMost(0) % 6]
            val s = if (obj.isCaught) (itemS * 0.7f).toInt() else itemS
            withTransform({ rotate(degrees = obj.rotation, pivot = Offset(obj.x * w, obj.y * h)) }) {
                drawImage(bitmap, dstOffset = IntOffset((obj.x * w - s/2).toInt(), (obj.y * h - s/2).toInt()), dstSize = IntSize(s, s))
            }
        }

        // 3. Коробка (передний слой)
        drawImage(if (state.playerDirection == PlayerDirection.LEFT) boxFrontLeft else boxFrontRight, dstOffset = IntOffset((boxX - bw/2).toInt(), boxY.toInt()), dstSize = IntSize(bw, bh))

        // 4. Конвейер
        drawImage(conveyorEmpty, dstOffset = IntOffset(0, convY.toInt()), dstSize = IntSize(w.toInt(), convH))
        
        state.conveyorObjects.forEach { obj ->
            val bitmap = if (obj.type == GameObjectType.KOROB_QR) qrKorob else korobs[(obj.type.ordinal - 13).coerceAtMost(0) % 5]
            
            // РАСЧЕТ ПРОПОРЦИЙ: Берем базовую ширину 14% от экрана и вычисляем высоту по картинке
            val ciw = (w * 0.14f).toInt()
            val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
            val cih = (ciw * aspectRatio).toInt()
            
            // Отрисовка: выравниваем по нижней линии конвейера
            drawImage(
                bitmap, 
                dstOffset = IntOffset(((obj.x * w) - (ciw / 2)).toInt(), (convY - cih + (h * 0.09f)).toInt()),
                dstSize = IntSize(ciw, cih)
            )
        }

        // 5. КУЧА ПОВЕРХ ВСЕГО (во время заваливания)
        if (state.status == GameStatus.BURIAL || state.status == GameStatus.GAME_OVER) {
            drawPile()
        }

        // 6. Всплывающий текст
        state.floatingTexts.forEach { ft ->
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = (ft.color).toInt()
                    textSize = when (ft.type) {
                        FloatingTextType.COMBO -> 64f
                        FloatingTextType.RECOVER -> 48f
                        else -> 42f
                    }
                    alpha = (ft.life * 255).toInt()
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(ft.text, ft.x * w, ft.y * h, paint)
            }
        }

        // 7. Частицы
        state.particles.forEach { p ->
            drawCircle(
                color = Color(p.color).copy(alpha = p.life),
                radius = p.size,
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}
