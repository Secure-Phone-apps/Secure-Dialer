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

    @Test
    fun `clearAllCallLogs clears the local and flow records`() {
        kotlinx.coroutines.runBlocking {
            viewModel.clearAllCallLogs()
            val callHistory = viewModel.allCallHistoryFlow.value
            assertTrue(callHistory.isEmpty())
        }
    }

    @Test
    fun `cursor-based digit insertion in middle of string works correctly`() {
        viewModel.onDialpadInputChange("1245")
        // Position cursor between '2' and '4' (index 2)
        viewModel.onDialpadTextFieldValueChange(
            androidx.compose.ui.text.input.TextFieldValue(
                text = "1245",
                selection = androidx.compose.ui.text.TextRange(2)
            )
        )
        // Insert '3'
        viewModel.insertDialpadDigit("3")

        assertEquals("12345", viewModel.dialpadInput.value)
        assertEquals("12345", viewModel.dialpadTextFieldValue.value.text)
        assertEquals(3, viewModel.dialpadTextFieldValue.value.selection.start)
    }

    @Test
    fun `cursor-based backspace in middle of string works correctly`() {
        viewModel.onDialpadInputChange("12345")
        // Position cursor right after '3' (index 3)
        viewModel.onDialpadTextFieldValueChange(
            androidx.compose.ui.text.input.TextFieldValue(
                text = "12345",
                selection = androidx.compose.ui.text.TextRange(3)
            )
        )
        // Backspace should remove '3'
        viewModel.backspaceDialpad()

        assertEquals("1245", viewModel.dialpadInput.value)
        assertEquals(2, viewModel.dialpadTextFieldValue.value.selection.start)
    }

    @Test
    fun `selection replacement and selection backspace works correctly`() {
        viewModel.onDialpadInputChange("12995")
        // Select "99" (from index 2 to 4)
        viewModel.onDialpadTextFieldValueChange(
            androidx.compose.ui.text.input.TextFieldValue(
                text = "12995",
                selection = androidx.compose.ui.text.TextRange(2, 4)
            )
        )
        // Insert "34" over selection
        viewModel.insertDialpadDigit("34")
        assertEquals("12345", viewModel.dialpadInput.value)

        // Select "34" and backspace
        viewModel.onDialpadTextFieldValueChange(
            androidx.compose.ui.text.input.TextFieldValue(
                text = "12345",
                selection = androidx.compose.ui.text.TextRange(2, 4)
            )
        )
        viewModel.backspaceDialpad()
        assertEquals("125", viewModel.dialpadInput.value)
        assertEquals(2, viewModel.dialpadTextFieldValue.value.selection.start)
    }

    @Test
    fun `openAddContactWithNumber pre-fills state properly`() {
        viewModel.openAddContactWithNumber("+15551234567")
        assertTrue(viewModel.isAddContactDialogVisible.value)
        assertEquals("+15551234567", viewModel.newContactNumber.value)
        assertEquals("", viewModel.newContactName.value)
    }
}
