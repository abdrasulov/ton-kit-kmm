package io.horizontalsystems.tonkit.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.tonkit.DriverFactory
import io.horizontalsystems.tonkit.TonKitFactory
import io.horizontalsystems.tonkit.TonTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
    private val passphrase = ""
    private val watchAddress = "UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM"

//    private val tonKit = TonKitFactory.create(words, passphrase)
    private val tonKit = TonKitFactory(DriverFactory(getApplication())).createWatch(watchAddress)

    val address = tonKit.receiveAddress

    private var balance: String? = null

    var uiState by mutableStateOf(
        MainUiState(
            balance = balance
        )
    )
        private set

    var transactionList: List<TonTransaction>? by mutableStateOf(null)
        private set

    init {
        tonKit.start()
        viewModelScope.launch {
            tonKit.balanceFlow.collect {
                updateBalance(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            tonKit.newTransactionsFlow.collect {
                transactionList = tonKit.transactions(null, 10)
            }
        }
    }

    override fun onCleared() {
        tonKit.stop()
    }

    private fun updateBalance(balance: String?) {
        this.balance = balance

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = MainUiState(
                balance = balance
            )
        }
    }
}

data class MainUiState(
    val balance: String?,
)
