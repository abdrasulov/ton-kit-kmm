package io.horizontalsystems.tonkit

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.bigint.plus
import org.ton.bitstring.BitString
import org.ton.block.AccountInfo
import org.ton.block.AddrStd
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
        val txAux = info.transaction.value.r1.value

        val outIntMsgInfo = txAux.outMsgs.singleOrNull()?.second?.value?.info as? IntMsgInfo
        val inIntMsgInfo = txAux.inMsg.value?.value?.info as? IntMsgInfo
        val intMsgInfo = outIntMsgInfo ?: inIntMsgInfo

        val transactionType = when {
            outIntMsgInfo != null -> TransactionType.Outgoing
            inIntMsgInfo != null -> TransactionType.Incoming
            else -> TransactionType.Unknown
        }

        var fee = info.transaction.value.totalFees.coins.amount.value
        if (outIntMsgInfo != null) {
            fee += outIntMsgInfo.fwd_fee.amount.value + outIntMsgInfo.ihr_fee.amount.value
        }

        return TonTransaction(
            hash = info.id.hash.toHex(),
            lt = info.id.lt,
            timestamp = info.transaction.value.now.toLong(),
            value_ = intMsgInfo?.value?.coins?.amount?.value?.toString(10),
            fee = fee.toString(10),
            type = transactionType.name,
            src = intMsgInfo?.src?.let { MsgAddressInt.toString(it, bounceable = false) },
            dest = intMsgInfo?.dest?.let { MsgAddressInt.toString(it, bounceable = false) },
        )
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
