package io.horizontalsystems.tonkit.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.tonkit.ConnectionManager
import io.horizontalsystems.tonkit.DriverFactory
import io.horizontalsystems.tonkit.SyncState
import io.horizontalsystems.tonkit.TonKitFactory
import io.horizontalsystems.tonkit.TonTransactionWithTransfers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
    private val passphrase = ""
    private val watchAddress = "UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM"

    val tonKitFactory = TonKitFactory(DriverFactory(getApplication()), ConnectionManager(getApplication()))
//    private val tonKit = tonKitFactory.create(words, passphrase, words.first())
    private val tonKit = tonKitFactory.createWatch(watchAddress, "watch")

    val address = tonKit.receiveAddress

    private var balance = tonKit.balance
    private var syncState = tonKit.balanceSyncStateFlow.value
    private var txSyncState = tonKit.transactionsSyncStateFlow.value
    private var transactionList: List<TonTransactionWithTransfers>? = null

    var uiState by mutableStateOf(
        MainUiState(
            balance = balance,
            syncState = syncState,
            txSyncState = txSyncState,
            transactionList = transactionList
        )
    )
        private set

    var fee: String? by mutableStateOf(null)
        private set

    init {
        tonKit.start()
        viewModelScope.launch {
            tonKit.balanceFlow.collect {
                updateBalance(it)
            }
        }
        viewModelScope.launch {
            tonKit.balanceSyncStateFlow.collect {
                updateSyncState(it)
            }
        }
        viewModelScope.launch {
            tonKit.transactionsSyncStateFlow.collect {
                updateTxSyncState(it)
            }
        }

        loadNextTransactionsPage()
        refreshFee()

        viewModelScope.launch(Dispatchers.IO) {
            tonKit.newTransactionsFlow.collect {
                transactionList = null
                loadNextTransactionsPage()
                refreshFee()
            }
        }
    }

    private fun refreshFee() {
        viewModelScope.launch(Dispatchers.IO) {
            val estimateFee = try {
                tonKit.estimateFee()
            } catch (e: Throwable) {
                e.message
            }

            viewModelScope.launch {
                fee = estimateFee
            }
        }
    }

    fun loadNextTransactionsPage() {
        viewModelScope.launch(Dispatchers.IO) {
            var list = transactionList ?: listOf()
            list += tonKit.transactions(transactionList?.lastOrNull()?.hash, null, null, 10)

            transactionList = list

            emitState()
        }
    }

    private fun updateSyncState(syncState: SyncState) {
        this.syncState = syncState

        emitState()
    }

    private fun updateTxSyncState(syncState: SyncState) {
        txSyncState = syncState

        emitState()
    }

    override fun onCleared() {
        tonKit.stop()
    }

    private fun updateBalance(balance: String) {
        this.balance = balance

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = MainUiState(
                balance = balance,
                syncState = syncState,
                txSyncState = txSyncState,
                transactionList = transactionList
            )
        }
    }

    private var sendRecipient: String? = null
    private var sendAmount: BigDecimal? = null

    fun setAmount(amount: String) {
        sendAmount = amount.toBigDecimal()
    }

    fun setRecipient(recipient: String) {
        sendRecipient = recipient
    }

    var sendResult by mutableStateOf("")
        private set

    fun send() {
        viewModelScope.launch(Dispatchers.Default) {
            sendResult = ""
            try {
                val sendRecipient = sendRecipient
                val sendAmount = sendAmount?.movePointRight(9)?.toBigInteger()
                checkNotNull(sendRecipient)
                checkNotNull(sendAmount)

                sendResult = "Sending..."

                tonKit.send(sendRecipient, sendAmount.toString(), "Test transaction")

                sendResult = "Send success"
            } catch (t: Throwable) {
                sendResult = "Send error: $t"
            }
        }
    }

    fun start() {
        tonKit.start()
    }

    fun stop() {
        tonKit.stop()
    }
}

data class MainUiState(
    val balance: String?,
    val syncState: SyncState,
    val txSyncState: SyncState,
    val transactionList: List<TonTransactionWithTransfers>?,
)

fun SyncState.toStr() = when (this) {
    is SyncState.NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
    is SyncState.Synced -> "Synced"
    is SyncState.Syncing -> "Syncing"
}