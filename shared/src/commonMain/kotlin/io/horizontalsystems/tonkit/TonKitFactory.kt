package io.horizontalsystems.tonkit

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.block.AddrStd
import org.ton.block.AddrVar
import org.ton.block.MsgAddressInt
import org.ton.contract.wallet.WalletV4R2Contract
import org.ton.mnemonic.Mnemonic

class TonKitFactory(private val driverFactory: DriverFactory) {
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

        return createInternal(adnl, receiveAddress, privateKey)
    }

    fun createWatch(address: String): TonKit {
        val addrStd = AddrStd.parse(address)
        val receiveAddress = MsgAddressInt.toString(addrStd, bounceable = false)
        val adnl = TonApiAdnl(addrStd)

        return createInternal(adnl, receiveAddress, null)
    }

    private fun createInternal(
        adnl: TonApiAdnl,
        receiveAddress: String,
        privateKey: PrivateKeyEd25519?
    ): TonKit {
        val database = Database(driverFactory)

        val transactionStorage = TransactionStorage(database)
        val transactionManager = TransactionManager(adnl, transactionStorage)
        val balanceManager = BalanceManager(adnl)

        val transactionSender = if (privateKey != null) {
            TransactionSender(adnl, privateKey)
        } else {
            null
        }

        val syncer = Syncer(transactionManager, balanceManager)
        return TonKit(transactionManager, balanceManager, receiveAddress, syncer, transactionSender)
    }
}
