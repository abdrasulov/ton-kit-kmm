package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.jvm.Synchronized

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

    @OptIn(ObsoleteCoroutinesApi::class)
    fun start() {
//        coroutineScope.launch {
//            // TON generates a new block on each shardchain and the masterchain approximately every 5 seconds
//            ticker(5 * 1000, 0).receiveAsFlow().collect {
//                sync()
//            }
//        }
//        connectionManager.listener = object: ConnectionManager.Listener {
//            override fun onConnectionChange() {
//                sync()
//            }
//        }
        sync()
    }

    @Synchronized
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
                    _balanceSyncStateFlow.update { syncState }
                }
            }

            coroutineScope.launch {
                transactionManager.sync().collect { syncState ->
                    _transactionsSyncStateFlow.update { syncState }
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
