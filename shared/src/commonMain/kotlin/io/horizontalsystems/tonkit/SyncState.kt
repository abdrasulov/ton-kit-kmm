package io.horizontalsystems.tonkit

sealed class SyncState {
    class Synced : SyncState()
    class NotSynced(val error: Throwable) : SyncState()
    class Syncing : SyncState()
}

sealed class SyncError : Exception() {
    class NotStarted : SyncError()
    class NoNetworkConnection : SyncError()
}
