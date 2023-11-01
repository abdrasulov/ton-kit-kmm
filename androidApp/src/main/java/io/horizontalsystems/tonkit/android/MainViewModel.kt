package io.horizontalsystems.tonkit.android

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.tonkit.TonApiAdnl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var balance: String? = null

    var uiState by mutableStateOf(
        MainUiState(
            balance = balance
        )
    )
        private set

    init {
        val tonApiAdnl = TonApiAdnl()

        viewModelScope.launch(Dispatchers.Default) {
            tonApiAdnl.balanceUpdatedFlow.collect {
                Log.e("AAA", "updateBalance")
                updateBalance(tonApiAdnl.balance)
            }
        }
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
