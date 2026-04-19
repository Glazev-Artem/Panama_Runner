package com.glazev.panama_runner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.glazev.panama_runner.presentation.ar.ARScreen
import com.glazev.panama_runner.presentation.game.GameScreen
import com.glazev.panama_runner.ui.theme.Panama_RunnerTheme
import com.yandex.mobile.ads.common.MobileAds
//import com.yandex.mobileads.common.MobileAds
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация Yandex Mobile Ads SDK
        MobileAds.initialize(this) {
            // Инициализация завершена
        }

        enableEdgeToEdge()
        hideSystemUI()
        setContent {
            Panama_RunnerTheme {
                var currentScreen by rememberSaveable { mutableStateOf("game") }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (currentScreen == "ar") Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    if (currentScreen == "game") {
                        GameScreen(onNavigateToAR = { currentScreen = "ar" })
                    } else {
                        ARScreen(onBack = { currentScreen = "game" })
                    }
                }
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
