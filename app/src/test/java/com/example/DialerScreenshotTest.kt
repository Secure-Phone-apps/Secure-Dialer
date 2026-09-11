package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
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

    @Test
    fun main_screen_dialpad_sliding_mid_transition() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val viewModel = DialerViewModel(context)
            viewModel.isDialpadVisible.value = true
            MyApplicationTheme(darkTheme = false) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = {},
                    isDefaultDialer = true
                )
            }
        }
        // Take manual control of the main Compose animation clock
        composeTestRule.mainClock.autoAdvance = false
        // Advance clock by exactly 80ms to freeze the kinetic spring slide-up mid-transition
        composeTestRule.mainClock.advanceTimeBy(80)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dialpad_slide_mid_transition.png")
    }

    @Test
    fun main_screen_dialpad_fully_open() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val viewModel = DialerViewModel(context)
            viewModel.isDialpadVisible.value = true
            MyApplicationTheme(darkTheme = false) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = {},
                    isDefaultDialer = true
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dialpad_fully_open.png")
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun tablet_landscape_mode_light() {
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
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_landscape_light.png")
    }

    @Test
    @Config(qualifiers = "w600dp-h900dp-port-hdpi", sdk = [34])
    fun foldable_portrait_mode_dark() {
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
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/foldable_portrait_dark.png")
    }

    @Test
    fun main_screen_accessibility_font_scale_150() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val viewModel = DialerViewModel(context)
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            val customDensity = androidx.compose.ui.unit.Density(
                density = currentDensity.density,
                fontScale = 1.5f
            )
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides customDensity
            ) {
                MyApplicationTheme(darkTheme = false) {
                    MainScreen(
                        viewModel = viewModel,
                        onShowRestrictedSettings = {},
                        isDefaultDialer = true
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/accessibility_font_scale_150.png")
    }

    @Test
    fun dialer_button_press_feedback() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val viewModel = DialerViewModel(context)
            MyApplicationTheme(darkTheme = false) {
                com.example.ui.components.DialpadOverlay(
                    inputValue = "",
                    onValueChange = {},
                    onClose = {},
                    onCallClick = {},
                    onSpeedDialCall = {},
                    speedDialMap = emptyMap(),
                    voicemailNumber = "",
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()

        // Take control of the animation clock
        composeTestRule.mainClock.autoAdvance = false

        // Locate Key 5 and initiate a persistent touch-down interaction
        composeTestRule.onNodeWithTag("dialpad_key_5").performTouchInput {
            down(center)
        }

        // Advance by 30ms to render the touch down shrink scaling transition mid-flow
        composeTestRule.mainClock.advanceTimeBy(30)

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dialpad_key_press_tactile_feedback.png")
    }

    @Test
    @Config(qualifiers = "de-rDE-hdpi", sdk = [34])
    fun main_screen_german_locale() {
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
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_locale_de.png")
    }

    @Test
    @Config(qualifiers = "ja-rJP-hdpi", sdk = [34])
    fun main_screen_japanese_locale() {
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
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_locale_ja.png")
    }

    @Test
    fun incoming_call_screen_light_mode() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                com.example.ui.components.ActiveCallScreen(
                    contactName = "John Smith",
                    contactNumber = "555-0144",
                    preferredSim = "SIM 2",
                    quickResponses = listOf("Sorry, busy.", "In a meeting."),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = true,
                    recordingEnabled = false,
                    isFake = true,
                    fakeState = "RINGING"
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/incoming_call_screen_light.png")
    }

    @Test
    fun active_call_screen_recording_state() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                com.example.ui.components.ActiveCallScreen(
                    contactName = "Jane Doe",
                    contactNumber = "555-0199",
                    preferredSim = "SIM 1",
                    quickResponses = listOf("Can't talk right now.", "I will call you back."),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = false,
                    recordingEnabled = true,
                    callNotesEnabled = true,
                    isFake = true,
                    fakeState = "ACTIVE"
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/active_call_screen_recording.png")
    }
}
