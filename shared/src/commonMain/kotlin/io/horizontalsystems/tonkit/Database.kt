package io.horizontalsystems.tonkit

import app.cash.sqldelight.EnumColumnAdapter

class Database(databaseDriverFactory: DriverFactory, databaseName: String) {
    private val database = KitDatabase(
        driver = databaseDriverFactory.createDriver(databaseName),
        TonTransactionAdapter = TonTransaction.Adapter(EnumColumnAdapter())
    )
    val transactionQuery = database.tonTransactionQueries
    val balanceQuery = database.tonBalanceQueries
}
