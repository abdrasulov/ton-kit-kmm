package io.horizontalsystems.tonkit

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.bigint.BigInt
import org.ton.block.AccountInfo
import org.ton.block.Coins
import org.ton.block.MsgAddressInt
import org.ton.contract.wallet.WalletTransfer
import org.ton.contract.wallet.WalletV4R2Contract

class TransactionSender(
    private val adnl: TonApiAdnl,
    private val privateKey: PrivateKeyEd25519,
) {
    suspend fun send(recipient: String, amount: String) {
        val liteApi = adnl.getLiteApi()

        val fullAccountState = adnl.getFullAccountState()
        val wallet = (fullAccountState.account.value as? AccountInfo)?.let {
            WalletV4R2Contract(it)
        }

        checkNotNull(wallet)

        wallet.transfer(liteApi, privateKey, WalletTransfer {
            destination = MsgAddressInt.parseUserFriendly(recipient)
            coins = Coins.ofNano(BigInt(amount))
            bounceable = false
        })
    }

    suspend fun estimateFee(): String {
        val fullAccountState = adnl.getFullAccountState()
        val accountInfo = fullAccountState.account.value as? AccountInfo

        val fee = when {
            accountInfo == null -> 0
            accountInfo.isActive -> 7000000
            accountInfo.isUninit -> 15000000
            else -> 0
        }

        return fee.toString()
    }
}
