package io.horizontalsystems.tonkit

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(databaseName: String): SqlDriver {
        return AndroidSqliteDriver(KitDatabase.Schema, context, databaseName)
    }
}