package com.paymentslab.core.data.di

import com.paymentslab.core.data.PaymentsLabDatabase
import com.paymentslab.core.data.RoomPendingPaymentJournal
import com.paymentslab.core.data.database.getDatabaseBuilder
import com.siddharth.kmp.paymentsapi.PendingPaymentJournal
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android Koin wiring for `core:data` — see `iosMain`'s `DataModule.kt` for the B8 iOS
 * counterpart (same shape, [getDatabaseBuilder] just doesn't need a `Context` there).
 *
 * The database is built once (single), its DAO exposed, and [RoomPendingPaymentJournal] bound to
 * the `core:payments-api` [PendingPaymentJournal] interface so consumers depend on the seam, not
 * the Room impl.
 */
val dataModule: Module =
    module {
        single<PaymentsLabDatabase> { getDatabaseBuilder(androidContext()).build() }
        single { get<PaymentsLabDatabase>().pendingPaymentDao() }
        single<PendingPaymentJournal> { RoomPendingPaymentJournal(get()) }
    }
