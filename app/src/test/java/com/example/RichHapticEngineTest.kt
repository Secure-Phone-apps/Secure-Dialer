package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.RichHapticEngine
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RichHapticEngineTest {

    @Test
    fun `verify all HapticStyle enum variants exist and perform gracefully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)

        for (style in RichHapticEngine.HapticStyle.values()) {
            // Must execute gracefully without throwing any runtime exception
            RichHapticEngine.performHaptic(context, style)
        }
    }

    @Test
    fun `verify HapticStyle key tick and click tactile execution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.HEAVY_CLICK)
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.DOUBLE_TICK)
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
    }
}
