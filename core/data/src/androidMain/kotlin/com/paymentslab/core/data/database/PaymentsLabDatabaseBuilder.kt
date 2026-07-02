package com.paymentslab.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.paymentslab.core.data.PaymentsLabDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Android database builder. Uses the bundled SQLite driver (KMP-portable, no NDK dependency) and
 * runs queries on [Dispatchers.IO]. No migrations at v1; no `fallbackToDestructiveMigration` —
 * future schema changes ship explicit [androidx.room.migration.Migration]s.
 */
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<PaymentsLabDatabase> =
    Room
        .databaseBuilder<PaymentsLabDatabase>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath("paymentslab.db").absolutePath,
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
