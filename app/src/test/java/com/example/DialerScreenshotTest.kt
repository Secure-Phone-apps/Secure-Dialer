package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.MainScreen
import com.example.ui.viewmodel.DialerViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DialerScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun main_screen_light_mode() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val viewModel = DialerViewModel(context)
            MyApplicationTheme(darkTheme = false) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = {},
                    isDefaultDialer = true
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_light.png")
    }

    @Test
    fun main_screen_dark_mode() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val viewModel = DialerViewModel(context)
            MyApplicationTheme(darkTheme = true) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = {},
                    isDefaultDialer = true
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_dark.png")
    }
}
