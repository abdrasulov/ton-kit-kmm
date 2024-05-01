package io.horizontalsystems.tonkit

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.bigint.BigInt
import org.ton.block.AccountInfo
import org.ton.block.Coins
import org.ton.block.MsgAddressInt
import org.ton.contract.wallet.MessageData
import org.ton.contract.wallet.WalletTransfer
import org.ton.contract.wallet.WalletV4R2Contract

class TransactionSender(
    private val adnl: TonApiAdnl,
    private val privateKey: PrivateKeyEd25519,
) {
    suspend fun send(recipient: String, amount: String, memo: String?) {
        val wallet = WalletV4R2Contract(adnl.getLiteClient(), adnl.addrStd)

        checkNotNull(wallet)

        wallet.transfer(privateKey, WalletTransfer {
            destination = MsgAddressInt.parseUserFriendly(recipient)
            coins = Coins.ofNano(BigInt(amount))
            bounceable = false
            memo?.let {
                messageData = MessageData.text(it)
            }
        })
    }

    suspend fun estimateFee(): String {
        val accountInfo = try {
            val fullAccountState = adnl.getFullAccountState()
            fullAccountState.account.value as? AccountInfo
        } catch (e: Throwable) {
            null
        }

        val fee = when {
            accountInfo == null -> 0
            accountInfo.isActive -> 7000000
            accountInfo.isUninit -> 15000000
            else -> 0
        }

        return fee.toString()
    }
}
