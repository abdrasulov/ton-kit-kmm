package io.horizontalsystems.tonkit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

class BalanceManager(private val adnl: TonApiAdnl, private val balanceStorage: BalanceStorage) {

    private val _balanceFlow = MutableStateFlow(balanceStorage.getBalance())
    val balanceFlow: StateFlow<String>
        get() = _balanceFlow.asStateFlow()

    suspend fun sync() = flow {
        emit(SyncState.Syncing())

        try {
            adnl.getBalance()?.let { balance ->
                balanceStorage.setBalance(balance)
                _balanceFlow.update { balance }
            }
            emit(SyncState.Synced())
        } catch (t: Throwable) {
            emit(SyncState.NotSynced(t))
        }
    }
}
