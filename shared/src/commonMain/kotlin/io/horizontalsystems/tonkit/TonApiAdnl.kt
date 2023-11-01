package io.horizontalsystems.tonkit

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.block.AccountInfo
import org.ton.block.AddrStd
import org.ton.lite.client.LiteClient

class TonApiAdnl(
//    private val words: List<String>,
//    private val passphrase: String,
) {
    private val httpClient = HttpClient()

    var balance: String? = null
    var balanceUpdatedFlow = MutableSharedFlow<Unit>()
    val address = "UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM"
    private val addrStd = AddrStd.parse(address)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private lateinit var liteClient: LiteClient

    init {
        coroutineScope.launch(Dispatchers.Default) {
            val config = json.decodeFromString(
                LiteClientConfigGlobal.serializer(),
                httpClient.get("https://ton.org/global.config.json").bodyAsText()
            )
            liteClient = LiteClient(Dispatchers.Default, config)

            val fullAccountState = liteClient.getAccountState(addrStd)
            val account = fullAccountState.account.value
            balance = if (account is AccountInfo) {
                account.storage.balance.coins.toString()
            } else {
                null
            }
            balanceUpdatedFlow.emit(Unit)
        }
    }

    fun start() {
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {

    }

//    suspend fun transactions(limit: Int, transactionHash: String?, lt: Long?): List<TonTransaction> {
//        val transactionId = when {
//            transactionHash != null && lt != null -> TransactionId(BitString(transactionHash), lt)
//            else -> liteClient.getAccountState(addrStd).lastTransactionId
//        } ?: return listOf()
//
//        val transactions = liteClient.getTransactions(addrStd, transactionId, limit)
//        return transactions.map { createTonTransaction(it) }
//    }

//    private fun createTonTransaction(info: TransactionInfo): TonTransaction {
//        val inMsg = info.transaction.value.r1.value.inMsg.value?.value?.info
//        val outMsgs = info.transaction.value.r1.value.outMsgs
//
//        val value: BigDecimal?
//        val transactionType: TransactionType
//        when {
//            outMsgs.count() == 1 -> {
//                value = getValue(outMsgs.first().second.value.info)
//                transactionType = TransactionType.Outgoing
//            }
//
//            inMsg != null -> {
//                value = getValue(inMsg)
//                transactionType = TransactionType.Incoming
//            }
//
//            else -> {
//                value = null
//                transactionType = TransactionType.Outgoing
//            }
//        }
//
//        return TonTransaction(
//            hash = info.id.hash.toHex(),
//            lt = info.id.lt,
//            timestamp = info.transaction.value.now.toLong(),
//            value = value ?: BigDecimal.ZERO,
//            type = transactionType
//        )
//    }

//    private fun getValue(inMsg: CommonMsgInfo) = when (inMsg) {
//        is IntMsgInfo -> BigDecimal(inMsg.value.coins.toString())
//        is ExtInMsgInfo -> null
//        is ExtOutMsgInfo -> null
//    }
}

//data class TonTransaction(
//    val hash: String,
//    val lt: Long,
//    val timestamp: Long,
//    val value: BigDecimal,
//    val type: TransactionType
//)

//enum class TransactionType {
//    Incoming, Outgoing
//}

