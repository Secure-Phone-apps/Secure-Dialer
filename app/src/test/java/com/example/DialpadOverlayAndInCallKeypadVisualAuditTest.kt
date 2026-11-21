package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.MainScreen
import com.example.ui.components.ActiveCallScreen
import com.example.ui.components.DialpadOverlay
import com.example.ui.components.DialpadTabContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DialpadOverlayAndInCallKeypadVisualAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(app).clearAllTables()
        }
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        CallManager.setMainActivityInForeground(false)
        CallManager.setInCallUiVisible(false)
    }

    private fun collectVisualDiagnostics(): List<String> {
        val issues = mutableListOf<String>()
        val textNodes = composeRule.onAllNodes(
            SemanticsMatcher("HasTextLayoutResult") { SemanticsActions.GetTextLayoutResult in it.config },
            useUnmergedTree = true
        ).fetchSemanticsNodes(atLeastOneRootRequired = false)

        val rootRight = try {
            composeRule.onRoot().fetchSemanticsNode().boundsInRoot.right
        } catch (_: Throwable) {
            Float.MAX_VALUE
        }

        for (node in textNodes) {
            val textList = if (SemanticsProperties.Text in node.config) {
                node.config[SemanticsProperties.Text].joinToString(" ") { it.text }
            } else ""
            val editableText = if (SemanticsProperties.EditableText in node.config) {
                node.config[SemanticsProperties.EditableText].text
            } else ""
            val label = textList.ifBlank { editableText }.ifBlank { continue }
            val tag = if (SemanticsProperties.TestTag in node.config) {
                " [${node.config[SemanticsProperties.TestTag]}]"
            } else ""

            val results = mutableListOf<TextLayoutResult>()
            if (node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results) == true) {
                for (layout in results) {
                    val hasEllipsis = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
                    if (hasEllipsis || layout.multiParagraph.didExceedMaxLines) {
                        issues.add("TEXT_OVERFLOW$tag: '$label' (didOverflowWidth=${layout.didOverflowWidth}, didOverflowHeight=${layout.didOverflowHeight})")
                    }
                }
            }

            var isScrollable = false
            var p: SemanticsNode? = node
            while (p != null) {
                if (SemanticsProperties.HorizontalScrollAxisRange in p.config) {
                    isScrollable = true
                    break
                }
                p = p.parent
            }
            val bounds = node.boundsInRoot
            if (!isScrollable && bounds.width > 0f && (bounds.left < -2f || bounds.right > rootRight + 2f)) {
                issues.add("BOUNDS_CLIP$tag: '$label' (left=${bounds.left}, right=${bounds.right}, rootRight=$rootRight)")
            }
        }
        return issues
    }

    @Test
    @Config(qualifiers = "w320dp-h560dp-port-hdpi", sdk = [34])
    fun compact320_dialpadTab_zeroOverflowAndReferenceCapture() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        viewModel.onDialpadInputChange("5550123456")

        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.requiredSize(320.dp, 560.dp)) {
                    DialpadTabContent(
                        inputValue = "5550123456",
                        onValueChange = {},
                        onCallClick = {},
                        onSpeedDialCall = {},
                        voicemailNumber = "",
                        speedDialMap = emptyMap(),
                        dialpadMatches = emptyList(),
                        onCollapseClick = {},
                        viewModel = viewModel
                    )
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialpad_create_contact_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_whatsapp_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/01_compact_dialpad_tab.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Compact DialpadTab overflow:\n${issues.joinToString("\n")}", issues.isEmpty())
    }

    @Test
    @Config(qualifiers = "w320dp-h560dp-port-hdpi", sdk = [34])
    fun compact320_dialpadOverlay_matchesTabParityZeroOverflowAndClipboardWorks() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        viewModel.onDialpadInputChange("5550123456")
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(320.dp, 560.dp)) {
                    DialpadOverlay(
                        inputValue = "5550123456",
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
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_create_contact_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_add_to_existing_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_whatsapp_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_key_5").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/02_compact_dialpad_overlay.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Compact DialpadOverlay overflow:\n${issues.joinToString("\n")}", issues.isEmpty())

        // Verify wired long-press clipboard menu opens Copy/Clear
        composeRule.onNodeWithTag("dialpad_overlay_number_container").performTouchInput {
            longClick(androidx.compose.ui.geometry.Offset(4f, centerY))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(app.getString(R.string.dialpad_copy)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.dialpad_clear)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-port-hdpi", sdk = [34])
    fun compact320_font150_dialpadOverlay_zeroOverflow() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        viewModel.onDialpadInputChange("5550123456")
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(320.dp, 640.dp)) {
                        DialpadOverlay(
                            inputValue = "5550123456",
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
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_whatsapp_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").performScrollTo().assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/03_compact_font150_dialpad_overlay.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Compact 1.5x DialpadOverlay overflow:\n${issues.joinToString("\n")}", issues.isEmpty())
    }

    @Test
    @Config(qualifiers = "w673dp-h841dp-port-hdpi", sdk = [34])
    fun foldablePortrait_mainScreenWithDialpadOverlay_clearsRailAndZeroOverflow() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        viewModel.isDialpadVisible.value = true
        viewModel.onDialpadInputChange("5550123456")
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(673.dp, 841.dp)) {
                    MainScreen(
                        viewModel = viewModel,
                        onShowRestrictedSettings = {},
                        isDefaultDialer = true
                    )
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        val railRight = composeRule.onNodeWithTag("app_navigation_rail").fetchSemanticsNode().boundsInRoot.right
        val overlayLeft = composeRule.onNodeWithTag("dialpad_overlay_number_field").fetchSemanticsNode().boundsInRoot.left
        assertTrue(
            "DialpadOverlay should stay clear of NavigationRail (railRight=$railRight, overlayLeft=$overlayLeft)",
            overlayLeft >= railRight
        )
        composeRule.onNodeWithTag("dialpad_call_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/04_foldable_main_dialpad_overlay.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h520dp-land-hdpi", sdk = [34])
    fun foldableLandscapeFont150_mainScreenWithDialpadOverlay_allKeysVisible() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        viewModel.isDialpadVisible.value = true
        viewModel.onDialpadInputChange("5550123456")
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(840.dp, 520.dp)) {
                        MainScreen(
                            viewModel = viewModel,
                            onShowRestrictedSettings = {},
                            isDefaultDialer = true
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialpad_key_5").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").performScrollTo().assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/05_foldable_land_font150_dialpad_overlay.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h560dp-port-hdpi", sdk = [34])
    fun compact320_inCallKeypad_accessible48dpClearAndZeroOverflow() {
        val keypadLabel = app.getString(R.string.keypad)
        var minTouchPx = 0f
        composeRule.setContent {
            minTouchPx = with(LocalDensity.current) { 48.dp.toPx() }
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(320.dp, 560.dp)) {
                    ActiveCallScreen(
                        contactName = "Alex Mercer",
                        contactNumber = "+1 555 0188",
                        preferredSim = "SIM 1",
                        quickResponses = listOf("Busy right now."),
                        onHangUp = {},
                        onQuickDecline = {},
                        isIncoming = false,
                        isFake = true,
                        fakeState = "ACTIVE"
                    )
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(keypadLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("in_call_keypad_input").assertIsDisplayed()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("5").performClick()
        composeRule.waitForIdle()

        val clearBounds = composeRule.onNodeWithTag("in_call_keypad_clear_button").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "InCallKeypad clear button must be >=48dp touch target (w=${clearBounds.width}, h=${clearBounds.height}, min=$minTouchPx)",
            clearBounds.width >= minTouchPx - 1f && clearBounds.height >= minTouchPx - 1f
        )
        composeRule.onNodeWithTag("in_call_keypad_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/06_compact_incall_keypad.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Compact InCallKeypad overflow:\n${issues.joinToString("\n")}", issues.isEmpty())
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-port-hdpi", sdk = [34])
    fun compact320_font150_inCallKeypad_noSlicedControlsAndZeroOverflow() {
        val keypadLabel = app.getString(R.string.keypad)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(320.dp, 640.dp)) {
                        ActiveCallScreen(
                            contactName = "Alex Mercer",
                            contactNumber = "+1 555 0188",
                            preferredSim = "SIM 1",
                            quickResponses = listOf("Busy right now."),
                            onHangUp = {},
                            onQuickDecline = {},
                            isIncoming = false,
                            isFake = true,
                            fakeState = "ACTIVE"
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(keypadLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("7").performClick()
        composeRule.onNodeWithText("9").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("in_call_keypad_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/07_compact_font150_incall_keypad.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Compact 1.5x InCallKeypad overflow:\n${issues.joinToString("\n")}", issues.isEmpty())
    }

    @Test
    @Config(qualifiers = "w673dp-h841dp-port-hdpi", sdk = [34])
    fun foldablePortrait_inCallKeypad_twoPaneZeroOverflow() {
        val keypadLabel = app.getString(R.string.keypad)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(673.dp, 841.dp)) {
                    ActiveCallScreen(
                        contactName = "Alex Mercer",
                        contactNumber = "+1 555 0188",
                        preferredSim = "SIM 1",
                        quickResponses = listOf("Busy right now."),
                        onHangUp = {},
                        onQuickDecline = {},
                        isIncoming = false,
                        isFake = true,
                        fakeState = "ACTIVE"
                    )
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(keypadLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("8").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("in_call_keypad_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/08_foldable_incall_keypad.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Foldable InCallKeypad overflow:\n${issues.joinToString("\n")}", issues.isEmpty())
    }

    @Test
    @Config(qualifiers = "w840dp-h520dp-land-hdpi", sdk = [34])
    fun foldableLandscapeFont150_inCallKeypad_twoPaneZeroOverflow() {
        val keypadLabel = app.getString(R.string.keypad)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(840.dp, 520.dp)) {
                        ActiveCallScreen(
                            contactName = "Alex Mercer",
                            contactNumber = "+1 555 0188",
                            preferredSim = "SIM 1",
                            quickResponses = listOf("Busy right now."),
                            onHangUp = {},
                            onQuickDecline = {},
                            isIncoming = false,
                            isFake = true,
                            fakeState = "ACTIVE"
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(keypadLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("in_call_keypad_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/dialpad_audit/09_foldable_land_font150_incall_keypad.png")
        val issues = collectVisualDiagnostics()
        assertTrue("Foldable Landscape 1.5x InCallKeypad overflow:\n${issues.joinToString("\n")}", issues.isEmpty())
    }
}
