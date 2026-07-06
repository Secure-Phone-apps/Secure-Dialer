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
import android.os.Build
import android.telecom.TelecomManager

// CallLog Helpers for Android OS Call Log Database
fun loadRealCallLog(context: Context): List<CallRecord> {
  val list = mutableListOf<CallRecord>()
  try {
    val cursor = context.contentResolver.query(
      CallLog.Calls.CONTENT_URI,
      arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.NUMBER,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.CACHED_NUMBER_TYPE,
        CallLog.Calls.DURATION
      ),
      null,
      null,
      CallLog.Calls.DATE + " DESC"
    )

    val colors = listOf(AvatarOrange, AvatarBlue, AvatarGreen)
    val textColors = listOf(AvatarOrangeText, AvatarBlueText, AvatarGreenText)

    cursor?.use {
      val idCol = it.getColumnIndex(CallLog.Calls._ID)
      val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
      val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
      val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
      val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
      val numTypeCol = it.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE)
      val durationCol = it.getColumnIndex(CallLog.Calls.DURATION)

      while (it.moveToNext()) {
        val id = if (idCol != -1) it.getInt(idCol) else 0
        val number = if (numCol != -1) it.getString(numCol) ?: "" else ""
        val nameRaw = if (nameCol != -1) it.getString(nameCol) else null
        val typeInt = if (typeCol != -1) it.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
        val dateMs = if (dateCol != -1) it.getLong(dateCol) else 0L
        val numType = if (numTypeCol != -1) it.getInt(numTypeCol) else -1

        val duration = if (durationCol != -1) it.getLong(durationCol) else 0L
        val isVoicemail = typeInt == CallLog.Calls.VOICEMAIL_TYPE

        val name = nameRaw?.takeIf { it.isNotBlank() } ?: "Unknown"

        val label = when (numType) {
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
          ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
          else -> "Other"
        }

        val type = when (typeInt) {
          CallLog.Calls.MISSED_TYPE -> CallType.MISSED
          CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
          else -> CallType.INCOMING
        }

        val timestamp = if (dateMs > 0) {
          DateFormat.format("MMM dd, h:mm a", Date(dateMs)).toString()
        } else {
          "Unknown"
        }

        val avatarText = if (name != "Unknown" && name.isNotEmpty()) {
          if (name.length >= 2) name.substring(0, 2).uppercase() else name.take(1).uppercase()
        } else {
          "?"
        }

        val hashCodeVal = name.hashCode()
        val posHashCode = if (hashCodeVal < 0) -hashCodeVal else hashCodeVal
        val colorIdx = posHashCode % colors.size
        val avatarBg = colors[colorIdx]
        val avatarTextColor = textColors[colorIdx]

        list.add(
          CallRecord(
            id = id,
            name = name,
            number = number,
            label = label,
            timestamp = timestamp,
            type = type,
            avatarText = avatarText,
            avatarBg = avatarBg,
            avatarTextColor = avatarTextColor,
            duration = duration,
            hasVoicemail = isVoicemail
          )
        )
      }
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return list
}

// Contact CRUD Helpers for Android OS Contacts Database
fun loadRealContacts(context: Context): List<Contact> {
  val list = mutableListOf<Contact>()
  try {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.TYPE,
        ContactsContract.CommonDataKinds.Phone.LABEL,
        ContactsContract.CommonDataKinds.Phone.STARRED
      ),
      null,
      null,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    val colors = listOf(AvatarOrange, AvatarBlue, AvatarGreen)
    val textColors = listOf(AvatarOrangeText, AvatarBlueText, AvatarGreenText)

    cursor?.use {
      val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
      val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
      val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
      val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
      val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

      while (it.moveToNext()) {
        val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
        val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
        val type = if (typeIndex != -1) it.getInt(typeIndex) else -1
        val labelStr = if (labelIndex != -1) it.getString(labelIndex) ?: "" else ""
        val favorite = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

        val label = when (type) {
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
          ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
          else -> if (labelStr.isNotEmpty()) labelStr else "Mobile"
        }

        if (number.isNotEmpty() && list.none { it.number == number && it.name == name }) {
          val colorIdx = (name.hashCode() and 0x7FFFFFFF) % colors.size
          list.add(
            Contact(
              name = name,
              number = number,
              label = label,
              favorite = favorite,
              avatarBg = colors[colorIdx],
              avatarTextColor = textColors[colorIdx]
            )
          )
        }
      }
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return list
}

fun addRealContact(context: Context, name: String, number: String, label: String): Boolean {
  return try {
    val ops = arrayListOf<ContentProviderOperation>()

    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
      .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
      .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
      .build())

    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
      .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
      .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
      .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
      .build())

    val type = when (label.lowercase()) {
      "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
      "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
      else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
    }

    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
      .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
      .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
      .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
      .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type)
      .build())

    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    true
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}

fun deleteRealContact(context: Context, name: String): Boolean {
  return try {
    val resolver = context.contentResolver
    resolver.delete(
      ContactsContract.RawContacts.CONTENT_URI,
      "${ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY} = ?",
      arrayOf(name)
    )
    true
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}

fun updateRealContact(context: Context, oldName: String, newName: String, newNumber: String, newLabel: String): Boolean {
  return try {
    val resolver = context.contentResolver

    val cursor = resolver.query(
      ContactsContract.Data.CONTENT_URI,
      arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
      "${ContactsContract.Data.DISPLAY_NAME} = ?",
      arrayOf(oldName),
      null
    )
    var rawContactId: Long? = null
    cursor?.use {
      if (it.moveToFirst()) {
        rawContactId = it.getLong(0)
      }
    }

    if (rawContactId == null) return false

    val nameValues = ContentValues().apply {
      put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
    }
    resolver.update(
      ContactsContract.Data.CONTENT_URI,
      nameValues,
      "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
      arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
    )

    val phoneValues = ContentValues().apply {
      put(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumber)
      val type = when (newLabel.lowercase()) {
        "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
      }
      put(ContactsContract.CommonDataKinds.Phone.TYPE, type)
    }
    resolver.update(
      ContactsContract.Data.CONTENT_URI,
      phoneValues,
      "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
      arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
    )

    true
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}

fun toggleRealContactFavorite(context: Context, name: String, isFavorite: Boolean): Boolean {
  return try {
    val resolver = context.contentResolver

    val cursor = resolver.query(
      ContactsContract.Contacts.CONTENT_URI,
      arrayOf(ContactsContract.Contacts._ID),
      "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
      arrayOf(name),
      null
    )
    var contactId: Long? = null
    cursor?.use {
      if (it.moveToFirst()) {
        contactId = it.getLong(0)
      }
    }

    if (contactId == null) return false

    val values = ContentValues().apply {
      put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
    }
    resolver.update(
      ContactsContract.Contacts.CONTENT_URI,
      values,
      "${ContactsContract.Contacts._ID} = ?",
      arrayOf(contactId.toString())
    )
    true
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}

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

// Data Classes
data class CallRecord(
  val id: Int,
  val name: String,
  val number: String,
  val label: String,
  val timestamp: String,
  val type: CallType,
  val avatarText: String,
  val avatarBg: Color,
  val avatarTextColor: Color,
  val duration: Long,
  val hasVoicemail: Boolean
)

enum class CallType {
  MISSED, OUTGOING, INCOMING
}

data class Contact(
  val name: String,
  val number: String,
  val label: String,
  val favorite: Boolean = false,
  val avatarBg: Color,
  val avatarTextColor: Color
)

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
      var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
      
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

      DialerTheme(isDarkTheme = isDarkTheme) {
        MainScreen(
          isDarkTheme = isDarkTheme,
          onThemeChange = { newVal ->
            isDarkTheme = newVal
            prefs.edit().putBoolean("dark_theme", newVal).apply()
          },
          dialpadTonesEnabled = dialpadTonesEnabled,
          onTonesChange = {
            dialpadTonesEnabled = it
            prefs.edit().putBoolean("dialpad_tones", it).apply()
          },
          vibrateOnClickEnabled = vibrateOnClickEnabled,
          onVibrateChange = {
            vibrateOnClickEnabled = it
            prefs.edit().putBoolean("vibrate_on_click", it).apply()
          },
          preferredSim = preferredSim,
          onSimChange = {
            preferredSim = it
            prefs.edit().putString("preferred_sim", it).apply()
          },
          voicemailNumber = voicemailNumber,
          onVoicemailChange = {
            voicemailNumber = it
            prefs.edit().putString("voicemail_number", it).apply()
          },
          blockedNumbers = blockedNumbers,
          quickResponses = quickResponses,
          speedDialMap = speedDialMap
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
  speedDialMap: MutableMap<Int, String>
) {
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
  var selectedTab by rememberSaveable { mutableIntStateOf(0) } // Default to "Recents"

  // Search state
  var searchQuery by rememberSaveable { mutableStateOf("") }

  // State for Dialpad Overlay
  var isDialpadVisible by rememberSaveable { mutableStateOf(false) }
  var dialpadInput by rememberSaveable { mutableStateOf("") }

  // Settings State Drawer / Dialog
  var isSettingsVisible by rememberSaveable { mutableStateOf(false) }

  // Settings features are now passed as parameters

  // State for Active In-call Screen
  var isCallActive by rememberSaveable { mutableStateOf(false) }
  var callingContactName by rememberSaveable { mutableStateOf("") }
  var callingContactNumber by rememberSaveable { mutableStateOf("") }

  // State for Add Contact Dialog
  var isAddContactDialogVisible by rememberSaveable { mutableStateOf(false) }
  var newContactName by rememberSaveable { mutableStateOf("") }
  var newContactNumber by rememberSaveable { mutableStateOf("") }
  var newContactLabel by rememberSaveable { mutableStateOf("Mobile") }

  // Mock call history state (initialized empty)
  val callHistory = remember {
    mutableStateListOf<CallRecord>()
  }

  // Mock contacts list (initialized empty)
  val contactsList = remember {
    mutableStateListOf<Contact>()
  }

  // Synchronize UI Call state reactively with the real OS Telecom Call Manager
  LaunchedEffect(systemActiveCall, systemCallState, systemCallerNumber, contactsList) {
    if (systemActiveCall != null) {
      isCallActive = true
      if (systemCallerNumber.isNotEmpty()) {
        callingContactNumber = systemCallerNumber
        val cleanCallerNumber = systemCallerNumber.filter { it.isDigit() }.takeLast(10)
        val matched = contactsList.find { 
          it.number.filter { it.isDigit() }.takeLast(10) == cleanCallerNumber 
        }
        callingContactName = matched?.name ?: systemCallerNumber
      } else {
        callingContactNumber = "Unknown"
        callingContactName = "Unknown"
      }
    } else {
      isCallActive = false
    }
  }

  // Contact & Call Log Permission and Database Loading State
  var hasContactsPermission by remember { mutableStateOf(false) }
  var hasCallLogPermission by remember { mutableStateOf(false) }
  var isLoadingPermissions by remember { mutableStateOf(true) }

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
  var isEditContactDialogVisible by remember { mutableStateOf(false) }
  var oldContactToEdit by remember { mutableStateOf<Contact?>(null) }
  var editContactName by remember { mutableStateOf("") }
  var editContactNumber by remember { mutableStateOf("") }
  var editContactLabel by remember { mutableStateOf("Mobile") }

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
          isDarkTheme = isDarkTheme,
          onThemeChange = onThemeChange,
          dialpadTonesEnabled = dialpadTonesEnabled,
          onTonesChange = onTonesChange,
          vibrateOnClickEnabled = vibrateOnClickEnabled,
          onVibrateChange = onVibrateChange,
          preferredSim = preferredSim,
          onSimChange = onSimChange,
          voicemailNumber = voicemailNumber,
          onVoicemailChange = onVoicemailChange,
          blockedNumbers = blockedNumbers,
          quickResponses = quickResponses,
          speedDialMap = speedDialMap,
          contacts = contactsList,
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
