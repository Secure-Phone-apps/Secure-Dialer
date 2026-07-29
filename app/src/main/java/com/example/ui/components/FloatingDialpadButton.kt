package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FloatingDialpadButton(
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    FloatingActionButton(
        onClick = onClick,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.testTag("dialpad_fab")
    ) {
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Open Dialpad",
            modifier = Modifier.size(24.dp)
        )
    }
}
