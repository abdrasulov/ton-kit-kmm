package io.horizontalsystems.tonkit.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.tonkit.TonKit
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
    private val passphrase = ""
    private val tonKit = TonKit(words, passphrase)

    val address = tonKit.receiveAddress

    private var balance: String? = null

    var uiState by mutableStateOf(
        MainUiState(
            balance = balance
        )
    )
        private set

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
