package io.horizontalsystems.tonkit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class ConnectionManager {
    actual val isConnectedFlow: StateFlow<Boolean> = MutableStateFlow(true)
    actual fun start() {}
    actual fun stop() {}
}
