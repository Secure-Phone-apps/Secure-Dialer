package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.DialerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun `dialpad input updates correctly`() {
        viewModel.dialpadInput.value = "123"
        assertEquals("123", viewModel.dialpadInput.value)
    }
}
