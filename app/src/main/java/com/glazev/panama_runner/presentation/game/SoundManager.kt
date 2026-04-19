package com.glazev.panama_runner.presentation.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.glazev.panama_runner.R

class SoundManager(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var catchSound: Int = 0
    private var errorSound: Int = 0
    private var gameOverSound: Int = 0

    init {
        // Пытаемся загрузить звуки, если файлы появятся в raw
        // Если файлов нет, id будет 0 и звук просто не проиграется
        catchSound = loadSound(context, "catch_sound")
        errorSound = loadSound(context, "error_sound")
        gameOverSound = loadSound(context, "game_over_sound")
    }

    private fun loadSound(context: Context, name: String): Int {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (id != 0) soundPool.load(context, id, 1) else 0
    }

    fun playCatch() { if (catchSound != 0) soundPool.play(catchSound, 1f, 1f, 1, 0, 1f) }
    fun playError() { if (errorSound != 0) soundPool.play(errorSound, 1f, 1f, 1, 0, 1f) }
    fun playGameOver() { if (gameOverSound != 0) soundPool.play(gameOverSound, 1f, 1f, 1, 0, 1f) }

    fun release() {
        soundPool.release()
    }
}
