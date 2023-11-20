package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class Syncer(
    private val transactionManager: TransactionManager,
    private val balanceManager: BalanceManager,
//    private val connectionManager: ConnectionManager,
) {
    private val _balanceSyncStateFlow = MutableStateFlow<SyncState>(SyncState.NotSynced(SyncError.NotStarted()))
    val balanceSyncStateFlow: StateFlow<SyncState>
        get() = _balanceSyncStateFlow

    private val _transactionsSyncStateFlow = MutableStateFlow<SyncState>(SyncState.NotSynced(SyncError.NotStarted()))
    val transactionsSyncStateFlow: StateFlow<SyncState>
        get() = _transactionsSyncStateFlow

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        coroutineScope.launch {
            // TON generates a new block on each shardchain and the masterchain approximately every 5 seconds
            tickerFlow(5.seconds).collect {
                sync()
            }
        }
//        connectionManager.listener = object: ConnectionManager.Listener {
//            override fun onConnectionChange() {
//                sync()
//            }
//        }
    }

    private fun sync() {
//        if (!connectionManager.isConnected) {
        if (false) {
            coroutineScope.launch {
                _balanceSyncStateFlow.update {
                    SyncState.NotSynced(SyncError.NoNetworkConnection())
                }
                _transactionsSyncStateFlow.update {
                    SyncState.NotSynced(SyncError.NoNetworkConnection())
                }
            }
        } else {
            coroutineScope.launch {
                balanceManager.sync().collect { syncState ->
//                    if (syncState !is SyncState.Syncing || _balanceSyncStateFlow.value !is SyncState.Synced) {
                        _balanceSyncStateFlow.update { syncState }
//                    }
                }
            }

            coroutineScope.launch {
                transactionManager.sync().collect { syncState ->
//                    if (syncState !is SyncState.Syncing || _transactionsSyncStateFlow.value !is SyncState.Synced) {
                        _transactionsSyncStateFlow.update { syncState }
//                    }
                }
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
//        connectionManager.listener = null
//        connectionManager.stop()
    }
}
