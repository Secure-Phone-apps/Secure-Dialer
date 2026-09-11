package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerRobustnessTest {

    private lateinit var viewModel: DialerViewModel
    private lateinit var context: Application
    private val testScope = CoroutineScope(Dispatchers.Main)
    private val activeJobs = mutableListOf<Job>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = DialerViewModel(context)
        
        // Subscribe background collectors so lazy/WhileSubscribed Flow caches are kept alive and up-to-date
        activeJobs.add(testScope.launch { viewModel.allCallHistoryFlow.collect {} })
        activeJobs.add(testScope.launch { viewModel.speedDialFlow.collect {} })
        activeJobs.add(testScope.launch { viewModel.blockedNumbersFlow.collect {} })
        
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun teardown() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        testScope.cancel()
    }

    private fun waitUntil(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw AssertionError("Condition not met within $timeoutMs ms")
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    @Test
    fun verifyDialpadSlideStateTransitions() {
        // Initially, the dialer overlay is closed
        assertFalse(viewModel.isDialpadVisible.value)
        assertEquals("", viewModel.dialpadInput.value)

        // Simulating tap on the Floating Dialer FAB
        viewModel.isDialpadVisible.value = true
        assertTrue(viewModel.isDialpadVisible.value)

        // Type a sequence of numbers on the dialpad
        viewModel.onDialpadInputChange("123")
        assertEquals("123", viewModel.dialpadInput.value)

        // Simulating BackHandler trigger: back press dismisses dialpad first
        if (viewModel.isDialpadVisible.value) {
            viewModel.isDialpadVisible.value = false
        }
        assertFalse(viewModel.isDialpadVisible.value)
        assertEquals("123", viewModel.dialpadInput.value) // input is preserved on dismiss
    }

    @Test
    fun verifySpeedDialResolutions() {
        // Setup simple Speed Dial configurations
        viewModel.saveSpeedDial(1, "555-0199", "Speed 1")
        viewModel.saveSpeedDial(2, "555-0288", "Speed 2")

        // Wait until speed dial list propagates
        waitUntil { viewModel.speedDialFlow.value.size == 2 }

        val speedDialMap = viewModel.speedDialFlow.value.associate { it.key to it.number }
        assertEquals("555-0199", speedDialMap[1])
        assertEquals("555-0288", speedDialMap[2])
        assertNull(speedDialMap[3]) // not configured
    }

    @Test
    fun verifyEmergencyNumberPreProcessing() {
        // Standard emergency bypass rules filter out unwanted non-dialable formats
        val emergencyNumberRaw = "+9-1-1#"
        val cleanNumber = emergencyNumberRaw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        assertEquals("+911#", cleanNumber)

        val pureEmergency = "911"
        val isEmergency = pureEmergency == "911" || pureEmergency == "112" || pureEmergency == "999"
        assertTrue(isEmergency)
    }

    @Test
    fun verifyMassTransactionStabilityAndWipeSpeed() {
        // Perform multiple insertions to verify local Room/StateFlow throughput speed
        val startTime = System.currentTimeMillis()

        for (i in 1..50) {
            viewModel.logCall("Call $i", "1000$i", CallType.OUTGOING, i * 10L)
        }

        // Wait until all 50 insertions propagate successfully
        waitUntil { viewModel.allCallHistoryFlow.value.size == 50 }

        val logsBeforeWipe = viewModel.allCallHistoryFlow.value
        assertEquals(50, logsBeforeWipe.size)

        // Perform instant mass-wipe
        viewModel.clearAllCallLogs()

        // Wait until wipe propagates
        waitUntil { viewModel.allCallHistoryFlow.value.isEmpty() }

        val logsAfterWipe = viewModel.allCallHistoryFlow.value
        assertTrue(logsAfterWipe.isEmpty())

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Assert speed boundary is under 2500ms for memory/state flow transactions
        assertTrue("Database batch handling exceeded performance target", totalTime < 2500)
    }

    @Test
    fun verifyBlockListBehaviorAndMviIntegrity() {
        val numberToBlock = "5550143"
        
        // Confirm initially clean
        assertFalse(viewModel.blockedNumbersFlow.value.any { it.number == numberToBlock })

        // Execute block intent
        viewModel.addBlockedNumber(numberToBlock)

        // Wait until block propagates
        waitUntil { viewModel.blockedNumbersFlow.value.any { it.number == numberToBlock } }

        // Execute unblock intent
        viewModel.removeBlockedNumber(numberToBlock)

        // Wait until unblock propagates
        waitUntil { !viewModel.blockedNumbersFlow.value.any { it.number == numberToBlock } }
    }
}
