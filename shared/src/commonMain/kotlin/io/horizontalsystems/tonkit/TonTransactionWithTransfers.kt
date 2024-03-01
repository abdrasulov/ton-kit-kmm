package io.horizontalsystems.tonkit

data class TonTransactionWithTransfers(
    val hash: String,
    val lt: Long,
    val timestamp: Long,
    val amount: String?,
    val fee: String?,
    val type: TransactionType,
    val transfers: List<TonTransfer>,
    val memo: String?,
)
