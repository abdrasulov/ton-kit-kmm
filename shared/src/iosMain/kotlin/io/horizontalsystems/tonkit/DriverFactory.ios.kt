package io.horizontalsystems.tonkit

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(databaseName: String): SqlDriver {
        return NativeSqliteDriver(KitDatabase.Schema, databaseName)
    }
}