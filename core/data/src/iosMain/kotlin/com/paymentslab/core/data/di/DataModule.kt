package com.paymentslab.core.data.di

import com.paymentslab.core.data.PaymentsLabDatabase
import com.paymentslab.core.data.RoomPendingPaymentJournal
import com.paymentslab.core.data.database.getDatabaseBuilder
import com.siddharth.kmp.paymentsapi.PendingPaymentJournal
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS Koin wiring for `core:data` — the B8 counterpart to the Android [dataModule] in
 * `androidMain`. Same shape (Room DB → DAO → [PendingPaymentJournal] binding); the only difference
 * is [getDatabaseBuilder] needs no `Context` on iOS, it resolves the Documents directory itself
 * (see the iOS `actual`).
 */
val dataModule: Module =
    module {
        single<PaymentsLabDatabase> { getDatabaseBuilder().build() }
        single { get<PaymentsLabDatabase>().pendingPaymentDao() }
        single<PendingPaymentJournal> { RoomPendingPaymentJournal(get()) }
    }
