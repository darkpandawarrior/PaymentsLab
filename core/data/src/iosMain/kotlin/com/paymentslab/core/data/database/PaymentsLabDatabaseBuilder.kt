package com.paymentslab.core.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.paymentslab.core.data.PaymentsLabDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS database builder. v1 ships Android-only wiring (no Koin on iOS); this `actual` exists so
 * `iosMain` compiles and the KMP Room contract is honoured across every declared target. The db
 * file lives in the app's Documents directory.
 */
@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(): RoomDatabase.Builder<PaymentsLabDatabase> {
    val documentsUrl =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    val dbFilePath = requireNotNull(documentsUrl?.path) + "/paymentslab.db"
    return Room
        .databaseBuilder<PaymentsLabDatabase>(name = dbFilePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
}
