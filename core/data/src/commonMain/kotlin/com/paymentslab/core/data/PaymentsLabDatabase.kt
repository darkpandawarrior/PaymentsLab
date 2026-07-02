package com.paymentslab.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The KMP Room database backing the process-death journal.
 *
 * v1 ships a single [PendingPaymentEntity] table with no migrations yet. `exportSchema = true`
 * writes the schema JSON to `schemas/` so that future explicit migrations can diff against it —
 * destructive fallback is deliberately never enabled.
 */
@Database(
    entities = [PendingPaymentEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(PaymentsLabDatabaseConstructor::class)
abstract class PaymentsLabDatabase : RoomDatabase() {
    abstract fun pendingPaymentDao(): PendingPaymentDao
}

/**
 * KMP database-constructor seam: KSP generates the `actual` per target so `commonMain` can stay
 * free of platform builders.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object PaymentsLabDatabaseConstructor : RoomDatabaseConstructor<PaymentsLabDatabase>
