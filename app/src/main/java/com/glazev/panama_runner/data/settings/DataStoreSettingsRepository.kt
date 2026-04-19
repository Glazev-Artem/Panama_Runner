package com.glazev.panama_runner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glazev.panama_runner.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val soundEnabledKey = booleanPreferencesKey("sound_enabled")
    private val vibrationEnabledKey = booleanPreferencesKey("vibration_enabled")
    private val welcomeShownKey = booleanPreferencesKey("welcome_shown")
    private val tutorialShownKey = booleanPreferencesKey("tutorial_shown")
    private val unlockedPromoKey = stringPreferencesKey("unlocked_promo")

    override fun isSoundEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[soundEnabledKey] ?: true
    }

    override suspend fun toggleSound() {
        context.dataStore.edit { preferences ->
            val current = preferences[soundEnabledKey] ?: true
            preferences[soundEnabledKey] = !current
        }
    }

    override fun isVibrationEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[vibrationEnabledKey] ?: true
    }

    override suspend fun toggleVibration() {
        context.dataStore.edit { preferences ->
            val current = preferences[vibrationEnabledKey] ?: true
            preferences[vibrationEnabledKey] = !current
        }
    }

    override fun isWelcomeShown(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[welcomeShownKey] ?: false
    }

    override suspend fun setWelcomeShown() {
        context.dataStore.edit { preferences ->
            preferences[welcomeShownKey] = true
        }
    }

    override fun isTutorialShown(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[tutorialShownKey] ?: false
    }

    override suspend fun setTutorialShown() {
        context.dataStore.edit { preferences ->
            preferences[tutorialShownKey] = true
        }
    }

    override fun getUnlockedPromoCode(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[unlockedPromoKey]
    }

    override suspend fun saveUnlockedPromoCode(promoCode: String) {
        context.dataStore.edit { preferences ->
            preferences[unlockedPromoKey] = promoCode
        }
    }
}
