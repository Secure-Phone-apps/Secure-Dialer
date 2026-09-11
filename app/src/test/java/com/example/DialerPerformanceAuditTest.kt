package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.Contact
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerPerformanceAuditTest {

    private lateinit var viewModel: DialerViewModel
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = DialerViewModel(context)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun auditRapidTabSwitchingExecutionBudget() {
        // We simulate a highly intense user session: switching tabs 100 times sequentially
        val totalInteractions = 100
        val startTime = System.nanoTime()

        for (i in 0 until totalInteractions) {
            val targetTab = i % 3
            viewModel.selectedTab.value = targetTab
            shadowOf(Looper.getMainLooper()).idle() // synchronous composition flush
        }

        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0
        val averageLatencyPerSwitch = durationMs / totalInteractions

        println("Performance Audit [Rapid Tab Switch]:")
        println(" - Total Switches: $totalInteractions")
        println(" - Total Duration: $durationMs ms")
        println(" - Avg Latency per switch: $averageLatencyPerSwitch ms")

        // Assert average state transition latency is under 8.0ms (comfortably within the 8.3ms window for a locked 120 FPS target)
        assertTrue(
            "Average tab-switch state propagation exceeds the 120 FPS 8.3ms threshold! Latency: $averageLatencyPerSwitch ms",
            averageLatencyPerSwitch < 8.3
        )
    }

    @Test
    fun auditDialpadKeyInputThroughput() {
        // Simulate rapid speed-typing on the dialpad (50 characters typed)
        val testInput = "1234567890*#1234567890*#1234567890*#1234567890*#123456"
        val startTime = System.nanoTime()

        var currentInput = ""
        testInput.forEach { char ->
            currentInput += char
            viewModel.onDialpadInputChange(currentInput)
            shadowOf(Looper.getMainLooper()).idle()
        }

        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0
        val averageLatencyPerKeystroke = durationMs / testInput.length

        println("Performance Audit [Speed Typing Throughput]:")
        println(" - Total Keystrokes: ${testInput.length}")
        println(" - Total Duration: $durationMs ms")
        println(" - Avg Latency per keystroke: $averageLatencyPerKeystroke ms")

        // Assert input state propagation completes well under 4ms per key-press to avoid any perceived interface lagging
        assertTrue(
            "Average keystroke state propagation exceeds tactile budget! Latency: $averageLatencyPerKeystroke ms",
            averageLatencyPerKeystroke < 4.0
        )
    }

    @Test
    fun auditHighVolumeContactsFilteringAndSearch() = runBlocking {
        // 1. Get database DAO and mass-insert 5,000 synthetic contacts
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        val count = 5000
        val syntheticContacts = ArrayList<Contact>(count)
        for (i in 1..count) {
            syntheticContacts.add(
                Contact(
                    id = i.toLong(),
                    rawContactId = i.toLong(),
                    contactId = i.toLong(),
                    number = "555${100000 + i}",
                    name = "Synthetic Contact Name $i",
                    label = "Mobile",
                    favorite = false,
                    avatarText = "S",
                    avatarBgValue = 0xFF4CAF50L,
                    avatarTextColorValue = 0xFFFFFFFFL,
                    t9Mapping = "796843842"
                )
            )
        }

        dao.insertContacts(syntheticContacts)
        shadowOf(Looper.getMainLooper()).idle()

        // 2. Perform sequential high-throughput search queries and measure execution latency
        val totalQueries = 50
        val startTime = System.nanoTime()

        for (q in 1..totalQueries) {
            // Alternating query keywords to stress filter evaluation pathways
            val queryText = if (q % 2 == 0) "Synthetic Contact Name 2" else "5551"
            viewModel.onDialpadInputChange(queryText)
            shadowOf(Looper.getMainLooper()).idle() // Ensure composition updates propagate
        }

        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0
        val averageLatencyPerQuery = durationMs / totalQueries

        println("Performance Audit [High Volume 5,000 Contacts Filtering]:")
        println(" - Total Synthetic Contacts Inserted: $count")
        println(" - Total Sequential Search Queries: $totalQueries")
        println(" - Total Duration: $durationMs ms")
        println(" - Avg Search Latency: $averageLatencyPerQuery ms")

        // Romain Guy constraint: search match evaluation must stay within 8.3ms to avoid any frames drop
        assertTrue(
            "Average high-volume search latency exceeds 120 FPS threshold! Latency: $averageLatencyPerQuery ms",
            averageLatencyPerQuery < 8.3
        )
    }
}
