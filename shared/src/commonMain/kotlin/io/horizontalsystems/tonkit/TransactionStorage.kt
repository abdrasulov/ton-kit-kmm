package io.horizontalsystems.tonkit

class TransactionStorage(database: Database) {
    private val transactionQuery = database.transactionQuery

    suspend fun getLatestTransaction(): TonTransaction? {
        return transactionQuery.selectLatest().executeAsOneOrNull()
    }

    suspend fun getEarliestTransaction(): TonTransaction? {
        return transactionQuery.selectEarliest().executeAsOneOrNull()
    }

    suspend fun getTransactions(fromTransactionHash: String?, limit: Long): List<TonTransaction> {
        if (fromTransactionHash == null) {
            return transactionQuery.selectAll(limit).executeAsList()
        } else {
            val fromTransaction =
                transactionQuery.selectByHash(fromTransactionHash).executeAsOneOrNull() ?: return listOf()
            return transactionQuery.selectEarlierThan(fromTransaction.timestamp, limit).executeAsList()
        }
    }

    fun add(transactions: List<TonTransaction>) {
        transactionQuery.transaction {
            transactions.forEach { tonTransaction ->
                transactionQuery.insert(
                    hash = tonTransaction.hash,
                    lt = tonTransaction.lt,
                    timestamp = tonTransaction.timestamp,
                    value_ = tonTransaction.value_
                )
            }
        }
    }

}
