package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Secure Dialer", appName)
  }

  @Test
  fun `verify CallManager initial state`() {
    assertNull(CallManager.currentCall.value)
    assertEquals(android.telecom.Call.STATE_DISCONNECTED, CallManager.callState.value)
    assertEquals("", CallManager.callerNumber.value)
    assertEquals("", CallManager.callerName.value)
  }

  @Test
  fun `verify CallManager updates correctly when cleared`() {
    CallManager.updateCall(null)
    assertNull(CallManager.currentCall.value)
    assertEquals(android.telecom.Call.STATE_DISCONNECTED, CallManager.callState.value)
    assertEquals("", CallManager.callerNumber.value)
  }
}
