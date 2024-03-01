package io.horizontalsystems.tonkit

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter

class Database(databaseDriverFactory: DriverFactory, databaseName: String) {
    private val database = KitDatabase(
        driver = databaseDriverFactory.createDriver(databaseName),
        TonTransactionAdapter = TonTransaction.Adapter(EnumColumnAdapter()),
        TonTransferAdapter = TonTransfer.Adapter(
            srcAdapter = TonAddressColumnAdapter(),
            destAdapter = TonAddressColumnAdapter(),
        )
    )
    val transactionQuery = database.tonTransactionQueries
    val transferQuery = database.tonTransferQueries
    val balanceQuery = database.tonBalanceQueries
}

class TonAddressColumnAdapter : ColumnAdapter<TonAddress, String> {
    override fun decode(databaseValue: String): TonAddress {
        return TonAddress(TonAddress.parse(databaseValue))
    }

    override fun encode(value: TonAddress): String {
        return value.toRaw()
    }
}
