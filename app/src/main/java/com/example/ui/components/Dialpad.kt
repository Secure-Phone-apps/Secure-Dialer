package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact
import androidx.paging.compose.LazyPagingItems

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadTabContent(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    voicemailNumber: String,
    speedDialMap: Map<Int, String>,
    activePill: Color,
    searchBg: Color,
    primaryText: Color,
    grayText: Color,
    contactsPaged: LazyPagingItems<Contact>
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // T9 Results Preview
        if (inputValue.isNotEmpty()) {
            Text(
                text = "T9 Search",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = grayText,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(searchBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (contactsPaged.itemCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matches", fontSize = 12.sp, color = grayText)
                    }
                } else {
                    for (i in 0 until minOf(contactsPaged.itemCount, 3)) {
                        val contact = contactsPaged[i]
                        if (contact != null) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCallClick(contact.number) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(contact.avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(contact.avatarText, fontSize = 12.sp, color = contact.avatarTextColor)
                                }
                                Text(
                                    text = contact.name,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = primaryText
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Dial Pad",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = grayText,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 4.dp, start = 4.dp)
            )
        }

        // Elegant Display Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = inputValue.ifEmpty { "Enter Number" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = if (inputValue.isEmpty()) Color.Gray.copy(alpha = 0.6f) else primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dialer Grid
        val keys = listOf(
            Triple("1", "Voicemail", 1),
            Triple("2", "A B C", 2),
            Triple("3", "D E F", 3),
            Triple("4", "G H I", 4),
            Triple("5", "J K L", 5),
            Triple("6", "M N O", 6),
            Triple("7", "P Q R S", 7),
            Triple("8", "T U V", 8),
            Triple("9", "W X Y Z", 9),
            Triple("*", "", -1),
            Triple("0", "+", 0),
            Triple("#", "", -1)
        )

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (j in 0 until 3) {
                        val index = i * 3 + j
                        val key = keys[index]
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(searchBg)
                                .combinedClickable(
                                    onClick = { onValueChange(inputValue + key.first) },
                                    onLongClick = {
                                        if (key.third != -1) {
                                            val speedNum = speedDialMap[key.third]
                                            if (speedNum != null) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "📞 Calling Speed Dial mapped to key ${key.first}!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                                onSpeedDialCall(speedNum)
                                            } else {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Speed dial not assigned for key ${key.first}. Assign in Settings!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        }
                                    }
                                )
                                .testTag("dialpad_key_${key.first}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = key.first,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryText
                                )
                                if (key.second.isNotEmpty()) {
                                    Text(
                                        text = key.second,
                                        fontSize = 8.sp,
                                        color = grayText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Row (Call & Backspace inline)
        Row(
            modifier = Modifier
                .width(280.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty box for symmetry on the left
            Box(modifier = Modifier.size(56.dp))

            // Central green call button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onCallClick(inputValue) }
                    .testTag("dialpad_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place call",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Backspace button on the right
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inputValue.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange(inputValue.dropLast(1)) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(searchBg)
                    ) {
                        Text(
                            text = "⌫",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun FloatingDialpadButton(
    onClick: () -> Unit,
    activePillColor: Color
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(activePillColor)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("dialpad_fab"),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "⌨️", fontSize = 24.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadOverlay(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    onCallClick: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    speedDialMap: Map<Int, String>,
    navBg: Color,
    activePill: Color,
    searchBg: Color,
    primaryText: Color,
    grayText: Color,
    brandBlue: Color
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(navBg, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .shadow(16.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Swipe down handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClose() }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(36.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(activePill)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▼", fontSize = 12.sp, color = brandBlue, fontWeight = FontWeight.Bold)
                }
            }

            // Input Display Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = inputValue.ifEmpty { "Enter Number" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = if (inputValue.isEmpty()) Color.Gray else primaryText,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                if (inputValue.isNotEmpty()) {
                    IconButton(onClick = { onValueChange(inputValue.dropLast(1)) }) {
                        Text("⌫", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = grayText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dialer Grid
            val keys = listOf(
                Triple("1", "Voicemail", 1),
                Triple("2", "A B C", 2),
                Triple("3", "D E F", 3),
                Triple("4", "G H I", 4),
                Triple("5", "J K L", 5),
                Triple("6", "M N O", 6),
                Triple("7", "P Q R S", 7),
                Triple("8", "T U V", 8),
                Triple("9", "W X Y Z", 9),
                Triple("*", "", -1),
                Triple("0", "+", 0),
                Triple("#", "", -1)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (j in 0 until 3) {
                            val index = i * 3 + j
                            val key = keys[index]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(searchBg)
                                    .combinedClickable(
                                        onClick = { onValueChange(inputValue + key.first) },
                                        onLongClick = {
                                            if (key.first == "1") {
                                                Toast
                                                    .makeText(context, "📞 Calling Voicemail", Toast.LENGTH_SHORT)
                                                    .show()
                                                // onSpeedDialCall(voicemailNumber)
                                            } else if (key.third != -1) {
                                                val speedNum = speedDialMap[key.third]
                                                if (speedNum != null) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "📞 Calling Speed Dial mapped to key ${key.first}!",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                    onSpeedDialCall(speedNum)
                                                } else {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Speed dial not assigned for key ${key.first}. Assign in Settings!",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                }
                                            }
                                        }
                                    )
                                    .testTag("dialpad_key_${key.first}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = key.first,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                    if (key.second.isNotEmpty()) {
                                        Text(
                                            text = key.second,
                                            fontSize = 9.sp,
                                            color = grayText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Green Call Action Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onCallClick(inputValue) }
                    .testTag("dialpad_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place call",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
