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

class DialerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        com.example.util.DynamicIslandOverlayManager.startCallMonitoring(this)
        // Fast-path non-blocking native SQLCipher preload and database warm-up
        appScope.launch {
            try {
                if (!AppDatabase.isRobolectricEnvironment()) {
                    try {
                        System.loadLibrary("sqlcipher")
                    } catch (_: UnsatisfiedLinkError) {
                        // AppDatabase.warmUpAsync -> buildDatabase will surface NativeCryptoLoadError
                    }
                }
                AppDatabase.warmUpAsync(this@DialerApplication)
            } catch (_: Throwable) {}
        }
    }
}
