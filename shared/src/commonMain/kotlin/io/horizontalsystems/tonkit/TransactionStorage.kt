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
        address: String?,
        limit: Long,
    ): List<TonTransactionWithTransfers> {
        val transactions = getTransactions(fromTransactionHash, type, address, limit)
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
        address: String?,
        limit: Long,
    ): List<TonTransaction> {
        val fromTransaction = fromTransactionHash?.let {
            transactionQuery.getByHash(fromTransactionHash).executeAsOneOrNull() ?: return emptyList()
        }

        return queryTransactions(
            type = type,
            timestamp = fromTransaction?.timestamp,
            lt = fromTransaction?.lt,
            address = address,
            limit = limit,
        )
    }

    private fun queryTransactions(
        type: TransactionType? = null,
        timestamp: Long? = null,
        lt: Long? = null,
        address: String? = null,
        limit: Long
    ): List<TonTransaction> {
        val skipEarlierThan = timestamp == null || lt == null

        val hashes = when {
            address != null -> transferQuery
                .getTransactionHashesByAddress(TonAddress.parse(address))
                .executeAsList()

            else -> listOf()
        }

        return transactionQuery.getByQuery(
            skipEarlierThan = skipEarlierThan,
            timestampEarlierThan = timestamp ?: Long.MAX_VALUE,
            ltEarlierThan = lt ?: Long.MAX_VALUE,
            skipHash = hashes.isEmpty(),
            hashes = hashes,
            type = type,
            limit = limit
        ).executeAsList()
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
