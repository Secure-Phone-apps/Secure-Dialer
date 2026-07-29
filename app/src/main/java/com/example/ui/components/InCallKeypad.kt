package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.CallManager

@Composable
fun InCallKeypad(
    onClose: () -> Unit
) {
    var inCallDialpadInput by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = inCallDialpadInput.ifEmpty { "Dialpad" },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            val inCallKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (c in 0 until 3) {
                            val key = inCallKeys[r * 3 + c]
                            Surface(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 48.dp)
                                    .clickable {
                                        inCallDialpadInput += key
                                        CallManager.playDtmf(key[0])
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(key, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
        }
    }
}
