package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.CallRecord
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerViewModelTest {

    private lateinit var viewModel: DialerViewModel
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = DialerViewModel(context)
    }

    @Test
    fun `initial state is correct`() {
        assertFalse(viewModel.isDialpadVisible.value)
        assertEquals("", viewModel.dialpadInput.value)
        assertEquals(0, viewModel.selectedTab.intValue)
    }

    @Test
    fun `onSearchQueryChange updates searchQuery state`() {
        val query = "John"
        viewModel.onSearchQueryChange(query)
        assertEquals(query, viewModel.searchQuery.value)
    }

    @Test
    fun `toggle dialpad visibility`() {
        viewModel.isDialpadVisible.value = true
        assertTrue(viewModel.isDialpadVisible.value)
        viewModel.isDialpadVisible.value = false
        assertFalse(viewModel.isDialpadVisible.value)
    }

    @Test
    fun `dialpad tones and vibrate on click settings persist across ViewModel recreation`() {
        viewModel.updateDialpadTonesEnabled(false)
        viewModel.updateVibrateOnClickEnabled(false)
        viewModel.updateFlipToSilenceEnabled(true)
        viewModel.updateCallWaitingEnabled(false)
        viewModel.updateRecordingEnabled(true)
        viewModel.updateFlashAlertsEnabled(true)
        viewModel.updateBiometricLockEnabled(true)
        viewModel.updatePocketProtectionEnabled(true)
        viewModel.updateCallbackRemindersEnabled(false)
        viewModel.updateCallNotesEnabled(false)
        viewModel.updateFakeCallSimulatorEnabled(false)
        viewModel.onAccountFilterChange("Google:test@gmail.com")
        viewModel.updateDefaultContactAccount("Google:test@gmail.com", "com.google")

        assertFalse(viewModel.dialpadTonesEnabled.value)
        assertFalse(viewModel.vibrateOnClickEnabled.value)
        assertTrue(viewModel.flipToSilenceEnabled.value)
        assertFalse(viewModel.callWaitingEnabled.value)
        assertTrue(viewModel.recordingEnabled.value)
        assertTrue(viewModel.flashAlertsEnabled.value)
        assertTrue(viewModel.isBiometricLockEnabled.value)
        assertTrue(viewModel.isPocketProtectionEnabled.value)
        assertFalse(viewModel.isCallbackRemindersEnabled.value)
        assertFalse(viewModel.isCallNotesEnabled.value)
        assertFalse(viewModel.isFakeCallSimulatorEnabled.value)
        assertEquals("Google:test@gmail.com", viewModel.selectedAccountFilter.value)
        assertEquals("Google:test@gmail.com", viewModel.defaultContactAccountName.value)

        // Recreate ViewModel simulating app restart
        val newViewModel = DialerViewModel(context)
        assertFalse(newViewModel.dialpadTonesEnabled.value)
        assertFalse(newViewModel.vibrateOnClickEnabled.value)
        assertTrue(newViewModel.flipToSilenceEnabled.value)
        assertFalse(newViewModel.callWaitingEnabled.value)
        assertTrue(newViewModel.recordingEnabled.value)
        assertTrue(newViewModel.flashAlertsEnabled.value)
        assertTrue(newViewModel.isBiometricLockEnabled.value)
        assertTrue(newViewModel.isPocketProtectionEnabled.value)
        assertFalse(newViewModel.isCallbackRemindersEnabled.value)
        assertFalse(newViewModel.isCallNotesEnabled.value)
        assertFalse(newViewModel.isFakeCallSimulatorEnabled.value)
        assertEquals("Google:test@gmail.com", newViewModel.selectedAccountFilter.value)
        assertEquals("Google:test@gmail.com", newViewModel.defaultContactAccountName.value)
    }

    @Test
    fun `inspect call logs dates`() {
        val repo = com.example.DialerRepository(context)
        kotlinx.coroutines.runBlocking {
            repo.syncCallLogs()
            val callLogs = repo.getAllCallHistoryFlow()
            val firstList = callLogs.first()
            println("DIALER_TEST_LOGS_COUNT: ${firstList.size}")
            if (firstList.isNotEmpty()) {
                val f = firstList.first()
                val l = firstList.last()
                println("FIRST_LOG_TIMESTAMP: ${f.timestamp}, timestampMs: ${f.timestampMs}")
                println("LAST_LOG_TIMESTAMP: ${l.timestamp}, timestampMs: ${l.timestampMs}")
            }
        }
    }
}
