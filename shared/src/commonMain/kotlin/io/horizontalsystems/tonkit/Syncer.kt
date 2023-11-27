package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class Syncer(
    private val transactionManager: TransactionManager,
    private val balanceManager: BalanceManager,
    private val connectionManager: ConnectionManager,
) {
    private val _balanceSyncStateFlow = MutableStateFlow<SyncState>(SyncState.NotSynced(SyncError.NotStarted()))
    val balanceSyncStateFlow: StateFlow<SyncState>
        get() = _balanceSyncStateFlow.asStateFlow()

    private val _transactionsSyncStateFlow = MutableStateFlow<SyncState>(SyncState.NotSynced(SyncError.NotStarted()))
    val transactionsSyncStateFlow: StateFlow<SyncState>
        get() = _transactionsSyncStateFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var syncerJob: Job? = null
    private var balanceSyncerJob: Job? = null
    private var transactionSyncerJob: Job? = null

    fun start() {
        coroutineScope.launch {
            connectionManager.isConnectedFlow.collect { isConnected ->
                if (isConnected) {
                    runSyncer()
                } else {
                    cancelSyncer()
                }
            }
        }
        connectionManager.start()
    }

    private suspend fun cancelSyncer() {
        syncerJob?.cancelAndJoin()
        balanceSyncerJob?.cancelAndJoin()
        transactionSyncerJob?.cancelAndJoin()

        _balanceSyncStateFlow.update {
            SyncState.NotSynced(SyncError.NoNetworkConnection())
        }
        _transactionsSyncStateFlow.update {
            SyncState.NotSynced(SyncError.NoNetworkConnection())
        }
    }

    private fun runSyncer() {
        syncerJob = coroutineScope.launch {
            // TON generates a new block on each shardchain and the masterchain approximately every 5 seconds
            tickerFlow(5.seconds).collect {
                sync()
            }
        }
    }

    private fun sync() {
        balanceSyncerJob = coroutineScope.launch {
            balanceManager.sync().collect { syncState ->
                if (_balanceSyncStateFlow.value !is SyncState.Synced) {
                    _balanceSyncStateFlow.update { syncState }
                } else if (syncState is SyncState.NotSynced) {
                    _balanceSyncStateFlow.update { syncState }
                }
            }
        }

        transactionSyncerJob = coroutineScope.launch {
            transactionManager.sync().collect { syncState ->
                if (_transactionsSyncStateFlow.value !is SyncState.Synced) {
                    _transactionsSyncStateFlow.update { syncState }
                } else if (syncState is SyncState.NotSynced) {
                    _transactionsSyncStateFlow.update { syncState }
                }
            }
        }
    }

    fun stop() {
        connectionManager.stop()
        coroutineScope.cancel()
        _balanceSyncStateFlow.update { SyncState.NotSynced(SyncError.NotStarted()) }
        _transactionsSyncStateFlow.update { SyncState.NotSynced(SyncError.NotStarted()) }
    }
}
