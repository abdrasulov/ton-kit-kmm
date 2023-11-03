package io.horizontalsystems.tonkit

class BalanceManager(private val adnl: TonApiAdnl) {
    suspend fun getBalance(): String? {
        return adnl.getBalance()
    }
}
