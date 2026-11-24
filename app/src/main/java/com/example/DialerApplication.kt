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

    override fun onCreate() {
        super.onCreate()
        com.example.util.DynamicIslandOverlayManager.startCallMonitoring(this)
        // Fast-path coordinated non-blocking native SQLCipher preload and database warm-up
        AppDatabase.warmUpAsync(this)
    }
}
