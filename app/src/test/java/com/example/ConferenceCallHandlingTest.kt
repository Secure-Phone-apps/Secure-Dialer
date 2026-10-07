/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.os.Bundle
import android.telecom.Call
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConferenceCallHandlingTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CallManager.updateCall(null)
    }

    @Test
    fun testConferenceInitialStateIsFalse() {
        assertFalse(CallManager.isConferenceActive.value)
        assertFalse(CallManager.isConference(null))
    }

    @Test
    fun testNullCallDetailsHandledSafely() {
        assertFalse(CallManager.isConference(null))
    }
}
