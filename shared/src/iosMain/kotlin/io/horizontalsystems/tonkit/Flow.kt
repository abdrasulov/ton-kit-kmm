package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface Cancellable {
    fun cancel()
}

fun <T> Flow<T>.collect(onEach: (T) -> Unit, onCompletion: (cause: Throwable?) -> Unit): Cancellable {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    scope.launch {
        try {
            collect {
                onEach(it)
            }

            onCompletion(null)
        } catch (e: Throwable) {
            onCompletion(e)
        }
    }

    return object : Cancellable {
        override fun cancel() {
            scope.cancel()
        }
    }
}

fun TonKit.balancePublisher(onEach: (String) -> Unit, onCompletion: (Throwable?) -> Unit): Cancellable =
    balanceFlow.collect(onEach, onCompletion)

fun TonKit.balanceSyncStatePublisher(onEach: (SyncState) -> Unit, onCompletion: (Throwable?) -> Unit): Cancellable =
    balanceSyncStateFlow.collect(onEach, onCompletion)

fun TonKit.transactionsSyncStatePublisher(onEach: (SyncState) -> Unit, onCompletion: (Throwable?) -> Unit): Cancellable =
    transactionsSyncStateFlow.collect(onEach, onCompletion)

fun TonKit.newTransactionsPublisher(onEach: (List<TonTransactionWithTransfers>) -> Unit, onCompletion: (Throwable?) -> Unit): Cancellable =
    newTransactionsFlow.collect(onEach, onCompletion)
