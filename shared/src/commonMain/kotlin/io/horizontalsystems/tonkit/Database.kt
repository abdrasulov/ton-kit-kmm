package io.horizontalsystems.tonkit

class Database(databaseDriverFactory: DriverFactory, databaseName: String) {
    private val database = KitDatabase(databaseDriverFactory.createDriver(databaseName))
    val transactionQuery = database.tonTransactionQueries
    val balanceQuery = database.tonBalanceQueries
}
