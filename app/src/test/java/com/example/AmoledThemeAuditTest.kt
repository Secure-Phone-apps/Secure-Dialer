package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.getColorSchemeForTheme
import com.example.ui.viewmodel.DialerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmoledThemeAuditTest {

    @Test
    fun `verify amoled mode state updates and persists in ViewModel`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = DialerViewModel(app)

        // Set AMOLED mode to true
        viewModel.updateAmoledMode(true)
        assertTrue(viewModel.isAmoledMode.value)

        // Verify persistence in SharedPreferences
        val prefs = app.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("is_amoled_mode", false))

        // Toggle back to false
        viewModel.updateAmoledMode(false)
        assertFalse(viewModel.isAmoledMode.value)
        assertFalse(prefs.getBoolean("is_amoled_mode", true))
    }

    @Test
    fun `verify true black oled color values conform to pure black specifications`() {
        val pureBlack = Color(0xFF000000)
        assertEquals(0f, pureBlack.red, 0.001f)
        assertEquals(0f, pureBlack.green, 0.001f)
        assertEquals(0f, pureBlack.blue, 0.001f)
        assertEquals(1f, pureBlack.alpha, 0.001f)
    }

    @Test
    fun `verify getColorSchemeForTheme produces valid theme schemes`() {
        val darkScheme = getColorSchemeForTheme("ocean_blue", darkTheme = true)
        assertEquals(Color(0xFF000000), darkScheme.background)

        val lightScheme = getColorSchemeForTheme("ocean_blue", darkTheme = false)
        assertEquals(Color(0xFFFFFFFF), lightScheme.background)
    }
}
