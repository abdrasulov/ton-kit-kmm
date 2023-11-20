package io.horizontalsystems.tonkit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

class BalanceManager(private val adnl: TonApiAdnl) {

    private val _balanceFlow = MutableStateFlow<String?>(null)
    val balanceFlow: StateFlow<String?>
        get() = _balanceFlow.asStateFlow()

    suspend fun sync() = flow {
        emit(SyncState.Syncing())

        try {
            _balanceFlow.update { adnl.getBalance() }
            emit(SyncState.Synced())
        } catch (t: Throwable) {
            emit(SyncState.NotSynced(t))
        }
    }
}
