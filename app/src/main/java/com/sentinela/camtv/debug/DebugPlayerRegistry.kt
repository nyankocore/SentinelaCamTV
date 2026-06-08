package com.sentinela.camtv.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class DebugPlayerSnapshot(
    val activePlayers: Int = 0,
    val reconnectRequests: Int = 0,
    val forceReconnectToken: Int = 0,
    val lastError: String? = null,
    val lastReconnectReason: String? = null,
)

object DebugPlayerRegistry {
    private val activePlayerIds = mutableSetOf<String>()
    private val _state = MutableStateFlow(DebugPlayerSnapshot())
    val state: StateFlow<DebugPlayerSnapshot> = _state

    fun register(playerId: String) {
        synchronized(activePlayerIds) {
            activePlayerIds += playerId
            _state.update { snapshot -> snapshot.copy(activePlayers = activePlayerIds.size) }
        }
    }

    fun unregister(playerId: String) {
        synchronized(activePlayerIds) {
            activePlayerIds -= playerId
            _state.update { snapshot -> snapshot.copy(activePlayers = activePlayerIds.size) }
        }
    }

    fun reportError(message: String?) {
        if (message.isNullOrBlank()) return
        _state.update { snapshot -> snapshot.copy(lastError = message.take(160)) }
    }

    fun reportReconnect(reason: String) {
        _state.update { snapshot ->
            snapshot.copy(
                reconnectRequests = snapshot.reconnectRequests + 1,
                lastReconnectReason = reason.take(160),
            )
        }
    }

    fun forceReconnect() {
        _state.update { snapshot ->
            snapshot.copy(forceReconnectToken = snapshot.forceReconnectToken + 1)
        }
    }
}
