package com.paymentslab.core.data.di

import com.paymentslab.core.data.PaymentsLabDatabase
import com.paymentslab.core.data.RoomPendingPaymentJournal
import com.paymentslab.core.data.database.getDatabaseBuilder
import com.paymentslab.core.paymentsapi.PendingPaymentJournal
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android Koin wiring for `core:data`. v1 ships Android only — iOS provides just the `actual`
 * database builder to keep the KMP contract compiling; no iOS Koin module is needed yet.
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
