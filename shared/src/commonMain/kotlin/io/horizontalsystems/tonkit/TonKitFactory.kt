package io.horizontalsystems.tonkit

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.block.AddrStd
import org.ton.block.MsgAddressInt
import org.ton.contract.wallet.WalletV4R2Contract
import org.ton.mnemonic.Mnemonic

class TonKitFactory(private val driverFactory: DriverFactory, private val connectionManager: ConnectionManager) {
    fun create(words: List<String>, passphrase: String, walletId: String): TonKit {
        return create(Mnemonic.toSeed(words, passphrase), walletId)
    }

    fun create(seed: ByteArray, walletId: String): TonKit {
        val privateKey = PrivateKeyEd25519(seed)
        val address = WalletV4R2Contract.address(privateKey, 0)
        val receiveAddress = MsgAddressInt.toString(address, bounceable = false)

        val adnl = TonApiAdnl(address)

        checkNotNull(adnl)

        return createInternal(adnl, receiveAddress, privateKey, walletId)
    }

    fun createWatch(address: String, walletId: String): TonKit {
        val addrStd = AddrStd.parse(address)
        val receiveAddress = MsgAddressInt.toString(addrStd, bounceable = false)
        val adnl = TonApiAdnl(addrStd)

        return createInternal(adnl, receiveAddress, null, walletId)
    }

    private fun createInternal(
        adnl: TonApiAdnl,
        receiveAddress: String,
        privateKey: PrivateKeyEd25519?,
        walletId: String
    ): TonKit {
        val database = Database(driverFactory, "ton-$walletId.db")

        val transactionManager = TransactionManager(adnl, TransactionStorage(
            database.transactionQuery,
            database.transferQuery
        ))
        val balanceManager = BalanceManager(adnl, BalanceStorage(database.balanceQuery))

        val transactionSender = if (privateKey != null) {
            TransactionSender(adnl, privateKey)
        } else {
            null
        }

        val syncer = Syncer(transactionManager, balanceManager, connectionManager)
        return TonKit(transactionManager, balanceManager, receiveAddress, syncer, transactionSender)
    }
}
