package com.glazev.panama_runner.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.panama_runner.R

/**
 * Главный экран меню игры.
 */
@Composable
fun MenuScreen(
    onStart: (Boolean) -> Unit, // Изменено: передаем статус сети
    isUserSignedIn: Boolean,
    userEmail: String?,
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    unlockedPromoCode: String? = null,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onArTryOn: () -> Unit,
    onShowTutorial: () -> Unit, // Новый параметр
    isNetworkAvailable: Boolean // Новый параметр
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var isPressed by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f, // Уменьшение на 10%
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "buttonScale"
    )

    // Диалог оффлайн-режима
    if (showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = { Text("НЕТ ИНТЕРНЕТА") },
            text = { Text("В оффлайн-режиме вы не сможете получить промокод и продолжить игру за рекламу. Продолжить?") },
            confirmButton = {
                Button(onClick = { 
                    showOfflineDialog = false
                    onStart(false) 
                }) {
                    Text("ДА")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfflineDialog = false }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = ImageBitmap.imageResource(id = R.drawable.zastavka_panama),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Верхний правый блок: Настройки и невидимая область над QR-кодом
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 15.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Прозрачная область для клика по QR-коду
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp, y = (-10).dp) // Смещение влево на 63% и вверх на 10%
                    .size(width = 105.dp, height = 126.dp) // Ширина 105, Высота: +10% вверх и +10% вниз
                    //.border(1.dp, Color.Red.copy(0.5f)) // Убрали отладочную границу
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wildberries.ru/catalog/253765354/detail.aspx?targetUrl=GP"))
                                context.startActivity(intent)
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .size(58.dp)
                    .offset(x = (-20).dp, y = (-30).dp)
                    .background(Color.Black.copy(0.4f), CircleShape)
            ) {
                Text("⚙️", fontSize = 38.sp)
            }
        }

        // Кнопка СТАРТ с анимацией стрелок (кадры _1, _2, ..., _7)
        var currentFrame by remember { mutableStateOf(1) }
        LaunchedEffect(Unit) {
            while(true) {
                kotlinx.coroutines.delay(100) // Скорость анимации (100мс)
                currentFrame = if (currentFrame < 7) currentFrame + 1 else 1
            }
        }

        val buttonResId = remember(currentFrame) {
            context.resources.getIdentifier("button$currentFrame", "drawable", context.packageName)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // bottom = 10% высоты, end = 12% ширины (регулируйте эти коэффициенты)
                .padding(bottom = screenHeight * 0.08f, end = screenWidth * 0.0f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (buttonResId != 0) {
                Image(
                    bitmap = ImageBitmap.imageResource(id = buttonResId),
                    contentDescription = "СТАРТ",
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        // Размер кнопки также делаем адаптивным (72% ширины и 14% высоты)
                        .width(screenWidth * 0.5f)
                        .height(screenHeight * 0.18f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    // Мгновенная реакция
                                    kotlinx.coroutines.delay(80) 
                                    isPressed = false
                                    if (isNetworkAvailable) {
                                        onStart(true)
                                    } else {
                                        showOfflineDialog = true
                                    }
                                }
                            )
                        },
                    contentScale = ContentScale.Fit
                )
            } else {
                // Если ресурсы еще не проиндексированы, показываем старую кнопку или заглушку
                Button(
                    onClick = { 
                        if (isNetworkAvailable) onStart(true) else showOfflineDialog = true 
                    },
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .width(220.dp)
                        .height(70.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                    shape = RoundedCornerShape(35.dp)
                ) {
                    Text("СТАРТ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        if (showSettings) {
            SettingsOverlay(
                isUserSignedIn = isUserSignedIn,
                userEmail = userEmail,
                isSoundEnabled = isSoundEnabled,
                isVibrationEnabled = isVibrationEnabled,
                unlockedPromoCode = unlockedPromoCode,
                onSignIn = { 
                    showSettings = false
                    onSignIn() 
                },
                onSignOut = onSignOut,
                onToggleSound = onToggleSound,
                onToggleVibration = onToggleVibration,
                onArTryOn = {
                    showSettings = false
                    onArTryOn()
                },
                onShowTutorial = {
                    showSettings = false
                    onShowTutorial()
                },
                onClose = { showSettings = false }
            )
        }
    }
}
