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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import android.provider.ContactsContract
import android.content.ContentProviderOperation
import android.content.ContentValues
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.provider.CallLog
import android.text.format.DateFormat
import java.util.Date
import android.content.Intent
import android.net.Uri
import android.app.role.RoleManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import android.os.Build
import android.telecom.TelecomManager

import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.ui.components.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.DialerViewModel


// Helper functions moved to com.example.data.DialerRepository

// Professional Polish Theme Colors
val BrandBlueLight = Color(0xFF0B57D0)
val SoftBlueBgLight = Color(0xFFF7F9FF)
val ActiveBluePillLight = Color(0xFFD3E3FD)
val SearchBarBgLight = Color(0xFFE9EEF6)
val GrayTextLight = Color(0xFF44474E)
val PrimaryDarkTextLight = Color(0xFF191C20)
val NavBgLight = Color(0xFFF3F6FC)
val NavBorderLight = Color(0xFFDDE3EA)

// Dark Theme Variants
val BrandBlueDark = Color(0xFFA8C7FA)
val SoftBlueBgDark = Color(0xFF111318)
val ActiveBluePillDark = Color(0xFF004A77)
val SearchBarBgDark = Color(0xFF282A2F)
val GrayTextDark = Color(0xFFC4C6D0)
val PrimaryDarkTextDark = Color(0xFFE2E2E9)
val NavBgDark = Color(0xFF1E2025)
val NavBorderDark = Color(0xFF44474E)

// Avatar Colors
val AvatarOrange = Color(0xFFFFDBCB)
val AvatarOrangeText = Color(0xFF311300)
val AvatarBlue = Color(0xFFD1E4FF)
val AvatarBlueText = Color(0xFF001D36)
val AvatarGreen = Color(0xFFD9E7CB)
val AvatarGreenText = Color(0xFF141E0D)


// Data Classes moved to com.example.model.DialerModels

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Support showing over lock screen
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
    
    enableEdgeToEdge()
    val prefs = getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

    // Check and request default dialer role on startup
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
      if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        startActivity(intent)
      }
    } else {
      val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
      val currentDefault = telecomManager.defaultDialerPackage
      if (currentDefault != packageName) {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
          putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        }
        startActivity(intent)
      }
    }

    setContent {
      val viewModel: DialerViewModel = viewModel()
      var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
      var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
      
      val roleHeld = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
          roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
          val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
          telecomManager.defaultDialerPackage == packageName
        }
      }
      
      if (showRestrictedSettingsDialog) {
          AlertDialog(
              onDismissRequest = { showRestrictedSettingsDialog = false },
              title = { Text("Restricted Settings") },
              text = { Text("To use this app as your default dialer, you may need to manually enable it. Go to System Settings > Apps > [Our App Name] > Advanced > Allow Restricted Settings, then try again.") },
              confirmButton = {
                  TextButton(onClick = { 
                      showRestrictedSettingsDialog = false
                      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                          data = Uri.fromParts("package", packageName, null)
                      }
                      startActivity(intent)
                  }) { Text("Open Settings") }
              },
              dismissButton = {
                  TextButton(onClick = { showRestrictedSettingsDialog = false }) { Text("Cancel") }
              }
          )
      }
      
      var dialpadTonesEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("dialpad_tones", true)) }
      var vibrateOnClickEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("vibrate_on_click", true)) }
      var preferredSim by rememberSaveable { mutableStateOf(prefs.getString("preferred_sim", "SIM 1") ?: "SIM 1") }
      var voicemailNumber by rememberSaveable { mutableStateOf(prefs.getString("voicemail_number", "+1 (555) 011-9988") ?: "+1 (555) 011-9988") }
      
      val blockedNumbers = remember {
        val blockedSet = prefs.getStringSet("blocked_numbers", setOf("+1 (555) 019-3847", "911-FAKE"))
        mutableStateListOf<String>().apply { addAll(blockedSet ?: emptyList()) }
      }
      
      val quickResponses = remember {
        val savedResponses = prefs.getStringSet("quick_responses", setOf(
          "In a meeting. Call you later?",
          "Can't talk right now. What's up?",
          "On my way, speak soon!",
          "I'll call you right back."
        ))
        mutableStateListOf<String>().apply { addAll(savedResponses ?: emptyList()) }
      }
      
      val speedDialMap = remember {
        val map = mutableStateMapOf<Int, String>()
        val savedSpeedDial = prefs.getString("speed_dial", "2:+1 (555) 012-3456,3:+1 (555) 014-2200")
        savedSpeedDial?.split(",")?.forEach {
          val parts = it.split(":")
          if (parts.size == 2) map[parts[0].toInt()] = parts[1]
        }
        map
      }

      DialerTheme(isDarkTheme = viewModel.isDarkTheme.value) {
        MainScreen(
          viewModel = viewModel,
          onShowRestrictedSettings = { showRestrictedSettingsDialog = true },
          isDefaultDialer = roleHeld
        )
      }
    }
  }
}

@Composable
fun DialerTheme(isDarkTheme: Boolean, content: @Composable () -> Unit) {
  val colors = if (isDarkTheme) {
    darkColorScheme(
      primary = BrandBlueDark,
      background = SoftBlueBgDark,
      surface = SoftBlueBgDark,
      onPrimary = Color.Black,
      onBackground = PrimaryDarkTextDark,
      onSurface = PrimaryDarkTextDark
    )
  } else {
    lightColorScheme(
      primary = BrandBlueLight,
      background = SoftBlueBgLight,
      surface = SoftBlueBgLight,
      onPrimary = Color.White,
      onBackground = PrimaryDarkTextLight,
      onSurface = PrimaryDarkTextLight
    )
  }
  MaterialTheme(
    colorScheme = colors,
    content = content
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
  viewModel: DialerViewModel,
  onShowRestrictedSettings: () -> Unit,
  isDefaultDialer: Boolean
) {
  val isDarkTheme by viewModel.isDarkTheme
  val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
  val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
  val preferredSim by viewModel.preferredSim
  val voicemailNumber by viewModel.voicemailNumber
  val blockedNumbers = viewModel.blockedNumbers
  val quickResponses = viewModel.quickResponses
  val speedDialMap = viewModel.speedDialMap
  
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  // Real-time Telecom Call observers
  val systemActiveCall by CallManager.currentCall.collectAsState()
  val systemCallState by CallManager.callState.collectAsState()
  val systemCallerNumber by CallManager.callerNumber.collectAsState()

  // Theme-aware styles
  val currentBrandBlue = if (isDarkTheme) BrandBlueDark else BrandBlueLight
  val currentSoftBlueBg = if (isDarkTheme) SoftBlueBgDark else SoftBlueBgLight
  val currentActiveBluePill = if (isDarkTheme) ActiveBluePillDark else ActiveBluePillLight
  val currentSearchBarBg = if (isDarkTheme) SearchBarBgDark else SearchBarBgLight
  val currentGrayText = if (isDarkTheme) GrayTextDark else GrayTextLight
  val currentPrimaryDarkText = if (isDarkTheme) PrimaryDarkTextDark else PrimaryDarkTextLight
  val currentNavBg = if (isDarkTheme) NavBgDark else NavBgLight
  val currentNavBorder = if (isDarkTheme) NavBorderDark else NavBorderLight

  // DTMF Tone Generator
  val toneGenerator = remember {
    try {
      ToneGenerator(AudioManager.STREAM_DTMF, 80)
    } catch (e: Exception) {
      null
    }
  }

  fun playDtmf(key: String) {
    toneGenerator?.let {
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

  // Navigation tabs state
  var selectedTab by viewModel.selectedTab

  // Search state
  var searchQuery by viewModel.searchQuery

  // State for Dialpad Overlay
  var isDialpadVisible by viewModel.isDialpadVisible
  var dialpadInput by viewModel.dialpadInput

  // Settings State Drawer / Dialog
  var isSettingsVisible by viewModel.isSettingsVisible

  // Settings features are now passed as parameters

  // State for Active In-call Screen
  var isCallActive by viewModel.isCallActive
  var callingContactName by viewModel.callingContactName
  var callingContactNumber by viewModel.callingContactNumber

  // State for Add Contact Dialog
  var isAddContactDialogVisible by viewModel.isAddContactDialogVisible
  var newContactName by viewModel.newContactName
  var newContactNumber by viewModel.newContactNumber
  var newContactLabel by viewModel.newContactLabel

  // Mock call history state (initialized empty)
  val callHistory = viewModel.callHistory

  // Mock contacts list (initialized empty)
  val contactsList = viewModel.contactsList

  // Synchronize UI Call state reactively with the real OS Telecom Call Manager
  LaunchedEffect(systemActiveCall, systemCallState, systemCallerNumber) {
    if (systemActiveCall != null) {
      isCallActive = true
      if (systemCallerNumber.isNotEmpty()) {
        callingContactNumber = systemCallerNumber
        callingContactName = getContactNameFromNumber(context, systemCallerNumber) ?: systemCallerNumber
      } else {
        callingContactNumber = "Unknown"
        callingContactName = "Unknown"
      }
    } else {
      isCallActive = false
    }
  }

  // Contact & Call Log Permission and Database Loading State
  var hasContactsPermission by viewModel.hasContactsPermission
  var hasCallLogPermission by viewModel.hasCallLogPermission
  var isLoadingPermissions by viewModel.isLoadingPermissions

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false
    hasCallLogPermission = permissions[Manifest.permission.READ_CALL_LOG] ?: false
    isLoadingPermissions = false
  }

  fun refreshContacts() {
    val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    hasContactsPermission = readGranted
    if (readGranted) {
      val realList = loadRealContacts(context)
      contactsList.clear()
      contactsList.addAll(realList)
    }
  }

  fun refreshCallLog() {
    val callLogGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    hasCallLogPermission = callLogGranted
    if (callLogGranted) {
      val realCallLog = loadRealCallLog(context)
      if (realCallLog.isNotEmpty()) {
        callHistory.clear()
        callHistory.addAll(realCallLog)
      }
    }
  }

  LaunchedEffect(hasContactsPermission, hasCallLogPermission) {
    refreshContacts()
    refreshCallLog()
  }

  LaunchedEffect(Unit) {
    refreshContacts()
    refreshCallLog()
  }

  // State for Edit Contact Dialog
  var isEditContactDialogVisible by viewModel.isEditContactDialogVisible
  var oldContactToEdit by viewModel.oldContactToEdit
  var editContactName by viewModel.editContactName
  var editContactNumber by viewModel.editContactNumber
  var editContactLabel by viewModel.editContactLabel

  // Voicemail state
  val voicemailRecords = remember {
    mutableStateListOf(
      CallRecord(101, "Unknown Caller", "+1 (555) 011-9988", "Voicemail", "June 28", CallType.INCOMING, "?", Color.LightGray, Color.DarkGray, 0L, true),
      CallRecord(102, "David Miller", "+1 (555) 013-1122", "Voicemail", "June 25", CallType.INCOMING, "D", AvatarBlue, AvatarBlueText, 0L, true)
    )
  }

  // Blocked check utility
  fun initiateCall(name: String, number: String, label: String = "Mobile") {
    // Check if the number is in the blocklist
    if (blockedNumbers.contains(number)) {
      Toast.makeText(context, "🚫 Call Blocked: $number is in your Blocklist!", Toast.LENGTH_LONG).show()
      return
    }

    callingContactName = name
    callingContactNumber = number
    isCallActive = true

    // Log the outgoing call
    callHistory.add(
      0, CallRecord(
        id = (callHistory.maxOfOrNull { it.id } ?: 0) + 1,
        name = name,
        number = number,
        label = label,
        timestamp = "Just now",
        type = CallType.OUTGOING,
        avatarText = if (name.length >= 2) name.substring(0, 2).uppercase() else name.take(1).uppercase(),
        avatarBg = AvatarBlue,
        avatarTextColor = AvatarBlueText,
        duration = 0L,
        hasVoicemail = false
      )
    )

    // Try to place a real phone call!
    CallManager.placeCall(context, number)
  }

  // Filter lists based on search
  val filteredCallHistory = callHistory.filter {
    it.name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery, ignoreCase = true)
  }

  val filteredContacts = contactsList.filter {
    it.name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery, ignoreCase = true)
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = currentSoftBlueBg,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        if (!isDefaultDialer) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "Set as default dialer to enable all features. Click to open settings."
                    }
                    .clickable { onShowRestrictedSettings() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Action Required",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Set as default dialer to enable all features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        // 1. Search Bar Header with Settings Menu Option
        HeaderSearchBar(
          searchQuery = searchQuery,
          onQueryChange = { searchQuery = it },
          onSettingsClick = { isSettingsVisible = true },
          onProfileClick = { isSettingsVisible = true },
          searchBg = currentSearchBarBg,
          textStyleColor = currentPrimaryDarkText,
          grayTextColor = currentGrayText,
          activePillColor = currentActiveBluePill
        )

        // 2. Main Content Area based on selected Tab
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          when (selectedTab) {
            0 -> RecentsTabContent(
              callRecords = filteredCallHistory,
              onCallClick = { record -> initiateCall(record.name, record.number, record.label) },
              onDeleteRecord = { id -> callHistory.removeAll { it.id == id } },
              primaryText = currentPrimaryDarkText,
              secondaryText = currentGrayText,
              activePill = currentActiveBluePill,
              brandBlue = currentBrandBlue,
              hasPermission = hasCallLogPermission,
              isLoading = isLoadingPermissions,
              onRequestPermission = {
                permissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG,
                    Manifest.permission.CALL_PHONE
                  )
                )
              }
            )

            1 -> ContactsTabContent(
              contacts = filteredContacts,
              onCallClick = { contact -> initiateCall(contact.name, contact.number, contact.label) },
              onAddContactClick = { isAddContactDialogVisible = true },
              onToggleFavorite = { contact ->
                if (hasContactsPermission) {
                  val success = toggleRealContactFavorite(context, contact.name, !contact.favorite)
                  if (success) {
                    Toast.makeText(context, "Star toggled successfully", Toast.LENGTH_SHORT).show()
                  }
                  refreshContacts()
                } else {
                  val index = contactsList.indexOfFirst { it.name == contact.name && it.number == contact.number }
                  if (index != -1) {
                    contactsList[index] = contactsList[index].copy(favorite = !contact.favorite)
                  }
                }
              },
              primaryText = currentPrimaryDarkText,
              secondaryText = currentGrayText,
              activePill = currentActiveBluePill,
              brandBlue = currentBrandBlue,
              hasPermission = hasContactsPermission,
              isLoading = isLoadingPermissions,
              onRequestPermission = {
                permissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                  )
                )
              },
              onEditContact = { contact ->
                oldContactToEdit = contact
                editContactName = contact.name
                editContactNumber = contact.number
                editContactLabel = contact.label
                isEditContactDialogVisible = true
              },
              onDeleteContact = { contact ->
                if (hasContactsPermission) {
                  val success = deleteRealContact(context, contact.name)
                  if (success) {
                    Toast.makeText(context, "🗑️ Contact deleted: ${contact.name}", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Failed to delete contact", Toast.LENGTH_SHORT).show()
                  }
                  refreshContacts()
                } else {
                  contactsList.removeAll { it.name == contact.name && it.number == contact.number }
                  Toast.makeText(context, "🗑️ Sim-Contact deleted: ${contact.name}", Toast.LENGTH_SHORT).show()
                }
              }
            )

            2 -> DialpadTabContent(
              inputValue = dialpadInput,
              onValueChange = {
                if (it.length > dialpadInput.length) {
                  val added = it.last().toString()
                  if (dialpadTonesEnabled) playDtmf(added)
                  if (vibrateOnClickEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                dialpadInput = it
              },
              onCallClick = { number ->
                if (number.isNotEmpty()) {
                  val matchedContact = contactsList.find { it.number == number }
                  initiateCall(matchedContact?.name ?: "Unknown Number", number, matchedContact?.label ?: "Mobile")
                  dialpadInput = ""
                }
              },
              onSpeedDialCall = { number ->
                val matchedContact = contactsList.find { it.number == number }
                initiateCall(matchedContact?.name ?: "Speed Dial", number, matchedContact?.label ?: "Mobile")
              },
              voicemailNumber = voicemailNumber,
              speedDialMap = speedDialMap,
              activePill = currentActiveBluePill,
              searchBg = currentSearchBarBg,
              primaryText = currentPrimaryDarkText,
              grayText = currentGrayText,
              contactsList = contactsList
            )
          }
        }

        // 3. Bottom Navigation Bar
        BottomNavBar(
          selectedTab = selectedTab,
          onTabSelected = { selectedTab = it },
          navBg = currentNavBg,
          navBorder = currentNavBorder,
          activePill = currentActiveBluePill,
          primaryText = currentPrimaryDarkText,
          grayText = currentGrayText
        )
      }

      // 6. Immersive Fullscreen Active Call Screen with Quick Responses Rejection
      AnimatedVisibility(
        visible = isCallActive,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        ActiveCallScreen(
          contactName = callingContactName,
          contactNumber = callingContactNumber,
          preferredSim = preferredSim,
          quickResponses = quickResponses,
          onHangUp = { 
            CallManager.disconnect()
            isCallActive = false 
          },
          onAnswer = {
            CallManager.answer()
          },
          onQuickDecline = { responseText ->
            CallManager.disconnect()
            isCallActive = false
            Toast.makeText(context, "💬 Rejection SMS Sent: \"$responseText\"", Toast.LENGTH_LONG).show()
          },
          isDarkTheme = isDarkTheme,
          isIncoming = (systemCallState == android.telecom.Call.STATE_RINGING),
          contacts = contactsList,
          activePill = currentActiveBluePill,
          callState = systemCallState
        )
      }

      // 7. Full-Featured Settings Panel
      AnimatedVisibility(
        visible = isSettingsVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
      ) {
        SettingsPanel(
          viewModel = viewModel,
          onClose = { isSettingsVisible = false },
          primaryText = currentPrimaryDarkText,
          secondaryText = currentGrayText,
          navBg = currentNavBg,
          searchBg = currentSearchBarBg,
          brandBlue = currentBrandBlue,
          activePill = currentActiveBluePill
        )
      }

      // 8. Add Contact Dialog
      if (isAddContactDialogVisible) {
        AddContactDialog(
          name = newContactName,
          onNameChange = { newContactName = it },
          number = newContactNumber,
          onNumberChange = { newContactNumber = it },
          label = newContactLabel,
          onLabelChange = { newContactLabel = it },
          onDismiss = {
            isAddContactDialogVisible = false
            newContactName = ""
            newContactNumber = ""
          },
          onConfirm = {
            if (newContactName.isNotBlank() && newContactNumber.isNotBlank()) {
              if (hasContactsPermission) {
                val success = addRealContact(context, newContactName, newContactNumber, newContactLabel)
                if (success) {
                  Toast.makeText(context, "Contact saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Failed to save contact", Toast.LENGTH_SHORT).show()
                }
                refreshContacts()
              } else {
                contactsList.add(
                  Contact(
                    name = newContactName,
                    number = newContactNumber,
                    label = newContactLabel,
                    avatarBg = listOf(AvatarOrange, AvatarBlue, AvatarGreen).random(),
                    avatarTextColor = listOf(AvatarOrangeText, AvatarBlueText, AvatarGreenText).random()
                  )
                )
                Toast.makeText(context, "Sim-Contact saved locally", Toast.LENGTH_SHORT).show()
              }
              isAddContactDialogVisible = false
              newContactName = ""
              newContactNumber = ""
            }
          },
          softBlueBg = currentSoftBlueBg,
          activeBluePill = currentActiveBluePill,
          searchBarBg = currentSearchBarBg,
          primaryDarkText = currentPrimaryDarkText,
          brandBlue = currentBrandBlue,
          grayText = currentGrayText
        )
      }

      // 9. Edit Contact Dialog
      if (isEditContactDialogVisible && oldContactToEdit != null) {
        AddContactDialog(
          name = editContactName,
          onNameChange = { editContactName = it },
          number = editContactNumber,
          onNumberChange = { editContactNumber = it },
          label = editContactLabel,
          onLabelChange = { editContactLabel = it },
          onDismiss = {
            isEditContactDialogVisible = false
            oldContactToEdit = null
          },
          onConfirm = {
            val old = oldContactToEdit
            if (old != null && editContactName.isNotBlank() && editContactNumber.isNotBlank()) {
              if (hasContactsPermission) {
                val success = updateRealContact(context, old.name, editContactName, editContactNumber, editContactLabel)
                if (success) {
                  Toast.makeText(context, "Contact updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Failed to update contact", Toast.LENGTH_SHORT).show()
                }
                refreshContacts()
              } else {
                val index = contactsList.indexOfFirst { it.name == old.name && it.number == old.number }
                if (index != -1) {
                  contactsList[index] = contactsList[index].copy(
                    name = editContactName,
                    number = editContactNumber,
                    label = editContactLabel
                  )
                }
                Toast.makeText(context, "Sim-Contact updated successfully", Toast.LENGTH_SHORT).show()
              }
              isEditContactDialogVisible = false
              oldContactToEdit = null
            }
          },
          softBlueBg = currentSoftBlueBg,
          activeBluePill = currentActiveBluePill,
          searchBarBg = currentSearchBarBg,
          primaryDarkText = currentPrimaryDarkText,
          brandBlue = currentBrandBlue,
          grayText = currentGrayText
        )
      }
    }
  }
}
