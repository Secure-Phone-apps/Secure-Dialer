/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ScheduledFakeCall
import com.example.util.FakeCallManager
import com.example.util.FakeCallVoicePlayer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FakeCallSimulatorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FakeCallManager.cancelAll(context)
    }

    @Test
    fun testScheduledFakeCallSerialization() {
        val original = ScheduledFakeCall(
            id = "test-call-1",
            name = "Boss",
            number = "+1 (555) 0192",
            triggerTimeMillis = 1700000000000L,
            audioUri = "content://media/audio/123",
            audioVoiceType = "OFFICE"
        )

        val json = original.toJson()
        val restored = ScheduledFakeCall.fromJson(json)

        assertEquals("test-call-1", restored.id)
        assertEquals("Boss", restored.name)
        assertEquals("+1 (555) 0192", restored.number)
        assertEquals(1700000000000L, restored.triggerTimeMillis)
        assertEquals("content://media/audio/123", restored.audioUri)
        assertEquals("OFFICE", restored.audioVoiceType)
    }

    @Test
    fun testFakeCallManagerScheduleAndRetrieveMultiple() {
        val now = System.currentTimeMillis()
        val call1 = ScheduledFakeCall(
            id = "call-1",
            name = "Doctor",
            number = "+1 800-555-0199",
            triggerTimeMillis = now + 60_000L,
            audioVoiceType = "OFFICE"
        )
        val call2 = ScheduledFakeCall(
            id = "call-2",
            name = "Mom",
            number = "+1 555-728-1100",
            triggerTimeMillis = now + 120_000L,
            audioVoiceType = "FAMILY"
        )

        FakeCallManager.scheduleCall(context, call1)
        FakeCallManager.scheduleCall(context, call2)

        val scheduled = FakeCallManager.getScheduledCalls(context)
        assertEquals(2, scheduled.size)
        assertEquals("Doctor", scheduled[0].name)
        assertEquals("Mom", scheduled[1].name)

        // Cancel one
        FakeCallManager.cancelCall(context, "call-1")
        val remaining = FakeCallManager.getScheduledCalls(context)
        assertEquals(1, remaining.size)
        assertEquals("Mom", remaining[0].name)

        // Cancel all
        FakeCallManager.cancelAll(context)
        assertTrue(FakeCallManager.getScheduledCalls(context).isEmpty())
    }

    @Test
    fun testOnCallTriggeredRemovesFromPending() {
        val now = System.currentTimeMillis()
        val call = ScheduledFakeCall(
            id = "call-auto-remove",
            name = "Lawyer",
            number = "+1 555-9128",
            triggerTimeMillis = now + 30_000L
        )
        FakeCallManager.scheduleCall(context, call)
        assertEquals(1, FakeCallManager.getScheduledCalls(context).size)

        FakeCallManager.onCallTriggered(context, "call-auto-remove")
        assertEquals(0, FakeCallManager.getScheduledCalls(context).size)
    }

    @Test
    fun testVoiceScriptsNonEmpty() {
        val officeScript = FakeCallVoicePlayer.getScript("OFFICE")
        val familyScript = FakeCallVoicePlayer.getScript("FAMILY")
        val friendScript = FakeCallVoicePlayer.getScript("FRIEND")

        assertTrue(officeScript.isNotEmpty())
        assertTrue(familyScript.isNotEmpty())
        assertTrue(friendScript.isNotEmpty())
        assertTrue(officeScript.contains("critical situation") || officeScript.contains("office"))
        assertTrue(familyScript.contains("home") || familyScript.contains("help"))
    }
}
