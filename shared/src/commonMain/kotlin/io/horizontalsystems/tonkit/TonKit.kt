package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.block.AddrStd
import org.ton.block.AddrVar
import org.ton.block.MsgAddressInt
import org.ton.contract.wallet.WalletV4R2Contract
import org.ton.mnemonic.Mnemonic

class TonKit(
    private val transactionManager: TransactionManager,
    private val balanceManager: BalanceManager,
    val receiveAddress: String,
) {
    val newTransactionsFlow by transactionManager::newTransactionsFlow
    val balanceFlow by balanceManager::balanceFlow

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        coroutineScope.launch {
            balanceManager.sync()
        }

        coroutineScope.launch {
            transactionManager.sync()
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    suspend fun transactions(fromTransactionHash: String?, limit: Int?): List<TonTransaction> {
        return transactionManager.transactions(fromTransactionHash, limit)
    }
}

object TonKitFactory {
    fun create(words: List<String>, passphrase: String): TonKit {
        val seed = Mnemonic.toSeed(words, passphrase)
        val privateKey = PrivateKeyEd25519(seed)
        val publicKey = privateKey.publicKey()
        val wallet = WalletV4R2Contract(0, publicKey)
//        val address = wallet.address
        val address: MsgAddressInt = AddrStd.parse("UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM")
        val receiveAddress = MsgAddressInt.toString(address, bounceable = false)

        val adnl = when (address) {
            is AddrStd -> TonApiAdnl(address)
            is AddrVar -> null
        }

        checkNotNull(adnl)

        val transactionManager = TransactionManager(adnl, TransactionStorage())
        val balanceManager = BalanceManager(adnl)

        return TonKit(transactionManager, balanceManager, receiveAddress)
    }
}
