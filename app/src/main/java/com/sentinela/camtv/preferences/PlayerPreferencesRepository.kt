package com.sentinela.camtv.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sentinela.camtv.data.mosaic.MOSAIC_COUNT
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
            showPlayerInfo = preferences[SHOW_PLAYER_INFO] ?: true,
            showMosaicInfo = preferences[SHOW_MOSAIC_INFO] ?: preferences[SHOW_PLAYER_INFO] ?: true,
            showFullscreenInfo = preferences[SHOW_FULLSCREEN_INFO] ?: preferences[SHOW_PLAYER_INFO] ?: true,
            fullscreenQuickMenuHintSeen = preferences[FULLSCREEN_QUICK_MENU_HINT_SEEN] ?: false,
            mosaicStreamQuality = preferences[MOSAIC_STREAM_QUALITY]
                ?.let { value -> runCatching { StreamQuality.valueOf(value) }.getOrNull() }
                ?: StreamQuality.SD,
            globalTransmissionMode = preferences[GLOBAL_TRANSMISSION_MODE]
                ?.let { value -> runCatching { TransmissionMode.valueOf(value) }.getOrNull() }
                ?: TransmissionMode.MENOR_LATENCIA,
            activeMosaicIndex = (preferences[ACTIVE_MOSAIC_INDEX] ?: 0).coerceIn(0, MOSAIC_COUNT - 1),
            photoCaptureTreeUri = preferences[PHOTO_CAPTURE_TREE_URI],
            appLanguageTag = preferences[APP_LANGUAGE_TAG],
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

    override suspend fun setActiveMosaicIndex(index: Int) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_MOSAIC_INDEX] = index.coerceIn(0, MOSAIC_COUNT - 1)
        }
    }

    override suspend fun setPhotoCaptureTreeUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(PHOTO_CAPTURE_TREE_URI)
            } else {
                preferences[PHOTO_CAPTURE_TREE_URI] = uri
            }
        }
    }

    override suspend fun setAppLanguageTag(tag: String?) {
        dataStore.edit { preferences ->
            if (tag.isNullOrBlank()) {
                preferences.remove(APP_LANGUAGE_TAG)
            } else {
                preferences[APP_LANGUAGE_TAG] = tag
            }
        }
    }

    private companion object {
        val SHOW_PLAYER_INFO = booleanPreferencesKey("show_player_info")
        val SHOW_MOSAIC_INFO = booleanPreferencesKey("show_mosaic_info")
        val SHOW_FULLSCREEN_INFO = booleanPreferencesKey("show_fullscreen_info")
        val FULLSCREEN_QUICK_MENU_HINT_SEEN = booleanPreferencesKey("fullscreen_quick_menu_hint_seen")
        val MOSAIC_STREAM_QUALITY = stringPreferencesKey("mosaic_stream_quality")
        val GLOBAL_TRANSMISSION_MODE = stringPreferencesKey("global_transmission_mode")
        val ACTIVE_MOSAIC_INDEX = intPreferencesKey("active_mosaic_index")
        val PHOTO_CAPTURE_TREE_URI = stringPreferencesKey("photo_capture_tree_uri")
        val APP_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")
    }
}

fun playerPreferencesRepository(context: Context): PlayerPreferencesRepository =
    PlayerPreferencesRepository(context.applicationContext.playerPreferencesDataStore)
