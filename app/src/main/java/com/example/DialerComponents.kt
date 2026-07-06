package com.example

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 1. HEADER / SEARCH BAR
@Composable
fun HeaderSearchBar(
  searchQuery: String,
  onQueryChange: (String) -> Unit,
  onSettingsClick: () -> Unit,
  onProfileClick: () -> Unit,
  searchBg: Color,
  textStyleColor: Color,
  grayTextColor: Color,
  activePillColor: Color
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .height(52.dp)
        .clip(RoundedCornerShape(26.dp))
        .background(searchBg)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "🔍",
        fontSize = 18.sp,
        modifier = Modifier.padding(end = 12.dp)
      )
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart
      ) {
        if (searchQuery.isEmpty()) {
          Text(
            text = "search contacts & numbers",
            color = grayTextColor,
            fontSize = 15.sp
          )
        }
        BasicTextFieldDisplay(
          value = searchQuery,
          onValueChange = onQueryChange,
          textColor = textStyleColor
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Direct Settings Gear button
        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier.size(36.dp).testTag("settings_gear_button")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = grayTextColor,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

@Composable
fun BasicTextFieldDisplay(
  value: String,
  onValueChange: (String) -> Unit,
  textColor: Color
) {
  androidx.compose.foundation.text.BasicTextField(
    value = value,
    onValueChange = onValueChange,
    textStyle = androidx.compose.ui.text.TextStyle(
      color = textColor,
      fontSize = 15.sp
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("search_input")
  )
}

// 2. FAVORITES TAB
@Composable
fun FavoritesTabContent(
  contacts: List<Contact>,
  onCallClick: (String, String) -> Unit,
  onToggleFavorite: (Contact) -> Unit,
  primaryText: Color,
  secondaryText: Color,
  activePill: Color
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "Favorites",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = secondaryText,
      modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )

    if (contacts.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("⭐", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
          Text("No favorites added yet", fontWeight = FontWeight.Medium, color = secondaryText)
          Text("Star contacts to access them instantly", fontSize = 12.sp, color = secondaryText)
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(contacts, key = { "${it.name}_${it.number}" }) { contact ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onCallClick(contact.name, contact.number) }
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(contact.avatarBg),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = contact.avatarTextColor
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = contact.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
              )
              Text(
                text = "${contact.label} • ${contact.number}",
                fontSize = 13.sp,
                color = secondaryText
              )
            }

            // Unstar button
            IconButton(onClick = { onToggleFavorite(contact) }) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Unstar",
                tint = Color(0xFFEAB308) // Nice Material yellow
              )
            }

            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(activePill)
                .clickable { onCallClick(contact.name, contact.number) },
              contentAlignment = Alignment.Center
            ) {
              Text("📞", fontSize = 16.sp)
            }
          }
        }
      }
    }
  }
}

// 3. RECENTS TAB
@Composable
fun RecentsTabContent(
  callRecords: List<CallRecord>,
  onCallClick: (CallRecord) -> Unit,
  onDeleteRecord: (Int) -> Unit,
  primaryText: Color,
  secondaryText: Color,
  activePill: Color,
  brandBlue: Color,
  hasPermission: Boolean = true,
  isLoading: Boolean = false,
  onRequestPermission: () -> Unit = {}
) {
  var expandedRecordId by remember { mutableStateOf<Int?>(null) }
  var isPlayingVoicemail by remember { mutableStateOf(false) }
  var voicemailProgress by remember { mutableStateOf(0.0f) }
  var currentPlayingId by remember { mutableStateOf<Int?>(null) }

  LaunchedEffect(isPlayingVoicemail, currentPlayingId) {
    if (isPlayingVoicemail && currentPlayingId != null) {
      while (voicemailProgress < 1.0f) {
        delay(100)
        voicemailProgress += 0.04f
      }
      isPlayingVoicemail = false
      voicemailProgress = 0.0f
    }
  }

  // Group call history records by number, preserving newest first order
  val groupedRecords = remember(callRecords) {
    val groups = mutableListOf<Pair<CallRecord, List<CallRecord>>>()
    val seenNumbers = mutableSetOf<String>()
    for (record in callRecords) {
      if (record.number !in seenNumbers) {
        seenNumbers.add(record.number)
        val sameNumberRecords = callRecords.filter { it.number == record.number }
        groups.add(record to sameNumberRecords)
      }
    }
    groups
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp, start = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Recents",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = secondaryText,
        modifier = Modifier.testTag("recents_header")
      )
      Text(
        text = "Clear All",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = brandBlue,
        modifier = Modifier.clickable {
          callRecords.forEach { onDeleteRecord(it.id) }
        }
      )
    }

    if (!hasPermission && !isLoading) {
      androidx.compose.material3.Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = activePill.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("Permissions Required", fontWeight = FontWeight.Bold, color = brandBlue, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "To access, load, and call the real call history on your phone, please enable the Call Log Permission.",
            fontSize = 11.sp,
            color = secondaryText,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Text("Enable Call Log", fontSize = 11.sp, color = Color.White)
          }
        }
      }
    }

    if (groupedRecords.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("🕒", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
          Text("Your call log is empty", fontWeight = FontWeight.Medium, color = secondaryText)
          Text("Recent calls will show up here", fontSize = 12.sp, color = secondaryText)
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(groupedRecords, key = { it.first.id }) { (record, sameNumberRecords) ->
          val isExpanded = expandedRecordId == record.id

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(if (isExpanded) secondaryText.copy(alpha = 0.06f) else Color.Transparent)
              .clickable {
                if (isExpanded) {
                  expandedRecordId = null
                  isPlayingVoicemail = false
                  voicemailProgress = 0.0f
                  currentPlayingId = null
                } else {
                  expandedRecordId = record.id
                  isPlayingVoicemail = false
                  voicemailProgress = 0.0f
                  currentPlayingId = record.id
                }
              }
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(record.avatarBg),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = record.avatarText,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium,
                  color = record.avatarTextColor
                )
              }

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (sameNumberRecords.size > 1) "${record.name} (${sameNumberRecords.size})" else record.name,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium,
                  color = primaryText
                )
                if (record.number.isNotEmpty()) {
                  Text(
                    text = record.number,
                    fontSize = 14.sp,
                    color = secondaryText
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  val (arrow, arrowColor) = when (record.type) {
                    CallType.MISSED -> "↙" to Color.Red
                    CallType.OUTGOING -> "↗" to Color(0xFF16A34A)
                    CallType.INCOMING -> "↔" to Color.Gray
                  }
                  Text(text = arrow, color = arrowColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  Text(
                    text = "${record.label} • ${record.timestamp}",
                    fontSize = 13.sp,
                    color = secondaryText
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(activePill)
                    .clickable { onCallClick(record) },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }

            AnimatedVisibility(
              visible = isExpanded,
              enter = expandVertically() + fadeIn(),
              exit = androidx.compose.animation.shrinkVertically() + fadeOut()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp, start = 64.dp, end = 8.dp)
              ) {
                HorizontalDivider(
                  color = secondaryText.copy(alpha = 0.12f),
                  thickness = 1.dp,
                  modifier = Modifier.padding(bottom = 12.dp)
                )

                // Call History Header
                Text(
                  text = "Call History (${sameNumberRecords.size})",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = secondaryText,
                  modifier = Modifier.padding(bottom = 8.dp)
                )

                sameNumberRecords.forEachIndexed { index, subRecord ->
                  Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        val (arrow, arrowColor) = when (subRecord.type) {
                          CallType.MISSED -> "↙" to Color.Red
                          CallType.OUTGOING -> "↗" to Color(0xFF16A34A)
                          CallType.INCOMING -> "↔" to Color.Gray
                        }
                        Text(text = arrow, color = arrowColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                          text = subRecord.type.name.lowercase().replaceFirstChar { it.uppercase() },
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Medium,
                          color = primaryText
                        )
                        Text(
                          text = "• ${subRecord.timestamp}",
                          fontSize = 13.sp,
                          color = secondaryText
                        )
                      }

                      IconButton(
                        onClick = { onDeleteRecord(subRecord.id) },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete",
                          tint = Color.Red.copy(alpha = 0.6f),
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }

                    Text(
                      text = "Duration: ${if (subRecord.type == CallType.MISSED) "0s (Missed)" else "${subRecord.duration / 60} mins ${subRecord.duration % 60} secs"}",
                      fontSize = 12.sp,
                      color = secondaryText,
                      modifier = Modifier.padding(start = 18.dp, bottom = 4.dp)
                    )

                    // Show voicemail player if this specific subRecord is a voicemail
                    if (subRecord.hasVoicemail) {
                      Card(
                        colors = CardDefaults.cardColors(containerColor = secondaryText.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(start = 18.dp, top = 4.dp, bottom = 4.dp)
                      ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                          ) {
                            IconButton(
                              onClick = {
                                if (currentPlayingId == subRecord.id) {
                                  isPlayingVoicemail = !isPlayingVoicemail
                                } else {
                                  currentPlayingId = subRecord.id
                                  voicemailProgress = 0.0f
                                  isPlayingVoicemail = true
                                }
                              },
                              modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(activePill)
                            ) {
                              Text(if (isPlayingVoicemail && currentPlayingId == subRecord.id) "⏸️" else "▶️", fontSize = 11.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                              Text(
                                text = "Voicemail Message",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              LinearProgressIndicator(
                                progress = { if (currentPlayingId == subRecord.id) voicemailProgress else 0.0f },
                                modifier = Modifier
                                  .fillMaxWidth()
                                  .height(3.dp)
                                  .clip(RoundedCornerShape(1.5.dp)),
                                color = activePill,
                                trackColor = secondaryText.copy(alpha = 0.15f)
                              )
                            }
                          }

                          Spacer(modifier = Modifier.height(4.dp))
                          Text(
                            text = "Transcript:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText
                          )
                          Text(
                            text = when (subRecord.number) {
                              "+1 (555) 013-1122" -> "\"Hey there, just checking in. I wanted to see if you have the files for the marketing campaign ready? Give me a call back when you can. Bye!\""
                              "+1 (555) 011-9988" -> "\"Hello, this is a call from utility services regarding your bill update. Please press 1 or call back at your earliest convenience.\""
                              else -> "\"Hey! I missed you. Let me know when you're free to chat. Talk to you soon!\""
                            },
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = primaryText,
                            modifier = Modifier.padding(top = 1.dp)
                          )
                        }
                      }
                    }

                    if (index < sameNumberRecords.lastIndex) {
                      HorizontalDivider(
                        color = secondaryText.copy(alpha = 0.06f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                      )
                    }
                  }
                }

                // Interactive Call Actions
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Button(
                    onClick = { onCallClick(record) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = activePill,
                      contentColor = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("Call Back", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = {
                      sameNumberRecords.forEach { onDeleteRecord(it.id) }
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFFDC2626), // Solid bright red
                      contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("Delete All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// 4. CONTACTS TAB
@Composable
fun ContactsTabContent(
  contacts: List<Contact>,
  onCallClick: (Contact) -> Unit,
  onAddContactClick: () -> Unit,
  onToggleFavorite: (Contact) -> Unit,
  primaryText: Color,
  secondaryText: Color,
  activePill: Color,
  brandBlue: Color,
  hasPermission: Boolean = true,
  isLoading: Boolean = false,
  onRequestPermission: () -> Unit = {},
  onEditContact: (Contact) -> Unit = {},
  onDeleteContact: (Contact) -> Unit = {}
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    if (!hasPermission && !isLoading) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = activePill.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("Permissions Required", fontWeight = FontWeight.Bold, color = brandBlue, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "To access, load, edit, and call the real contacts on your phone, please enable the Contacts Permission.",
            fontSize = 11.sp,
            color = secondaryText,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Text("Enable Phone Contacts", fontSize = 11.sp, color = Color.White)
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp, start = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Contacts",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = secondaryText
      )
      Button(
        onClick = onAddContactClick,
        colors = ButtonDefaults.buttonColors(
          containerColor = activePill,
          contentColor = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp).testTag("add_contact_button")
      ) {
        Text("+ Add New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    if (contacts.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("👥", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
          Text("No contacts found", fontWeight = FontWeight.Medium, color = secondaryText)
          Text("Tap '+ Add New' to create a contact", fontSize = 12.sp, color = secondaryText)
        }
      }
    } else {
      val favorites = contacts.filter { it.favorite }

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        if (favorites.isNotEmpty()) {
          item {
            Text(
              text = "⭐ Favorites",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = secondaryText,
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
              items(favorites, key = { "${it.name}_${it.number}" }) { contact ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .width(72.dp)
                    .clickable { onCallClick(contact) }
                ) {
                  Box(
                    modifier = Modifier
                      .size(56.dp)
                      .clip(CircleShape)
                      .background(contact.avatarBg),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = contact.avatarTextColor
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = contact.name.split(" ").firstOrNull() ?: contact.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
            HorizontalDivider(
              color = secondaryText.copy(alpha = 0.12f),
              thickness = 1.dp,
              modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
          }
        }

        item {
          Text(
            text = "All Contacts",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
          )
        }

        items(contacts, key = { "${it.name}_${it.number}" }) { contact ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onCallClick(contact) }
              .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(contact.avatarBg),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = contact.avatarTextColor
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = contact.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
              )
              Text(
                text = "${contact.label} • ${contact.number}",
                fontSize = 13.sp,
                color = secondaryText
              )
            }

            // Star/Unstar toggle
            IconButton(
              onClick = { onToggleFavorite(contact) },
              modifier = Modifier.width(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite Toggle",
                tint = if (contact.favorite) Color(0xFFEAB308) else Color.LightGray
              )
            }

            // More options dropdown
            var menuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(horizontal = 0.dp)) {
              IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.width(30.dp)
              ) {
                Text("⋮", fontSize = 18.sp, color = secondaryText, fontWeight = FontWeight.Bold)
              }
              DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
              ) {
                DropdownMenuItem(
                  text = { Text("✏️ Edit Contact") },
                  onClick = {
                    menuExpanded = false
                    onEditContact(contact)
                  }
                )
                DropdownMenuItem(
                  text = { Text("🗑️ Delete Contact") },
                  onClick = {
                    menuExpanded = false
                    onDeleteContact(contact)
                  }
                )
              }
            }

            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(activePill)
                .clickable { onCallClick(contact) },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                tint = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

// 5. VOICEMAIL TAB
@Composable
fun VoicemailTabContent(
  voicemailRecords: List<CallRecord>,
  onPlayClick: (CallRecord) -> Unit,
  primaryText: Color,
  secondaryText: Color,
  activePill: Color,
  navBg: Color
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "Voicemail",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = secondaryText,
      modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )

    if (voicemailRecords.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("📼", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
          Text("No voicemails", fontWeight = FontWeight.Medium, color = secondaryText)
          Text("You are all caught up", fontSize = 12.sp, color = secondaryText)
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(voicemailRecords, key = { it.id }) { record ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = navBg)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(record.avatarBg),
                  contentAlignment = Alignment.Center
                ) {
                  Text(record.avatarText, fontWeight = FontWeight.Bold, color = record.avatarTextColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(record.name, fontWeight = FontWeight.Medium, color = primaryText)
                  Text("${record.timestamp} • 0:24 mins", fontSize = 12.sp, color = secondaryText)
                }
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(activePill)
                    .clickable { onPlayClick(record) },
                  contentAlignment = Alignment.Center
                ) {
                  Text("▶️", fontSize = 16.sp)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "Transcript: \"Hey, just calling to catch up. Let me know when you're free this evening! Standard billing rates may apply.\"",
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

// 6. BOTTOM NAVIGATION BAR
@Composable
fun BottomNavBar(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit,
  navBg: Color,
  navBorder: Color,
  activePill: Color,
  primaryText: Color,
  grayText: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(80.dp)
      .background(navBg)
      .drawBehind {
        val borderHeight = 1.dp.toPx()
        drawLine(
          color = navBorder,
          start = androidx.compose.ui.geometry.Offset(0f, 0f),
          end = androidx.compose.ui.geometry.Offset(size.width, 0f),
          strokeWidth = borderHeight
        )
      }
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceAround
  ) {
    val items = listOf(
      Triple("Recents", Icons.AutoMirrored.Filled.List, "recents_tab"),
      Triple("Contacts", Icons.Default.Person, "contacts_tab"),
      Triple("Dialpad", Icons.Default.Call, "dialpad_tab")
    )

    items.forEachIndexed { index, (label, icon, tag) ->
      val isSelected = selectedTab == index
      Column(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) { onTabSelected(index) }
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(width = 64.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) activePill else Color.Transparent),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) (if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49)) else grayText,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = label,
          fontSize = 12.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) primaryText else grayText
        )
      }
    }
  }
}

// 7. DIALPAD INLINE TAB CONTENT
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
  contactsList: List<Contact>
) {
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Dial Pad",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = grayText,
      modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp, start = 4.dp)
    )

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
                        Toast.makeText(context, "📞 Calling Speed Dial mapped to key ${key.first}!", Toast.LENGTH_SHORT).show()
                        onSpeedDialCall(speedNum)
                      } else {
                        Toast.makeText(context, "Speed dial not assigned for key ${key.first}. Assign in Settings!", Toast.LENGTH_SHORT).show()
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
      modifier = Modifier.width(280.dp).fillMaxWidth(),
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
          .background(Color(0xFF16A34A))
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

// 7. FLOATING DIALPAD ACTION BUTTON
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

// 8. DIALPAD OVERLAY (Sliding T9 Dialpad exactly like Google Dialer)
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
                        Toast.makeText(context, "📞 Calling Voicemail", Toast.LENGTH_SHORT).show()
                        // onSpeedDialCall(voicemailNumber)
                      } else if (key.third != -1) {
                        val speedNum = speedDialMap[key.third]
                        if (speedNum != null) {
                          Toast.makeText(context, "📞 Calling Speed Dial mapped to key ${key.first}!", Toast.LENGTH_SHORT).show()
                          onSpeedDialCall(speedNum)
                        } else {
                          Toast.makeText(context, "Speed dial not assigned for key ${key.first}. Assign in Settings!", Toast.LENGTH_SHORT).show()
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
          .background(Color(0xFF16A34A))
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

// 9. OUTGOING / ACTIVE CALL SCREEN WITH QUICK DECLINE
@Composable
fun ActiveCallScreen(
  contactName: String,
  contactNumber: String,
  preferredSim: String,
  quickResponses: List<String>,
  onHangUp: () -> Unit,
  onQuickDecline: (String) -> Unit,
  isDarkTheme: Boolean = true,
  isIncoming: Boolean = false,
  contacts: List<Contact> = emptyList(),
  activePill: Color = Color.Unspecified,
  onAnswer: () -> Unit = {},
  callState: Int = android.telecom.Call.STATE_DISCONNECTED
) {
  var callDuration by remember { mutableStateOf(0) }
  var isMuted by remember { mutableStateOf(false) }
  var isSpeakerOn by remember { mutableStateOf(false) }
  var isBluetoothOn by remember { mutableStateOf(false) }
  var isOnHold by remember { mutableStateOf(false) }
  var isQuickDeclineMenuOpen by remember { mutableStateOf(false) }
  var isInCallDialpadOpen by remember { mutableStateOf(false) }
  var inCallDialpadInput by remember { mutableStateOf("") }
  var isRecording by remember { mutableStateOf(false) }
  var isAddCallDialogOpen by remember { mutableStateOf(false) }
  var addCallNumberInput by remember { mutableStateOf("") }
  var selectedAddCallContactName by remember { mutableStateOf("") }
  var participants by remember(contactName, contactNumber) {
    mutableStateOf(listOf(Pair(contactName, contactNumber)))
  }

  val context = LocalContext.current

  // Manage standard audio routing cleanup when ActiveCallScreen is entered and disposed
  DisposableEffect(Unit) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val originalMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
    val originalSpeaker = audioManager?.isSpeakerphoneOn ?: false
    val originalMute = audioManager?.isMicrophoneMute ?: false
    onDispose {
      try {
        audioManager?.let { am ->
          am.isMicrophoneMute = originalMute
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            am.clearCommunicationDevice()
          }
          am.isSpeakerphoneOn = originalSpeaker
          am.mode = originalMode
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  // DTMF Tone Generator for In-Call Keypad
  val inCallToneGenerator = remember {
    try {
      ToneGenerator(AudioManager.STREAM_DTMF, 80)
    } catch (e: Exception) {
      null
    }
  }

  fun playInCallDtmf(key: String) {
    inCallToneGenerator?.let {
      val tone = when (key) {
        "1" -> ToneGenerator.TONE_DTMF_1
        "2" -> ToneGenerator.TONE_DTMF_2
        "3" -> ToneGenerator.TONE_DTMF_3
        "4" -> ToneGenerator.TONE_DTMF_4
        "5" -> ToneGenerator.TONE_DTMF_5
        "6" -> ToneGenerator.TONE_DTMF_6
        "7" -> ToneGenerator.TONE_DTMF_7
        "8" -> ToneGenerator.TONE_DTMF_8
        "9" -> ToneGenerator.TONE_DTMF_9
        "0" -> ToneGenerator.TONE_DTMF_0
        "*" -> ToneGenerator.TONE_DTMF_S
        "#" -> ToneGenerator.TONE_DTMF_P
        else -> -1
      }
      if (tone != -1) {
        it.startTone(tone, 120)
      }
    }
  }

  LaunchedEffect(key1 = callState) {
    if (callState == android.telecom.Call.STATE_ACTIVE) {
      while (true) {
        delay(1000)
        callDuration++
      }
    }
  }

  val formattedTime = String.format("%02d:%02d", callDuration / 60, callDuration % 60)

  // Color theme adapters
  val backgroundColor = if (isDarkTheme) Color(0xFF111318) else Color(0xFFF4F6FA)
  val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1C1E)
  val subTextColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.8f) else Color(0xFF43474E)
  val cardContainerColor = if (isDarkTheme) Color(0xFF1E2025) else Color(0xFFE1E2EC)
  val listBgColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = backgroundColor
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Status Info
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 40.dp)
      ) {
        val displayHeader = if (isOnHold) {
          "Call on hold"
        } else if (participants.size > 1) {
          "Conference call via $preferredSim"
        } else {
          "Ongoing call via $preferredSim"
        }

        Text(
          text = displayHeader,
          color = subTextColor,
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          if (isRecording) {
            Box(
              modifier = Modifier
                .padding(end = 8.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Red)
            )
          }

          val displayName = if (participants.size > 1) {
            if (participants.size == 2) {
              "${participants[0].first.ifEmpty { participants[0].second }} & ${participants[1].first.ifEmpty { participants[1].second }}"
            } else {
              "Conference (${participants.size})"
            }
          } else {
            contactName.ifEmpty { contactNumber }
          }

          Text(
            text = displayName,
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val displaySubtitle = if (participants.size > 1) {
          if (participants.size > 2) {
            participants.joinToString(", ") { it.first.ifEmpty { it.second } }
          } else {
            "${participants[0].second} • ${participants[1].second}"
          }
        } else {
          if (contactName.isNotEmpty() && contactNumber.isNotEmpty()) contactNumber else ""
        }

        if (displaySubtitle.isNotEmpty()) {
          Text(
            text = displaySubtitle,
            color = subTextColor.copy(alpha = 0.7f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = formattedTime,
          color = textColor,
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // Middle Call Screen options
      if (isIncoming && isQuickDeclineMenuOpen) {
        Card(
          colors = CardDefaults.cardColors(containerColor = cardContainerColor),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          shape = RoundedCornerShape(24.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Select Quick Rejection Message:",
              color = textColor,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(quickResponses, key = { it }) { resp ->
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(listBgColor)
                    .clickable { onQuickDecline(resp) }
                    .padding(12.dp)
                ) {
                  Text(resp, color = textColor, fontSize = 14.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { isQuickDeclineMenuOpen = false },
              colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
              modifier = Modifier.align(Alignment.End)
            ) {
              Text("Cancel", color = Color.White)
            }
          }
        }
      } else if (isInCallDialpadOpen) {
        // Interactive In-Call Keypad for DTMF entry
        Card(
          colors = CardDefaults.cardColors(containerColor = cardContainerColor),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          shape = RoundedCornerShape(24.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = inCallDialpadInput.ifEmpty { "Touch Tones" },
              color = textColor,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 12.dp),
              textAlign = TextAlign.Center
            )

            val inCallKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  for (c in 0 until 3) {
                    val key = inCallKeys[r * 3 + c]
                    Box(
                      modifier = Modifier
                        .size(width = 64.dp, height = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(listBgColor)
                        .clickable {
                          inCallDialpadInput += key
                          CallManager.playDtmf(key[0])
                        },
                      contentAlignment = Alignment.Center
                    ) {
                      Text(key, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { isInCallDialpadOpen = false }) {
              Text("Close Keypad", color = if (isDarkTheme) Color(0xFFA8C7FA) else Color(0xFF0B57D0))
            }
          }
        }
      } else {
        Box(
          modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
          contentAlignment = Alignment.Center
        ) {
          if (participants.size > 1) {
            Text(
              text = "👥",
              fontSize = 54.sp
            )
          } else {
            val pName = participants.firstOrNull()?.first ?: contactName
            Text(
              text = if (pName.length >= 2) pName.substring(0, 2).uppercase() else "📞",
              fontSize = 48.sp,
              color = textColor,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Add Call Dialog
      if (isAddCallDialogOpen) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { isAddCallDialogOpen = false }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                "➕ Add Call",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
              )
              Spacer(modifier = Modifier.height(12.dp))
              
              androidx.compose.material3.OutlinedTextField(
                value = addCallNumberInput,
                onValueChange = {
                  addCallNumberInput = it
                  selectedAddCallContactName = ""
                },
                label = { Text("Search contact or enter number") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = if (isDarkTheme) Color(0xFFA8C7FA) else Color(0xFF0B57D0),
                  unfocusedBorderColor = subTextColor.copy(alpha = 0.5f),
                  focusedLabelColor = if (isDarkTheme) Color(0xFFA8C7FA) else Color(0xFF0B57D0)
                ),
                modifier = Modifier.fillMaxWidth()
              )
              
              Spacer(modifier = Modifier.height(8.dp))

              val filteredContacts = remember(addCallNumberInput, contacts) {
                if (addCallNumberInput.isBlank()) {
                  contacts
                } else {
                  contacts.filter {
                    it.name.contains(addCallNumberInput, ignoreCase = true) ||
                    it.number.contains(addCallNumberInput)
                  }
                }
              }

              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                items(filteredContacts) { contact ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                      .clickable {
                        addCallNumberInput = contact.number
                        selectedAddCallContactName = contact.name
                      }
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(contact.avatarBg),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = if (contact.name.isNotEmpty()) contact.name.take(1).uppercase() else "?",
                        color = contact.avatarTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = contact.name,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                      Text(
                        text = contact.number,
                        color = subTextColor,
                        fontSize = 11.sp
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                TextButton(onClick = { isAddCallDialogOpen = false }) {
                  Text("Cancel", color = subTextColor)
                }
                Button(
                  onClick = {
                    if (addCallNumberInput.isNotBlank()) {
                      val finalName = if (selectedAddCallContactName.isNotBlank()) {
                        selectedAddCallContactName
                      } else {
                        contacts.find { it.number == addCallNumberInput }?.name ?: addCallNumberInput
                      }
                      participants = participants + Pair(finalName, addCallNumberInput)
                      Toast.makeText(context, "📞 Merged call with $finalName", Toast.LENGTH_LONG).show()
                      isAddCallDialogOpen = false
                      addCallNumberInput = ""
                      selectedAddCallContactName = ""
                    } else {
                      Toast.makeText(context, "Please select or enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = if (isDarkTheme) Color(0xFFA8C7FA) else Color(0xFF0B57D0))
                ) {
                  Text("Add & Merge", color = if (isDarkTheme) Color.Black else Color.White)
                }
              }
            }
          }
        }
      }

      // Call Option buttons: keypad, mute, speaker, hold, add call, record
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Row 1: Keypad, Mute, Speaker
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          InCallButton(
            icon = "⌨️",
            label = "Keypad",
            isActive = isInCallDialpadOpen,
            onClick = { isInCallDialpadOpen = !isInCallDialpadOpen },
            isDarkTheme = isDarkTheme
          )
          InCallButton(
            icon = "🔇",
            label = "Mute",
            isActive = isMuted,
            onClick = {
              isMuted = !isMuted
              CallManager.setMuted(isMuted)
              Toast.makeText(context, if (isMuted) "🎤 Microphone Muted" else "🎤 Microphone Active", Toast.LENGTH_SHORT).show()
            },
            isDarkTheme = isDarkTheme
          )
          InCallButton(
            icon = "🔊",
            label = "Speaker",
            isActive = isSpeakerOn,
            onClick = {
              isSpeakerOn = !isSpeakerOn
              CallManager.setSpeaker(isSpeakerOn)
              Toast.makeText(context, if (isSpeakerOn) "🔊 Speakerphone On" else "🔈 Speakerphone Off", Toast.LENGTH_SHORT).show()
            },
            isDarkTheme = isDarkTheme
          )
        }

        // Row 2: Hold, Add Call, Record
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          InCallButton(
            icon = "⏸️",
            label = "Hold",
            isActive = isOnHold,
            onClick = {
              isOnHold = !isOnHold
              CallManager.setHold(isOnHold)
              Toast.makeText(context, if (isOnHold) "⏸️ Call placed on hold" else "▶️ Call resumed", Toast.LENGTH_SHORT).show()
            },
            isDarkTheme = isDarkTheme
          )
          InCallButton(
            icon = "🎧",
            label = "Bluetooth",
            isActive = isBluetoothOn,
            onClick = {
              isBluetoothOn = !isBluetoothOn
              CallManager.setBluetooth(isBluetoothOn)
              Toast.makeText(context, if (isBluetoothOn) "🎧 Bluetooth On" else "🎧 Bluetooth Off", Toast.LENGTH_SHORT).show()
            },
            isDarkTheme = isDarkTheme
          )
          Spacer(modifier = Modifier.size(60.dp))
        }

        // Styled "Quick Decline SMS" text link/button below the 6 main buttons
        if (isIncoming) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                .clickable { isQuickDeclineMenuOpen = !isQuickDeclineMenuOpen }
                .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
              Text(
                text = "💬 Send Quick SMS",
                color = if (isDarkTheme) Color(0xFFA8C7FA) else Color(0xFF0B57D0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Actions: Answer and Hang Up
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isIncoming) {
          // Green Answer Button
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(Color(0xFF16A34A))
              .clickable { onAnswer() }
              .testTag("answer_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "📞",
              fontSize = 22.sp,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.width(48.dp))
        }

        // Red Hang Up / Decline Button
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFFDC2626))
            .clickable { onHangUp() }
            .testTag("hangup_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "❌",
            fontSize = 22.sp,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun InCallButton(
  icon: String, // Kept for backwards compatibility but we now draw clean vector symbols
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  isDarkTheme: Boolean = true
) {
  val btnBg = if (isActive) {
    if (isDarkTheme) Color(0xFFD3E3FD) else Color(0xFF004A77)
  } else {
    if (isDarkTheme) Color(0xFF004A77) else Color(0xFFD3E3FD)
  }
  val contentColor = if (isActive) {
    if (isDarkTheme) Color(0xFF041E49) else Color.White
  } else {
    if (isDarkTheme) Color.White else Color(0xFF041E49)
  }
  val labelColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.9f) else Color(0xFF43474E)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(60.dp)
        .clip(CircleShape)
        .background(btnBg)
        .clickable { onClick() },
      contentAlignment = Alignment.Center
    ) {
      when (label) {
        "Keypad" -> {
          Canvas(modifier = Modifier.size(24.dp)) {
            val dotRadius = 1.8f.dp.toPx()
            val spacing = 6.dp.toPx()
            val startX = size.width / 2 - spacing
            val startY = size.height / 2 - spacing
            for (row in 0..2) {
              for (col in 0..2) {
                drawCircle(
                  color = contentColor,
                  radius = dotRadius,
                  center = androidx.compose.ui.geometry.Offset(
                    startX + col * spacing,
                    startY + row * spacing
                  )
                )
              }
            }
          }
        }
        "Mute" -> {
          Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            // Speaker bell
            val speakerPath = androidx.compose.ui.graphics.Path().apply {
              moveTo(w * 0.12f, h * 0.38f)
              lineTo(w * 0.32f, h * 0.38f)
              lineTo(w * 0.55f, h * 0.16f)
              lineTo(w * 0.55f, h * 0.84f)
              lineTo(w * 0.32f, h * 0.62f)
              lineTo(w * 0.12f, h * 0.62f)
              close()
            }
            drawPath(path = speakerPath, color = contentColor)
            
            // X symbol on the right
            val cx = w * 0.78f
            val cy = h * 0.50f
            val halfSize = 3.5f.dp.toPx()
            drawLine(
              color = contentColor,
              start = androidx.compose.ui.geometry.Offset(cx - halfSize, cy - halfSize),
              end = androidx.compose.ui.geometry.Offset(cx + halfSize, cy + halfSize),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )
            drawLine(
              color = contentColor,
              start = androidx.compose.ui.geometry.Offset(cx - halfSize, cy + halfSize),
              end = androidx.compose.ui.geometry.Offset(cx + halfSize, cy - halfSize),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )
          }
        }
        "Speaker" -> {
          Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            // Speaker bell
            val speakerPath = androidx.compose.ui.graphics.Path().apply {
              moveTo(w * 0.12f, h * 0.38f)
              lineTo(w * 0.32f, h * 0.38f)
              lineTo(w * 0.55f, h * 0.16f)
              lineTo(w * 0.55f, h * 0.84f)
              lineTo(w * 0.32f, h * 0.62f)
              lineTo(w * 0.12f, h * 0.62f)
              close()
            }
            drawPath(path = speakerPath, color = contentColor)
            
            // Wave 1
            drawArc(
              color = contentColor,
              startAngle = -40f,
              sweepAngle = 80f,
              useCenter = false,
              topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.30f),
              size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.40f),
              style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Wave 2
            drawArc(
              color = contentColor,
              startAngle = -40f,
              sweepAngle = 80f,
              useCenter = false,
              topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.18f),
              size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.64f),
              style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
          }
        }
        "Hold" -> {
          Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val barWidth = 4.dp.toPx()
            val barHeight = 13.dp.toPx()
            val topY = h * 0.23f
            
            // Bar 1
            drawRoundRect(
              color = contentColor,
              topLeft = androidx.compose.ui.geometry.Offset(w * 0.33f, topY),
              size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            // Bar 2
            drawRoundRect(
              color = contentColor,
              topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, topY),
              size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
          }
        }
        "Add Call" -> {
          Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            
            // Plus sign (top-right)
            val cx = w * 0.70f
            val cy = h * 0.30f
            val sz = 3.5f.dp.toPx()
            drawLine(
              color = contentColor,
              start = androidx.compose.ui.geometry.Offset(cx - sz, cy),
              end = androidx.compose.ui.geometry.Offset(cx + sz, cy),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )
            drawLine(
              color = contentColor,
              start = androidx.compose.ui.geometry.Offset(cx, cy - sz),
              end = androidx.compose.ui.geometry.Offset(cx, cy + sz),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )
            
            // Phone handset silhouette (bottom-left)
            val receiverPath = androidx.compose.ui.graphics.Path().apply {
              moveTo(w * 0.15f, h * 0.40f)
              quadraticTo(w * 0.15f, h * 0.65f, w * 0.40f, h * 0.85f)
              lineTo(w * 0.52f, h * 0.73f)
              quadraticTo(w * 0.32f, h * 0.53f, w * 0.27f, h * 0.40f)
              close()
            }
            drawPath(
              path = receiverPath,
              color = contentColor
            )
          }
        }
        "Record" -> {
          Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            
            // Outer Ring
            drawCircle(
              color = contentColor.copy(alpha = 0.5f),
              radius = 9.dp.toPx(),
              center = androidx.compose.ui.geometry.Offset(w / 2, h / 2),
              style = Stroke(width = 2.dp.toPx())
            )
            
            // Inner Solid Circle
            drawCircle(
              color = if (isActive) Color.Red else contentColor,
              radius = 4.5f.dp.toPx(),
              center = androidx.compose.ui.geometry.Offset(w / 2, h / 2)
            )
          }
        }
        else -> {
          // Fallback just in case
          Text(
            text = icon,
            fontSize = 22.sp,
            color = contentColor
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      color = labelColor,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

// 10. FULL-FEATURED SETTINGS PANEL
@Composable
fun SettingsPanel(
  isDarkTheme: Boolean,
  onThemeChange: (Boolean) -> Unit,
  dialpadTonesEnabled: Boolean,
  onTonesChange: (Boolean) -> Unit,
  vibrateOnClickEnabled: Boolean,
  onVibrateChange: (Boolean) -> Unit,
  preferredSim: String,
  onSimChange: (String) -> Unit,
  voicemailNumber: String,
  onVoicemailChange: (String) -> Unit,
  blockedNumbers: MutableList<String>,
  quickResponses: MutableList<String>,
  speedDialMap: MutableMap<Int, String>,
  contacts: List<Contact>,
  onClose: () -> Unit,
  primaryText: Color,
  secondaryText: Color,
  navBg: Color,
  searchBg: Color,
  brandBlue: Color,
  activePill: Color
) {
  var activeTab by remember { mutableStateOf(0) } // 0: Settings, 1: Block List, 2: Speed Dial, 3: Quick Responses
  var newBlockedInput by remember { mutableStateOf("") }
  var newQuickRespInput by remember { mutableStateOf("") }
  var targetSpeedDialKey by remember { mutableIntStateOf(-1) }

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(navBg)
          .statusBarsPadding()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(activePill)
            .clickable {
              if (activeTab != 0) {
                activeTab = 0
              } else {
                onClose()
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = brandBlue)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
          text = when (activeTab) {
            0 -> "Settings"
            1 -> "Block List Settings"
            2 -> "Speed Dial Mapping"
            3 -> "Quick SMS Responses"
            else -> "Settings"
          },
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = primaryText
        )
      }
    },
    containerColor = navBg
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
    ) {
      when (activeTab) {
        0 -> {
          // General Settings Tab
          LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
              Text("Theme & Display Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
              Spacer(modifier = Modifier.height(8.dp))
              SettingsRowToggle(
                title = "Dark Theme Mode",
                subtitle = "Enable battery-saving dark interface style",
                checked = isDarkTheme,
                onCheckedChange = onThemeChange,
                primaryText = primaryText,
                secondaryText = secondaryText
              )
            }

            item {
              HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
              Spacer(modifier = Modifier.height(8.dp))
              Text("Sound & Haptics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
              Spacer(modifier = Modifier.height(8.dp))
              SettingsRowToggle(
                title = "Dialpad Touch Tones",
                subtitle = "Play DTMF synthesizer tones upon number press",
                checked = dialpadTonesEnabled,
                onCheckedChange = onTonesChange,
                primaryText = primaryText,
                secondaryText = secondaryText
              )
              Spacer(modifier = Modifier.height(12.dp))
              SettingsRowToggle(
                title = "Tactile Vibration Feedback",
                subtitle = "Vibrate briefly upon clicking key buttons",
                checked = vibrateOnClickEnabled,
                onCheckedChange = onVibrateChange,
                primaryText = primaryText,
                secondaryText = secondaryText
              )
            }

            item {
              HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
              Spacer(modifier = Modifier.height(8.dp))
              Text("Dialer Capabilities", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
              Spacer(modifier = Modifier.height(8.dp))
              SettingsRowNav(
                title = "Speed Dial Setup",
                subtitle = "Assign digit hotkeys 1-9 to specific numbers",
                onClick = { activeTab = 2 },
                primaryText = primaryText,
                secondaryText = secondaryText,
                activePill = activePill
              )
              Spacer(modifier = Modifier.height(12.dp))
              SettingsRowNav(
                title = "Call Blocking",
                subtitle = "Manage phone numbers to auto-reject",
                onClick = { activeTab = 1 },
                primaryText = primaryText,
                secondaryText = secondaryText,
                activePill = activePill
              )
              Spacer(modifier = Modifier.height(12.dp))
              SettingsRowNav(
                title = "Quick SMS Templates",
                subtitle = "Edit SMS responses for declined calls",
                onClick = { activeTab = 3 },
                primaryText = primaryText,
                secondaryText = secondaryText,
                activePill = activePill
              )
            }

            item {
              HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
              Spacer(modifier = Modifier.height(8.dp))
              Text("SIM & Telecom Config", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Preferred Outbound SIM", fontWeight = FontWeight.Medium, color = primaryText)
                  Text("SIM subscription for default calls", fontSize = 12.sp, color = secondaryText)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  listOf("SIM 1", "SIM 2", "Ask").forEach { op ->
                    val sel = preferredSim == op
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) activePill else searchBg)
                        .clickable { onSimChange(op) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(op, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) brandBlue else secondaryText)
                    }
                  }
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              OutlinedTextField(
                value = voicemailNumber,
                onValueChange = onVoicemailChange,
                label = { Text("Voicemail Retrieval Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        1 -> {
          // Block List View
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = newBlockedInput,
                onValueChange = { newBlockedInput = it },
                label = { Text("Block New Number") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Button(
                onClick = {
                  if (newBlockedInput.isNotBlank()) {
                    blockedNumbers.add(newBlockedInput.trim())
                    newBlockedInput = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("+ Block", color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Currently Blocked Numbers:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = secondaryText)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(blockedNumbers, key = { it }) { num ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(searchBg)
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(num, color = primaryText, fontWeight = FontWeight.Medium)
                  IconButton(onClick = { blockedNumbers.remove(num) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Unblock", tint = Color.Red.copy(alpha = 0.8f))
                  }
                }
              }
            }
          }
        }

        2 -> {
          // Speed Dial Setup
          Column(modifier = Modifier.fillMaxSize()) {
            Text("Assign keys 1-9 to instantly call specified contacts:", fontSize = 13.sp, color = secondaryText)
            Spacer(modifier = Modifier.height(16.dp))

            if (targetSpeedDialKey != -1) {
              // Show assignment dialog / contact picker list
              Card(
                colors = CardDefaults.cardColors(containerColor = searchBg),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    "Select Contact for Speed Dial Key $targetSpeedDialKey:",
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                  )
                  Spacer(modifier = Modifier.height(12.dp))

                  LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(260.dp)
                  ) {
                    items(contacts, key = { "speed_dial_${it.name}_${it.number}" }) { contact ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(12.dp))
                          .clickable {
                            speedDialMap[targetSpeedDialKey] = contact.number
                            targetSpeedDialKey = -1
                          }
                          .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Box(
                          modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(contact.avatarBg),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(contact.name.take(1).uppercase(), fontSize = 12.sp, color = contact.avatarTextColor)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                          Text(contact.name, color = primaryText, fontSize = 14.sp)
                          Text(contact.number, color = secondaryText, fontSize = 12.sp)
                        }
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(12.dp))
                  Button(
                    onClick = { targetSpeedDialKey = -1 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                  ) {
                    Text("Cancel Picker", color = brandBlue)
                  }
                }
              }
            } else {
              LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..9).toList(), key = { it }) { digit ->
                  val assignedNum = speedDialMap[digit]
                  val assignedName = contacts.find { it.number == assignedNum }?.name ?: assignedNum

                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(searchBg)
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .clip(CircleShape)
                          .background(activePill),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(digit.toString(), fontWeight = FontWeight.Bold, color = brandBlue)
                      }
                      Spacer(modifier = Modifier.width(16.dp))
                      Column {
                        Text(
                          text = assignedName ?: "Unassigned Speed Key",
                          color = primaryText,
                          fontWeight = FontWeight.Bold
                        )
                        Text(
                          text = if (assignedNum != null) "Long press $digit on Dialer to trigger call" else "Tap setup icon to map a contact",
                          fontSize = 12.sp,
                          color = secondaryText
                        )
                      }
                    }

                    Row {
                      IconButton(onClick = { targetSpeedDialKey = digit }) {
                        Text("⚙️", fontSize = 16.sp)
                      }
                      if (assignedNum != null) {
                        IconButton(onClick = { speedDialMap.remove(digit) }) {
                          Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Speed dial key", tint = Color.Red.copy(alpha = 0.7f))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        3 -> {
          // Quick Responses View
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = newQuickRespInput,
                onValueChange = { newQuickRespInput = it },
                label = { Text("Add Quick SMS Template") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Button(
                onClick = {
                  if (newQuickRespInput.isNotBlank()) {
                    quickResponses.add(newQuickRespInput.trim())
                    newQuickRespInput = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("+ Add", color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Decline SMS Templates List:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = secondaryText)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(quickResponses, key = { it }) { resp ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(searchBg)
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(resp, color = primaryText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                  IconButton(onClick = { quickResponses.remove(resp) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete response", tint = Color.Red.copy(alpha = 0.8f))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsRowToggle(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  primaryText: Color,
  secondaryText: Color
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontWeight = FontWeight.Medium, color = primaryText)
      Text(text = subtitle, fontSize = 12.sp, color = secondaryText)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = BrandBlueLight
      )
    )
  }
}

@Composable
fun SettingsRowNav(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  primaryText: Color,
  secondaryText: Color,
  activePill: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontWeight = FontWeight.Medium, color = primaryText)
      Text(text = subtitle, fontSize = 12.sp, color = secondaryText)
    }
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(activePill),
      contentAlignment = Alignment.Center
    ) {
      Text("➔", fontSize = 12.sp, color = BrandBlueLight, fontWeight = FontWeight.Bold)
    }
  }
}

// 11. ADD CONTACT DIALOG COMPOSABLE
@Composable
fun AddContactDialog(
  name: String,
  onNameChange: (String) -> Unit,
  number: String,
  onNumberChange: (String) -> Unit,
  label: String,
  onLabelChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  softBlueBg: Color,
  activeBluePill: Color,
  searchBarBg: Color,
  primaryDarkText: Color,
  brandBlue: Color,
  grayText: Color
) {
  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = softBlueBg,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.Start
      ) {
        Text(
          text = "Create Contact",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = primaryDarkText,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
          value = name,
          onValueChange = onNameChange,
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("dialog_name_input")
        )

        OutlinedTextField(
          value = number,
          onValueChange = onNumberChange,
          label = { Text("Phone Number") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("dialog_phone_input")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("Mobile", "Work", "Home").forEach { option ->
            val isSelected = label == option
            Box(
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) activeBluePill else searchBarBg)
                .clickable { onLabelChange(option) },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = option,
                fontSize = 12.sp,
                color = if (isSelected) brandBlue else grayText,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Text("Cancel", color = brandBlue, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
          ) {
            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
