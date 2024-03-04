package io.horizontalsystems.tonkit

import org.ton.block.MsgAddressInt

class TonAddress(private val address: MsgAddressInt) {

    fun getNonBounceable(): String {
        return MsgAddressInt.toString(address, userFriendly = true, bounceable = false)
    }

    fun toRaw() = MsgAddressInt.toString(address, userFriendly = false)

    companion object {
        fun parse(addrStr: String) = TonAddress(MsgAddressInt.parse(addrStr))
    }

}
