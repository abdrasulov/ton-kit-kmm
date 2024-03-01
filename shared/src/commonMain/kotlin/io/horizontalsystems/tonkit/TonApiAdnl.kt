package io.horizontalsystems.tonkit

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.bigint.BigInt
import org.ton.bigint.plus
import org.ton.bigint.toBigInt
import org.ton.bitstring.BitString
import org.ton.block.AccountInfo
import org.ton.block.AddrStd
import org.ton.block.IntMsgInfo
import org.ton.block.Message
import org.ton.cell.Cell
import org.ton.contract.wallet.MessageText
import org.ton.lite.api.exception.LiteServerUnknownException
import org.ton.lite.client.LiteClient
import org.ton.lite.client.internal.FullAccountState
import org.ton.lite.client.internal.TransactionId
import org.ton.lite.client.internal.TransactionInfo
import org.ton.tlb.CellRef

class TonApiAdnl(val addrStd: AddrStd) {
    private val httpClient = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var liteClient: LiteClient? = null

    suspend fun getBalance(): String? {
        val fullAccountState = getFullAccountStateOrNull()
        val account = fullAccountState?.account?.value
        return if (account is AccountInfo) {
            account.storage.balance.coins.amount.value.toString(10)
        } else {
            null
        }
    }

    suspend fun transactions(transactionHash: String?, lt: Long?, limit: Int): List<TonTransactionWithTransfers> {
        val transactionId = when {
            transactionHash != null && lt != null -> TransactionId(BitString(transactionHash), lt)
            else -> getFullAccountStateOrNull()?.lastTransactionId
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

    suspend fun getFullAccountStateOrNull(): FullAccountState? = try {
        getLiteClient().getAccountState(addrStd)
    } catch (e: Throwable) {
        null
    }

    private fun createTonTransaction(info: TransactionInfo): TonTransactionWithTransfers {
        val transactionHash = info.id.hash.toHex()
        val txAux = info.transaction.value.r1.value

        val transactionType: TransactionType
        val amount: BigInt?
        val memo: String?
        var fee = info.transaction.value.totalFees.coins.amount.value
        val transfers = mutableListOf<TonTransfer>()

        val outIntMsgInfoList = txAux.outMsgs.mapNotNull { (_, messageCellRef) ->
            messageCellRef.value.info as? IntMsgInfo
        }

        val inIntMsgInfo = txAux.inMsg.value?.value?.info as? IntMsgInfo

        if (outIntMsgInfoList.isNotEmpty()) {
            var transferAmountsSum = 0.toBigInt()
            var transferFeesSum = 0.toBigInt()
            outIntMsgInfoList.forEach { msgInfo ->
                val transferAmount = msgInfo.value.coins.amount.value
                transfers.add(
                    TonTransfer(
                        transactionHash = transactionHash,
                        src = TonAddress(msgInfo.src),
                        dest = TonAddress(msgInfo.dest),
                        amount = transferAmount.toString(10),
                    )
                )

                transferAmountsSum += transferAmount
                transferFeesSum += msgInfo.fwd_fee.amount.value + msgInfo.ihr_fee.amount.value
            }

            fee += transferFeesSum
            amount = transferAmountsSum
            memo = txAux.outMsgs.firstNotNullOfOrNull { (_, cellRef) ->
                parseMemo(cellRef)
            }
            transactionType = TransactionType.Outgoing
        } else if (inIntMsgInfo != null) {
            val transferAmount = inIntMsgInfo.value.coins.amount.value
            transfers.add(
                TonTransfer(
                    transactionHash = transactionHash,
                    src = TonAddress(inIntMsgInfo.src),
                    dest = TonAddress(inIntMsgInfo.dest),
                    amount = transferAmount.toString(10),
                )
            )

            amount = transferAmount
            memo = txAux.inMsg.value?.let { parseMemo(it) }
            transactionType = TransactionType.Incoming
        } else {
            amount = null
            memo = null
            transactionType = TransactionType.Unknown
        }

        return TonTransactionWithTransfers(
            hash = transactionHash,
            lt = info.id.lt,
            timestamp = info.transaction.value.now.toLong(),
            amount = amount?.toString(10),
            memo = memo,
            fee = fee.toString(10),
            type = transactionType,
            transfers = transfers
        )
    }

    private fun parseMemo(cellRef: CellRef<Message<Cell>>): String? {
        val cell = cellRef.value.body.let {
            it.x ?: it.y?.value
        }

        val messageText = cell?.let {
            try {
                MessageText.loadTlb(cell) as? MessageText.Raw
            } catch (e: Exception) {
                null
            }
        }

        return messageText?.text
    }

    suspend fun getLatestTransactionHash(): String? {
        return getFullAccountStateOrNull()?.lastTransactionId?.hash?.toHex()
    }

    suspend fun getLiteClient(): LiteClient {
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
