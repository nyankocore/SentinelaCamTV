package com.sentinela.camtv.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playerPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_preferences",
)

class PlayerPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    val preferences: Flow<PlayerUiPreferences> = dataStore.data.map { preferences ->
        PlayerUiPreferences(
            showPlayerInfo = preferences[SHOW_PLAYER_INFO] ?: false,
            showMosaicInfo = preferences[SHOW_MOSAIC_INFO] ?: preferences[SHOW_PLAYER_INFO] ?: false,
            showFullscreenInfo = preferences[SHOW_FULLSCREEN_INFO] ?: preferences[SHOW_PLAYER_INFO] ?: false,
            fullscreenQuickMenuHintSeen = preferences[FULLSCREEN_QUICK_MENU_HINT_SEEN] ?: false,
            mosaicStreamQuality = preferences[MOSAIC_STREAM_QUALITY]
                ?.let { value -> runCatching { StreamQuality.valueOf(value) }.getOrNull() }
                ?: StreamQuality.SD,
            globalTransmissionMode = preferences[GLOBAL_TRANSMISSION_MODE]
                ?.let { value -> runCatching { TransmissionMode.valueOf(value) }.getOrNull() }
                ?: TransmissionMode.MENOR_LATENCIA,
            freeActiveCameraId = preferences[FREE_ACTIVE_CAMERA_ID],
            diagnosticsEnabled = preferences[DIAGNOSTICS_ENABLED] ?: true,
            premiumGraceUntilEpochMillis = preferences[PREMIUM_GRACE_UNTIL_EPOCH_MILLIS] ?: 0L,
        )
    }

    override fun observePreferences(): Flow<PlayerUiPreferences> = preferences

    override suspend fun setShowPlayerInfo(showPlayerInfo: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_PLAYER_INFO] = showPlayerInfo
            preferences[SHOW_MOSAIC_INFO] = showPlayerInfo
            preferences[SHOW_FULLSCREEN_INFO] = showPlayerInfo
        }
    }

    override suspend fun setShowMosaicInfo(showMosaicInfo: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_MOSAIC_INFO] = showMosaicInfo
        }
    }

    override suspend fun setShowFullscreenInfo(showFullscreenInfo: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_FULLSCREEN_INFO] = showFullscreenInfo
        }
    }

    override suspend fun setFullscreenQuickMenuHintSeen(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[FULLSCREEN_QUICK_MENU_HINT_SEEN] = seen
        }
    }

    override suspend fun setMosaicStreamQuality(streamQuality: StreamQuality) {
        dataStore.edit { preferences ->
            preferences[MOSAIC_STREAM_QUALITY] = streamQuality.name
        }
    }

    override suspend fun setGlobalTransmissionMode(transmissionMode: TransmissionMode) {
        dataStore.edit { preferences ->
            preferences[GLOBAL_TRANSMISSION_MODE] = transmissionMode.name
        }
    }

    override suspend fun setFreeActiveCameraId(cameraId: String?) {
        dataStore.edit { preferences ->
            if (cameraId == null) {
                preferences.remove(FREE_ACTIVE_CAMERA_ID)
            } else {
                preferences[FREE_ACTIVE_CAMERA_ID] = cameraId
            }
        }
    }

    override suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIAGNOSTICS_ENABLED] = enabled
        }
    }

    override suspend fun setPremiumGraceUntilEpochMillis(epochMillis: Long) {
        dataStore.edit { preferences ->
            preferences[PREMIUM_GRACE_UNTIL_EPOCH_MILLIS] = epochMillis
        }
    }

    private companion object {
        val SHOW_PLAYER_INFO = booleanPreferencesKey("show_player_info")
        val SHOW_MOSAIC_INFO = booleanPreferencesKey("show_mosaic_info")
        val SHOW_FULLSCREEN_INFO = booleanPreferencesKey("show_fullscreen_info")
        val FULLSCREEN_QUICK_MENU_HINT_SEEN = booleanPreferencesKey("fullscreen_quick_menu_hint_seen")
        val MOSAIC_STREAM_QUALITY = stringPreferencesKey("mosaic_stream_quality")
        val GLOBAL_TRANSMISSION_MODE = stringPreferencesKey("global_transmission_mode")
        val FREE_ACTIVE_CAMERA_ID = stringPreferencesKey("free_active_camera_id")
        val DIAGNOSTICS_ENABLED = booleanPreferencesKey("diagnostics_enabled")
        val PREMIUM_GRACE_UNTIL_EPOCH_MILLIS = longPreferencesKey("premium_grace_until_epoch_millis")
    }
}

fun playerPreferencesRepository(context: Context): PlayerPreferencesRepository =
    PlayerPreferencesRepository(context.applicationContext.playerPreferencesDataStore)
