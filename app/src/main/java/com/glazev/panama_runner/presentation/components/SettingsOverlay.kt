package com.glazev.panama_runner.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.glazev.panama_runner.R

@Composable
fun SettingsOverlay(
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
    onShowTutorial: () -> Unit,
    onClose: () -> Unit
) {
    var showInstructions by remember { mutableStateOf(false) }
    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    val panamas = remember {
        val good = (1..7).map { Pair(context.resources.getIdentifier("panama_$it", "drawable", context.packageName), "Лови ✅") }
        val bad = (1..6).map { Pair(context.resources.getIdentifier("brak_panama_$it", "drawable", context.packageName), "Не лови ❌") }
        (good + bad).filter { it.first != 0 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.7f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!showInstructions) {
                    MainSettingsContent(
                        isUserSignedIn = isUserSignedIn,
                        userEmail = userEmail,
                        isSoundEnabled = isSoundEnabled,
                        isVibrationEnabled = isVibrationEnabled,
                        unlockedPromoCode = unlockedPromoCode,
                        onSignIn = onSignIn,
                        onSignOut = onSignOut,
                        onToggleSound = onToggleSound,
                        onToggleVibration = onToggleVibration,
                        onArTryOn = onArTryOn,
                        onShowTutorial = onShowTutorial,
                        onShowInstructions = { showInstructions = true },
                        onClose = onClose
                    )
                } else {
                    InstructionsContent(
                        panamas = panamas,
                        onBack = { showInstructions = false },
                        onOpenImage = { index -> fullscreenIndex = index }
                    )
                }
            }
        }

        if (fullscreenIndex != null) {
            val pagerState = rememberPagerState(initialPage = fullscreenIndex!!, pageCount = { panamas.size })
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) { detectTapGestures { fullscreenIndex = null } },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                        val (resId, label) = panamas[page]
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.6f), contentScale = ContentScale.Fit)
                            Spacer(modifier = Modifier.height(30.dp))
                            Text(text = label, color = if (label.contains("✅")) Color.Green else Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Свайп для листания • Нажми, чтобы выйти", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 30.dp))
                }
            }
        }
    }
}

@Composable
private fun MainSettingsContent(
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
    onShowTutorial: () -> Unit,
    onShowInstructions: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("НАСТРОЙКИ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Text("❌", color = Color.White) }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isUserSignedIn) "👤" else "🔑", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (isUserSignedIn) "Аккаунт" else "Сохранение", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (isUserSignedIn && userEmail != null) Text(userEmail, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { if (isUserSignedIn) onSignOut() else onSignIn() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isUserSignedIn) Color.DarkGray else Color(0xFFFFCC00)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = if (isUserSignedIn) "ВЫЙТИ" else "ВОЙТИ", color = Color.Black, fontSize = 11.sp)
                    }
                }

                SettingItemCompact(icon = if (isSoundEnabled) "🔊" else "🔈", title = "Звук", value = if (isSoundEnabled) "ВКЛ" else "ВЫКЛ", onClick = onToggleSound)
                SettingItemCompact(icon = if (isVibrationEnabled) "📳" else "📴", title = "Вибрация", value = if (isVibrationEnabled) "ВКЛ" else "ВЫКЛ", onClick = onToggleVibration)
                SettingItemCompact(icon = "🤳", title = "Примерить панаму", value = "📸", onClick = onArTryOn)
                SettingItemCompact(icon = "🎓", title = "Обучение", value = "▶️", onClick = onShowTutorial)
                SettingItemCompact(icon = "📖", title = "Инструкция", value = "👁️", onClick = onShowInstructions)
                
                if (unlockedPromoCode != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE91E63).copy(0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE91E63).copy(0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(unlockedPromoCode))
                                Toast.makeText(context, "Промокод скопирован!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ВАШ ПРОМОКОД:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(unlockedPromoCode, color = Color(0xFFE91E63), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text("(нажмите, чтобы скопировать)", color = Color.Gray, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Кастомный индикатор скролла (полоса прокрутки)
            if (scrollState.maxValue > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 10.dp)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(0.1f), CircleShape)
                ) {
                    val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.2f)
                            .graphicsLayer {
                                translationY = scrollRatio * (size.height * 4f)
                            }
                            .background(Color(0xFFFFCC00), CircleShape)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // ВЕРСИЯ И ССЫЛКА НА RUSTORE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Версия 1.0.0", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(15.dp))
            Text(
                text = "Лавка приложений",
                color = Color(0xFFFFCC00),
                fontSize = 11.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rustore.ru/catalog/developer/yqcezb2f"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun InstructionsContent(
    panamas: List<Pair<Int, String>>,
    onBack: () -> Unit,
    onOpenImage: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("КАК ИГРАТЬ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onBack) { Text("⬅️", color = Color.White) }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(end = 12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Цель: Лови падающие панамки в коробку. Набери как можно больше очков!", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(20.dp))
                Text("ОБРАЗЦЫ (нажми для просмотра):", color = Color(0xFFFFCC00), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(panamas.size) { index ->
                        val (resId, label) = panamas[index]
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOpenImage(index) }) {
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.1f)), contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                            }
                            Text(label, color = if (label.contains("✅")) Color.Green else Color.Red, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFCC00).copy(0.1f), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFFFCC00), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Column {
                        Text("🎁 ПРИЗ ЗА ИГРУ:", color = Color(0xFFFFCC00), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("По завершении вы получите промокод на скидку 50% на Wildberries!", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🛒 Купите сразу, отсканировав QR на главном экране или зажав его (длинный тап).", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)), modifier = Modifier.fillMaxWidth().height(45.dp)) {
                Text("К ИГРЕ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // КАРТОМНЫЙ ИНДИКАТОР СКРОЛЛА
        if (scrollState.maxValue > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 70.dp, bottom = 70.dp)
                    .width(4.dp)
                    .fillMaxHeight(0.6f)
                    .background(Color.White.copy(0.1f), CircleShape)
            ) {
                val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.2f) // Бегунок занимает 20% высоты дорожки
                        .graphicsLayer {
                            // Так как бегунок = 20%, то вся дорожка = 5 бегунков.
                            // Чтобы дойти до низа, ему нужно сместиться на 4 свои высоты (80% дорожки).
                            translationY = scrollRatio * (size.height * 4f)
                        }
                        .background(Color(0xFFFFCC00), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SettingItemCompact(icon: String, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).clickable { onClick() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(value, color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
