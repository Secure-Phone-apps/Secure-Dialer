/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase

class DialerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Fast-path non-blocking database and security warm-up
        appScope.launch {
            try {
                SQLiteDatabase.loadLibs(this@DialerApplication)
                AppDatabase.warmUpAsync(this@DialerApplication)
            } catch (_: Throwable) {}
        }
    }
}
