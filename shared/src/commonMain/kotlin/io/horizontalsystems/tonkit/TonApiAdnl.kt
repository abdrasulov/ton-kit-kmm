package io.horizontalsystems.tonkit

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
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
import org.ton.lite.client.internal.FullAccountState
import org.ton.lite.client.internal.TransactionId
import org.ton.lite.client.internal.TransactionInfo

class TonApiAdnl(private val addrStd: AddrStd) {
    private val httpClient = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var liteClient: LiteClient? = null

    suspend fun getBalance(): String? {
        val fullAccountState = getFullAccountState()
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
            else -> getFullAccountState().lastTransactionId
        } ?: return listOf()

        val transactions = getLiteClient().getTransactionsWithAttempts(addrStd, transactionId, limit)
        return transactions.map { createTonTransaction(it) }
    }

    private suspend fun LiteClient.getTransactionsWithAttempts(
        accountAddress: AddrStd,
        fromTransactionId: TransactionId,
        count: Int,
    ): List<TransactionInfo> {
        val maxAttempts = 3
        var attemptNumber = 1

        while (true) {
            val result = try {
                getTransactions(accountAddress, fromTransactionId, count)
            } catch (e: LiteServerUnknownException) {
                listOf()
            }

            if (result.size == count || attemptNumber++ == maxAttempts) return result
        }
    }

    suspend fun getFullAccountState(): FullAccountState {
        return getLiteClient().getAccountState(addrStd)
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
            fee = info.transaction.value.totalFees.coins.amount.value.toString(10),
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
        return getFullAccountState().lastTransactionId?.hash?.toHex()
    }

    suspend fun getLiteApi(): LiteApi {
        return getLiteClient().liteApi
    }

    private suspend fun getLiteClient(): LiteClient {
        liteClient?.let {
            return it
        }

        val client = createLiteClient()
        liteClient = client

        return client
    }

    private suspend fun createLiteClient(): LiteClient {
        val config = json.decodeFromString(
            LiteClientConfigGlobal.serializer(),
            httpClient.get("https://ton.org/global.config.json").bodyAsText()
        )
        return LiteClient(Dispatchers.Default, config)
    }
}
