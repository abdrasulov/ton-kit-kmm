package io.horizontalsystems.tonkit

class TransactionStorage {

    private var cache = listOf<TonTransaction>()

    suspend fun getLastTransaction(): TonTransaction? {
        return cache.firstOrNull()
    }

    suspend fun getTransactions(fromTransactionHash: String?, limit: Int?): List<TonTransaction> {
        if (cache.isEmpty()) return listOf()

        val fromIndex = when (fromTransactionHash) {
            null -> 0
            else -> cache.indexOfFirst { it.hash == fromTransactionHash }
        }

        if (fromIndex == -1) return listOf()

        val toIndex = if (limit == null) {
            cache.size
        } else {
            fromIndex + limit
        }

        return cache.subList(fromIndex, toIndex)
    }

    fun add(transactions: List<TonTransaction>) {
        cache = (cache + transactions).sortedByDescending { it.timestamp }
    }

}
