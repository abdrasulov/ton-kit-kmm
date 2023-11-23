package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.ton.block.AddrStd

class TonKit(
    private val transactionManager: TransactionManager,
    private val balanceManager: BalanceManager,
    val receiveAddress: String,
    private val syncer: Syncer,
    private val transactionSender: TransactionSender?
) {
    val newTransactionsFlow by transactionManager::newTransactionsFlow
    val balanceFlow by balanceManager::balanceFlow
    val balanceSyncStateFlow by syncer::balanceSyncStateFlow
    val transactionsSyncStateFlow by syncer::transactionsSyncStateFlow

    val balance: String?
        get() = balanceFlow.value
    val balanceSyncState: SyncState
        get() = balanceSyncStateFlow.value
    val transactionsSyncState: SyncState
        get() = transactionsSyncStateFlow.value

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        syncer.start()
    }

    fun stop() {
        coroutineScope.cancel()
        syncer.stop()
    }

    suspend fun send(recipient: String, amount: String) {
        checkNotNull(transactionSender) {
            "Sending is not available for watch account"
        }

        transactionSender.send(recipient, amount)
    }

    suspend fun transactions(fromTransactionHash: String?, type: TransactionType?, limit: Long): List<TonTransaction> {
        return transactionManager.transactions(fromTransactionHash, type, limit)
    }

    companion object {
        fun validate(address: String) {
            AddrStd.parse(address)
        }
    }
}
