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
    val receiveAddress: String
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

    suspend fun transactions(fromTransactionHash: String?, limit: Long): List<TonTransaction> {
        return transactionManager.transactions(fromTransactionHash, limit)
    }
}

class TonKitFactory(private val dbDriverFactory: DriverFactory) {
    fun create(words: List<String>, passphrase: String): TonKit {
        return create(Mnemonic.toSeed(words, passphrase))
    }

    fun create(seed: ByteArray): TonKit {
        val privateKey = PrivateKeyEd25519(seed)
        val publicKey = privateKey.publicKey()
        val wallet = WalletV4R2Contract(0, publicKey)
        val address = wallet.address
        val receiveAddress = MsgAddressInt.toString(address, bounceable = false)

        val adnl = when (address) {
            is AddrStd -> TonApiAdnl(address)
            is AddrVar -> null
        }

        checkNotNull(adnl)

        val database = Database(dbDriverFactory)
        val transactionManager = TransactionManager(adnl, TransactionStorage(database))
        val balanceManager = BalanceManager(adnl)

        return TonKit(transactionManager, balanceManager, receiveAddress)
    }

    fun createWatch(address: String): TonKit {
        val addrStd = AddrStd.parse(address)
        val receiveAddress = MsgAddressInt.toString(addrStd, bounceable = false)
        val adnl = TonApiAdnl(addrStd)

        val database = Database(dbDriverFactory)
        val transactionManager = TransactionManager(adnl, TransactionStorage(database))
        val balanceManager = BalanceManager(adnl)

        return TonKit(transactionManager, balanceManager, receiveAddress)
    }
}
