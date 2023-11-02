package io.horizontalsystems.tonkit

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.block.AddrStd
import org.ton.block.AddrVar
import org.ton.block.MsgAddressInt
import org.ton.contract.wallet.WalletV4R2Contract
import org.ton.mnemonic.Mnemonic

class TonKit(words: List<String>, passphrase: String) {
    private val wallet: WalletV4R2Contract?
    private val adnl: TonApiAdnl?
    val receiveAddress: String

    init {
        val seed = Mnemonic.toSeed(words, passphrase)
        val privateKey = PrivateKeyEd25519(seed)
        val publicKey = privateKey.publicKey()
        wallet = WalletV4R2Contract(0, publicKey)
        val address = wallet.address
//        val address = AddrStd.parse("UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM")
        receiveAddress = MsgAddressInt.toString(wallet.address, bounceable = false)

        adnl = when (address) {
            is AddrStd -> TonApiAdnl(address)
            is AddrVar -> null
        }
    }
}
