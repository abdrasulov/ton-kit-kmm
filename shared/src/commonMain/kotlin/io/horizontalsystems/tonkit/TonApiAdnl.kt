package io.horizontalsystems.tonkit

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.bitstring.BitString
import org.ton.block.AccountInfo
import org.ton.block.AddrStd
import org.ton.block.CommonMsgInfo
import org.ton.block.ExtInMsgInfo
import org.ton.block.ExtOutMsgInfo
import org.ton.block.IntMsgInfo
import org.ton.block.MsgAddressInt
import org.ton.lite.api.LiteApi
import org.ton.lite.api.exception.LiteServerUnknownException
import org.ton.lite.client.LiteClient
import org.ton.lite.client.internal.TransactionId
import org.ton.lite.client.internal.TransactionInfo

class TonApiAdnl(private val addrStd: AddrStd) {
    private val httpClient = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val liteClient: LiteClient

    init {
        val config = json.decodeFromString(
            LiteClientConfigGlobal.serializer(),
            runBlocking {
                httpClient.get("https://ton.org/global.config.json").bodyAsText()
            }
        )
        liteClient = LiteClient(Dispatchers.Default, config)
    }

    suspend fun getBalance(): String? {
        val fullAccountState = getFullAccountStateOrNull() ?: return null
        val account = fullAccountState.account.value
        return if (account is AccountInfo) {
            account.storage.balance.coins.amount.value.toString(10)
        } else {
            null
        }
    }

    suspend fun transactions(transactionHash: String?, lt: Long?, limit: Int): List<TonTransaction> {
        val transactionId = when {
            transactionHash != null && lt != null -> TransactionId(BitString(transactionHash), lt)
            else -> getFullAccountStateOrNull()?.lastTransactionId
        } ?: return listOf()

        try {
            val transactions = liteClient.getTransactions(addrStd, transactionId, limit)
            return transactions.map { createTonTransaction(it) } ?: listOf()
        } catch (e: LiteServerUnknownException) {
            return listOf()
        }
    }

    suspend fun getFullAccountStateOrNull() = try {
        liteClient?.getAccountState(addrStd)
    } catch (e: Exception) {
        null
    }

    private fun createTonTransaction(info: TransactionInfo): TonTransaction {
        val inMsg = info.transaction.value.r1.value.inMsg.value?.value?.info
        val outMsgs = info.transaction.value.r1.value.outMsgs

        val transactionType: TransactionType
        val msgInfo: CommonMsgInfo?
        when {
            outMsgs.count() == 1 -> {
                val outMsg = outMsgs.first().second.value.info
                msgInfo = outMsg
                transactionType = TransactionType.Outgoing
            }

            inMsg != null -> {
                msgInfo = inMsg
                transactionType = TransactionType.Incoming
            }

            else -> {
                msgInfo = null
                transactionType = TransactionType.Outgoing
            }
        }

        return TonTransaction(
            hash = info.id.hash.toHex(),
            lt = info.id.lt,
            timestamp = info.transaction.value.now.toLong(),
            value_ = getValue(msgInfo),
            type = transactionType.name,
            src = getSrc(msgInfo),
            dest = getDest(msgInfo),
        )
    }

    private fun getValue(msgInfo: CommonMsgInfo?) = when (msgInfo) {
        is IntMsgInfo -> msgInfo.value.coins.amount.value.toString(10)
        is ExtInMsgInfo -> null
        is ExtOutMsgInfo -> null
        null -> null
    }

    private fun getSrc(msgInfo: CommonMsgInfo?) = when (msgInfo) {
        is IntMsgInfo -> MsgAddressInt.toString(msgInfo.src, bounceable = false)
        is ExtInMsgInfo -> null
        is ExtOutMsgInfo -> null
        null -> null
    }

    private fun getDest(msgInfo: CommonMsgInfo?) = when (msgInfo) {
        is IntMsgInfo -> MsgAddressInt.toString(msgInfo.dest, bounceable = false)
        is ExtInMsgInfo -> null
        is ExtOutMsgInfo -> null
        null -> null
    }

    suspend fun getLatestTransactionHash(): String? {
        return getFullAccountStateOrNull()?.lastTransactionId?.hash?.toHex()
    }

    fun getLiteApi(): LiteApi? {
        return liteClient?.liteApi
    }
}
