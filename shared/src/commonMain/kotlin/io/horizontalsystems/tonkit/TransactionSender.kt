package io.horizontalsystems.tonkit

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.block.AccountInfo
import org.ton.contract.wallet.WalletV4R2Contract

class TransactionSender(
    private val adnl: TonApiAdnl,
    private val privateKey: PrivateKeyEd25519,
) {
    suspend fun send(dest: String, amount: String) {
        val liteApi = adnl.getLiteApi()
        checkNotNull(liteApi)

        val fullAccountStateOrNull = adnl.getFullAccountStateOrNull()
        val wallet = (fullAccountStateOrNull?.account?.value as? AccountInfo)?.let {
            WalletV4R2Contract(it)
        }

        checkNotNull(wallet)
        val decimals = 9

//        wallet.transfer(liteApi, privateKey, WalletTransfer {
//            destination = MsgAddressInt.parseUserFriendly(dest)
//            coins = Coins.ofNano(amount.movePointRight(decimals).toBigInteger())
//            bounceable = false
//        })
    }
}
