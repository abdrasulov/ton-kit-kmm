package io.horizontalsystems.tonkit

class TransactionStorage(
    private val transactionQuery: TonTransactionQueries,
    private val transferQuery: TonTransferQueries
) {

    fun getLatestTransaction(): TonTransaction? {
        return transactionQuery.getLatest().executeAsOneOrNull()
    }

    fun getEarliestTransaction(): TonTransaction? {
        return transactionQuery.getEarliest().executeAsOneOrNull()
    }

    fun getTransactionsWithTransfers(
        fromTransactionHash: String?,
        type: TransactionType?,
        limit: Long,
    ): List<TonTransactionWithTransfers> {
        val transactions = getTransactions(fromTransactionHash, type, limit)
        val transactionHashes = transactions.map { it.hash }
        val transfers = getTransfers(transactionHashes).groupBy { it.transactionHash }

        return transactions.map {
            TonTransactionWithTransfers(
                hash = it.hash,
                lt = it.lt,
                timestamp = it.timestamp,
                amount = it.amount,
                fee = it.fee,
                type = it.type,
                transfers = transfers[it.hash] ?: listOf(),
                memo = it.memo,
            )
        }
    }

    private fun getTransfers(transactionHashes: List<String>): List<TonTransfer> {
        return transferQuery.getByTransactionHash(transactionHashes).executeAsList()
    }

    private fun getTransactions(
        fromTransactionHash: String?,
        type: TransactionType?,
        limit: Long,
    ) = when (type) {
        null -> getTransactions(fromTransactionHash, limit)
        else -> getTransactionsByType(fromTransactionHash, type, limit)
    }

    private fun getTransactions(
        fromTransactionHash: String?,
        limit: Long,
    ): List<TonTransaction> {
        if (fromTransactionHash == null) {
            return transactionQuery.getAll(limit).executeAsList()
        } else {
            val fromTransaction = transactionQuery.getByHash(fromTransactionHash).executeAsOneOrNull() ?: return listOf()
            return transactionQuery.getEarlierThan(
                fromTransaction.timestamp,
                fromTransaction.lt,
                limit
            ).executeAsList()
        }
    }

    private fun getTransactionsByType(
        fromTransactionHash: String?,
        type: TransactionType,
        limit: Long,
    ): List<TonTransaction> {
        if (fromTransactionHash == null) {
            return transactionQuery.getAllByType(type = type, limit = limit).executeAsList()
        } else {
            val fromTransaction = transactionQuery.getByHash(fromTransactionHash).executeAsOneOrNull() ?: return listOf()
            return transactionQuery.getEarlierThanByType(
                timestamp = fromTransaction.timestamp,
                lt = fromTransaction.lt,
                type = type,
                limit = limit
            ).executeAsList()
        }
    }

    fun add(transactions: List<TonTransactionWithTransfers>) {
        transactionQuery.transaction {
            transactions.forEach { transaction ->
                transactionQuery.insert(
                    transaction.hash,
                    transaction.lt,
                    transaction.timestamp,
                    transaction.amount,
                    transaction.fee,
                    transaction.type,
                    "",
                    transaction.memo
                )

                transferQuery.deleteAllByTransactionHash(transaction.hash)

                transaction.transfers.forEach {
                    transferQuery.insert(
                        it.transactionHash,
                        it.src,
                        it.dest,
                        it.amount,
                    )
                }
            }
        }
    }
}
