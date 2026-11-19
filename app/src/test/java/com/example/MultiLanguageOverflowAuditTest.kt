/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.MainScreen
import com.example.ui.components.ActiveCallScreen
import com.example.ui.components.BatterySurvivabilitySettings
import com.example.ui.components.CallingAccountsSettings
import com.example.ui.components.DialpadOverlay
import com.example.ui.components.DialpadTabContent
import com.example.ui.components.SimSelectDialog
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
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-port-hdpi", sdk = [34])
class MultiLanguageOverflowAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application

    private val allLocales = listOf("tr", "de", "fr", "es", "pt", "pl", "ar", "hi", "ja", "zh", "en")

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

    private fun createLocalizedContext(base: Context, langTag: String): Pair<Context, Configuration> {
        val locale = Locale.forLanguageTag(langTag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val ctx = base.createConfigurationContext(config)
        return ctx to config
    }

    private fun findVisualOverflows(): List<String> {
        val overflows = mutableListOf<String>()
        val nodes = composeRule.onAllNodes(
            SemanticsMatcher("HasTextLayoutResult") { node ->
                SemanticsActions.GetTextLayoutResult in node.config
            },
            useUnmergedTree = true
        ).fetchSemanticsNodes(atLeastOneRootRequired = false)

        val rootNode: SemanticsNode? = try {
            composeRule.onRoot().fetchSemanticsNode()
        } catch (_: Throwable) {
            null
        }
        val rootRight = rootNode?.boundsInRoot?.right ?: Float.MAX_VALUE

        for (node in nodes) {
            val textList = if (SemanticsProperties.Text in node.config) {
                node.config[SemanticsProperties.Text].joinToString(" ") { it.text }
            } else {
                ""
            }
            val editableText = if (SemanticsProperties.EditableText in node.config) {
                node.config[SemanticsProperties.EditableText].text
            } else {
                ""
            }
            val label = textList.ifBlank { editableText }.ifBlank { continue }
            val tag = if (SemanticsProperties.TestTag in node.config) {
                " [${node.config[SemanticsProperties.TestTag]}]"
            } else {
                ""
            }

            val results = mutableListOf<TextLayoutResult>()
            val action = node.config[SemanticsActions.GetTextLayoutResult].action
            val success = action?.invoke(results) == true
            if (success && results.isNotEmpty()) {
                for (layout in results) {
                    val hasEllipsis = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
                    val exceededMaxLines = layout.multiParagraph.didExceedMaxLines
                    val zeroDimension = (layout.size.width == 0 || layout.size.height == 0) && label.isNotBlank() && !layout.multiParagraph.didExceedMaxLines

                    if (hasEllipsis || exceededMaxLines || zeroDimension) {
                        overflows.add(
                            "TEXT_OVERFLOW$tag: '$label' (didOverflowWidth=${layout.didOverflowWidth}, ellipsis=$hasEllipsis, exceededMaxLines=$exceededMaxLines)"
                        )
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
                overflows.add(
                    "BOUNDS_CLIP$tag: '$label' (left=${bounds.left}, right=${bounds.right}, rootRight=$rootRight)"
                )
            }
        }
        return overflows
    }

    @Test
    @Config(qualifiers = "tr-w320dp-h640dp-port-hdpi", sdk = [34])
    fun turkish_compact320_font150_mainScreen_zeroOverflowAndScreenshot() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
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
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_main_screen.png")

        val overflows = findVisualOverflows()
        assertTrue("Turkish MainScreen overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "tr-w320dp-h640dp-port-hdpi", sdk = [34])
    fun turkish_compact320_font150_dialpadOverlay_zeroOverflowAndScreenshot() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 560.dp)) {
                        DialpadOverlay(
                            inputValue = "5550123",
                            onValueChange = {},
                            onClose = {},
                            onCallClick = {},
                            onSpeedDialCall = {},
                            speedDialMap = emptyMap(),
                            voicemailNumber = ""
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_dialpad_overlay.png")

        val overflows = findVisualOverflows()
        assertTrue("Turkish DialpadOverlay overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "tr-w320dp-h640dp-port-hdpi", sdk = [34])
    fun turkish_compact320_font150_dialpadTab_zeroOverflowAndScreenshot() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 560.dp)) {
                        DialpadTabContent(
                            inputValue = "5550199",
                            onValueChange = {},
                            onCallClick = {},
                            onSpeedDialCall = {},
                            voicemailNumber = "",
                            speedDialMap = emptyMap(),
                            dialpadMatches = emptyList(),
                            onCollapseClick = {}
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_dialpad_tab.png")

        val overflows = findVisualOverflows()
        assertTrue("Turkish DialpadTab overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "tr-w320dp-h640dp-port-hdpi", sdk = [34])
    fun turkish_compact320_font150_activeCall_zeroOverflowAndScreenshot() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 640.dp)) {
                        ActiveCallScreen(
                            contactName = "Mehmet Yilmaz",
                            contactNumber = "+90 555 0199",
                            preferredSim = "SIM 1",
                            quickResponses = listOf("Şu an müsait değilim."),
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
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_active_call.png")

        val overflows = findVisualOverflows()
        assertTrue("Turkish ActiveCallScreen overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "tr-w320dp-h640dp-port-hdpi", sdk = [34])
    fun turkish_compact320_font150_callingAccountsAndBattery_zeroOverflowAndScreenshot() {
        val viewModel = DialerViewModel(app)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.requiredSize(width = 320.dp, height = 640.dp)
                    ) {
                        CallingAccountsSettings(
                            viewModel = viewModel,
                            cardBgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_calling_accounts.png")

        val overflows = findVisualOverflows()
        assertTrue("Turkish CallingAccountsSettings overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "tr-w320dp-h760dp-port-hdpi", sdk = [34])
    fun turkish_compact320_font150_batteryGuide_zeroOverflowAndScreenshot() {
        val viewModel = DialerViewModel(app)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.requiredSize(width = 320.dp, height = 760.dp)
                    ) {
                        BatterySurvivabilitySettings(
                            viewModel = viewModel,
                            cardBgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_battery_guide.png")

        val overflows = findVisualOverflows()
        assertTrue("Turkish BatterySurvivabilitySettings overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "de-w320dp-h640dp-port-hdpi", sdk = [34])
    fun german_compact320_font150_keyScreens_zeroOverflowAndScreenshot() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
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
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/de_320dp_150_main_screen.png")

        val overflows = findVisualOverflows()
        assertTrue("German MainScreen overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    @Config(qualifiers = "ar-w320dp-h640dp-port-hdpi", sdk = [34])
    fun arabicRtl_compact320_font150_mainScreen_zeroOverflowAndScreenshot() {
        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f),
                LocalLayoutDirection provides LayoutDirection.Rtl
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
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/ar_320dp_150_main_screen.png")

        val overflows = findVisualOverflows()
        assertTrue("Arabic RTL MainScreen overflows at 320dp @ 1.5x:\n${overflows.joinToString("\n")}", overflows.isEmpty())
    }

    @Test
    fun allLocales_compact320_font150_dialpadOverlayAndSimDialog_zeroOverflow() {
        val currentLocaleTag = androidx.compose.runtime.mutableStateOf("en")
        val showSimDialog = androidx.compose.runtime.mutableStateOf(false)
        val viewModel = DialerViewModel(app)

        composeRule.setContent {
            val tag = currentLocaleTag.value
            val (locCtx, locConfig) = rememberLocaleResources(app, tag)
            val dir = if (tag == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides locCtx,
                LocalConfiguration provides locConfig,
                LocalLayoutDirection provides dir,
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 600.dp)) {
                        if (!showSimDialog.value) {
                            DialpadOverlay(
                                inputValue = "5550123",
                                onValueChange = {},
                                onClose = {},
                                onCallClick = {},
                                onSpeedDialCall = {},
                                speedDialMap = emptyMap(),
                                voicemailNumber = "",
                                viewModel = viewModel
                            )
                        } else {
                            SimSelectDialog(
                                context = locCtx,
                                viewModel = viewModel,
                                pendingCallNumber = "+1 555-0100",
                                onDismiss = {},
                                onSimSelected = {}
                            )
                        }
                    }
                }
            }
        }

        val failuresByLocale = mutableListOf<String>()
        for (lang in allLocales) {
            showSimDialog.value = false
            currentLocaleTag.value = lang
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            if (lang in listOf("fr", "es", "pl", "pt")) {
                composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/${lang}_320dp_150_dialpad_overlay.png")
            }
            val overlayOverflows = findVisualOverflows()
            for (o in overlayOverflows) {
                failuresByLocale.add("[$lang DialpadOverlay] $o")
            }

            showSimDialog.value = true
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            if (lang == "tr") {
                composeRule.onRoot().captureRoboImage("src/test/screenshots/overflow/tr_320dp_150_sim_dialog.png")
            }
            val simOverflows = findVisualOverflows()
            for (o in simOverflows) {
                failuresByLocale.add("[$lang SimSelectDialog] $o")
            }
        }
        assertTrue(
            "Multi-locale DialpadOverlay/SimSelectDialog overflows at 320dp @ 1.5x:\n${failuresByLocale.joinToString("\n")}",
            failuresByLocale.isEmpty()
        )
    }

    @androidx.compose.runtime.Composable
    private fun rememberLocaleResources(base: Context, langTag: String): Pair<Context, Configuration> {
        return androidx.compose.runtime.remember(langTag) {
            createLocalizedContext(base, langTag)
        }
    }
}
