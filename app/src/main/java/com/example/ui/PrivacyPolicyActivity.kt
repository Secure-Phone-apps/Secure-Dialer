package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class PrivacyPolicyActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Privacy Policy") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "Last Updated: July 06, 2026",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            Text(
                                text = "At Secure Dialer, we prioritize your privacy. This policy outlines how your data is handled.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            // Section 1
                            PolicySection(
                                title = "1. Local Data Processing",
                                content = "We believe in data sovereignty. Your contacts, call logs, and personal calling history are never transmitted to our servers. All dialer functions and data processing occur exclusively on your device."
                            )
                            
                            // Section 2
                            Text(
                                text = "2. Data Usage",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Your information is used only to provide core dialer functionality:",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Column(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BulletPoint(boldText = "Contacts: ", text = "Accessed solely to map phone numbers to names within the app's local interface.")
                                BulletPoint(boldText = "Call Logs: ", text = "Accessed only to display your local call history.")
                                BulletPoint(boldText = "Microphone: ", text = "Used only when you are in an active call to transmit your voice to the party you are calling.")
                            }
                            
                            // Section 3
                            PolicySection(
                                title = "3. Data Sharing",
                                content = "We do not share, sell, or rent your data to any third parties, including advertisers or analytics providers."
                            )
                            
                            // Section 4
                            PolicySection(
                                title = "4. Your Control",
                                content = "You have full control over your data. Permissions can be revoked at any time through your device's system settings."
                            )
                            
                            // Section 5
                            PolicySection(
                                title = "5. Contact Us",
                                content = "If you have any questions or concerns, please email us at: movstore.online@gmail.com"
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PolicySection(title: String, content: String) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    @Composable
    private fun BulletPoint(boldText: String, text: String) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "•  ", style = MaterialTheme.typography.bodyLarge)
            Column {
                Text(
                    text = boldText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

