package io.horizontalsystems.tonkit

class Database(databaseDriverFactory: DriverFactory) {
    private val database = KitDatabase(databaseDriverFactory.createDriver())
    val transactionQuery = database.tonTransactionQueries


}