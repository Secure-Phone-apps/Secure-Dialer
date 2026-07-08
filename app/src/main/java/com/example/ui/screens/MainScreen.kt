package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.model.CallRecord
import com.example.model.Contact
import androidx.compose.ui.graphics.Color
import com.example.ui.components.HeaderSearchBar

@Composable
fun MainScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    searchBg: Color,
    textStyleColor: Color,
    grayTextColor: Color,
    activePillColor: Color,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeaderSearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    onSettingsClick = onSettingsClick,
                    onProfileClick = onProfileClick,
                    searchBg = searchBg,
                    textStyleColor = textStyleColor,
                    grayTextColor = grayTextColor,
                    activePillColor = activePillColor
                )
                content()
            }
        }
    }
}
