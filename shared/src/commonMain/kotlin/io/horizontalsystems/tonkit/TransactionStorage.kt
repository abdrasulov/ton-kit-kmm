package io.horizontalsystems.tonkit

class TransactionStorage(private val transactionQuery: TonTransactionQueries) {

    fun getLatestTransaction(): TonTransaction? {
        return transactionQuery.getLatest().executeAsOneOrNull()
    }

    fun getEarliestTransaction(): TonTransaction? {
        return transactionQuery.getEarliest().executeAsOneOrNull()
    }

    fun getTransactions(
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

    fun add(transactions: List<TonTransaction>) {
        transactionQuery.transaction {
            transactions.forEach { transaction ->
                transactionQuery.insert(
                    transaction.hash,
                    transaction.lt,
                    transaction.timestamp,
                    transaction.amount,
                    transaction.fee,
                    transaction.type,
                    transaction.transfersJson,
                    transaction.memo
                )
            }
        }
    }
}
