package io.horizontalsystems.tonkit

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(databaseName: String): SqlDriver
}
