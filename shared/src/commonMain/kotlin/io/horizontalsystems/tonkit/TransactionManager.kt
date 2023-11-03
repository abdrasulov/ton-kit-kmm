package io.horizontalsystems.tonkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TransactionManager(private val adnl: TonApiAdnl, private val storage: TransactionStorage) {

    private val _newTransactionsFlow = MutableSharedFlow<List<TonTransaction>>()
    val newTransactionsFlow: Flow<List<TonTransaction>>
        get() = _newTransactionsFlow.asSharedFlow()

    suspend fun sync() {
        val localLastTransactionHash = storage.getLastTransaction()?.hash
        val remoteLastTransactionHash = adnl.getLastTransactionId()

        if (remoteLastTransactionHash != localLastTransactionHash) {
            syncUntil(localLastTransactionHash)
        }
    }

    private suspend fun syncUntil(until: String?) {
        val limit = 10
        var fromTransactionHash: String? = null
        var fromTransactionLt: Long? = null
        while (true) {
            val transactions = adnl.transactions(fromTransactionHash, fromTransactionLt, limit)
            val newTransactions = when (until) {
                null -> transactions
                else -> transactions.subList(0, transactions.indexOfFirst { it.hash == until })
            }
            storage.add(newTransactions)
            _newTransactionsFlow.emit(newTransactions)

            if (newTransactions.size < limit) break

            val last = transactions.last()
            fromTransactionHash = last.hash
            fromTransactionLt = last.lt
        }
    }

    suspend fun transactions(fromTransactionHash: String?, limit: Int?): List<TonTransaction> {
        return storage.getTransactions(fromTransactionHash, limit)
    }

}

