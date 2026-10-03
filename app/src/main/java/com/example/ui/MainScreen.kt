/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.CallManager
import com.example.ContactCache
import com.example.R
import com.example.getContactNameFromNumber
import com.example.getSavedCnapName
import com.example.model.CallRecording
import com.example.model.CallType
import com.example.ui.components.*
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: DialerViewModel,
    onShowRestrictedSettings: () -> Unit,
    isDefaultDialer: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val preferredSim by viewModel.preferredSim
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsStateWithLifecycle()
    val blockedNumbers = remember(blockedNumbersEntities) { blockedNumbersEntities.map { it.number } }

    var selectedTab by viewModel.selectedTab
    val searchQuery by viewModel.searchQuery
    val isCallHistoryDetailsOpen by viewModel.isCallHistoryDetailsOpen
    var isDialpadVisible by viewModel.isDialpadVisible
    var dialpadInput by viewModel.dialpadInput
    var isSettingsVisible by viewModel.isSettingsVisible
    var isCallActive by viewModel.isCallActive
    var isCallMinimized by viewModel.isCallMinimized

    LaunchedEffect(selectedTab) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(isSettingsVisible) {
        if (isSettingsVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    LaunchedEffect(isCallActive, viewModel.isFakeCallActive.value) {
        if (isCallActive || viewModel.isFakeCallActive.value) {
            isSettingsVisible = false
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    LaunchedEffect(isCallActive) {
        if (!isCallActive) {
            isCallMinimized = false
        }
    }

    var callingContactName by viewModel.callingContactName
    var callingContactNumber by viewModel.callingContactNumber
    var isAddContactDialogVisible by viewModel.isAddContactDialogVisible
    var oldContactToEdit by viewModel.oldContactToEdit
    var editContactName by viewModel.editContactName
    var editContactNumber by viewModel.editContactNumber
    var editContactLabel by viewModel.editContactLabel
    var isEditContactDialogVisible by viewModel.isEditContactDialogVisible

    var hasContactsPermission by viewModel.hasContactsPermission
    var hasCallLogPermission by viewModel.hasCallLogPermission
    var hasNotificationPermission by viewModel.hasNotificationPermission
    var isLoadingPermissions by viewModel.isLoadingPermissions
    var showProminentDisclosure by remember { mutableStateOf(false) }

    val systemActiveCall by CallManager.currentCall.collectAsStateWithLifecycle()
    val systemCallState by CallManager.callState.collectAsStateWithLifecycle()
    val systemAudioState by CallManager.audioState.collectAsStateWithLifecycle()
    val systemCallerNumber by CallManager.callerNumber.collectAsStateWithLifecycle()
    val systemCallerCnapName by CallManager.callerCnapName.collectAsStateWithLifecycle()

    var toneGenerator by remember { mutableStateOf<ToneGenerator?>(null) }
    DisposableEffect(Unit) {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
        } catch (_: Exception) {}
        onDispose {
            try { toneGenerator?.release() } catch (_: Exception) {}
            toneGenerator = null
        }
    }

    fun playDtmf(key: String) {
        toneGenerator?.let { tg ->
            try {
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
                if (tone != -1) tg.startTone(tone, 120)
            } catch (_: Throwable) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: false
        hasContactsPermission = contactsGranted
        hasCallLogPermission = callLogGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        }
        isLoadingPermissions = false
        if (contactsGranted || callLogGranted) {
            viewModel.startDataSyncAndObservation()
        }
    }

    LaunchedEffect(Unit) {
        val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val callLogGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

        hasContactsPermission = contactsGranted
        hasCallLogPermission = callLogGranted
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        isLoadingPermissions = false

        if (contactsGranted || callLogGranted) {
            viewModel.startDataSyncAndObservation()
        }

        if (!contactsGranted || !callLogGranted) {
            showProminentDisclosure = true
        }
    }

    LaunchedEffect(systemActiveCall, systemCallState, systemCallerNumber, systemCallerCnapName) {
        if (systemActiveCall != null) {
            isCallActive = true
            if (systemCallerNumber.isNotEmpty()) {
                callingContactNumber = systemCallerNumber
                val contactName = getContactNameFromNumber(context, systemCallerNumber)
                if (contactName != null) {
                    callingContactName = contactName
                } else if (systemCallerCnapName.isNotEmpty()) {
                    callingContactName = systemCallerCnapName
                } else {
                    val savedCnap = getSavedCnapName(context, systemCallerNumber)
                    callingContactName = savedCnap ?: systemCallerNumber
                }
            }
        } else {
            if (isCallActive) {
                delay(650)
            }
            isCallActive = false
        }
    }

    var showSimSelectDialog by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf("") }
    var pendingCallName by remember { mutableStateOf("") }

    fun initiateCall(name: String, number: String, label: String = "Mobile") {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (blockedNumbers.contains(number)) {
            Toast.makeText(context, context.getString(R.string.call_blocked_toast), Toast.LENGTH_LONG).show()
            return
        }

        var resolvedName = name
        val memoryContact = getContactNameFromNumber(context, number)
        val memoryCnap = ContactCache.getCnapName(number)

        if (resolvedName == "Unknown" || resolvedName.isEmpty() || resolvedName == number) {
            if (memoryContact != null) {
                resolvedName = memoryContact
            } else if (!memoryCnap.isNullOrBlank()) {
                resolvedName = memoryCnap
            }
        }

        if (preferredSim == "Ask") {
            pendingCallName = resolvedName
            pendingCallNumber = number
            showSimSelectDialog = true

            if (resolvedName == "Unknown" || resolvedName.isEmpty() || resolvedName == number) {
                coroutineScope.launch {
                    val savedCnap = getSavedCnapName(context, number)
                    if (!savedCnap.isNullOrBlank()) {
                        pendingCallName = savedCnap
                    }
                }
            }
        } else {
            callingContactName = resolvedName
            callingContactNumber = number
            isCallActive = true
            CallManager.placeCall(context, number, preferredSim)

            if (resolvedName == "Unknown" || resolvedName.isEmpty() || resolvedName == number) {
                coroutineScope.launch {
                    val savedCnap = getSavedCnapName(context, number)
                    if (!savedCnap.isNullOrBlank()) {
                        callingContactName = savedCnap
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        val tabSlotLeft by viewModel.tabSlotLeft
        val tabSlotMiddle by viewModel.tabSlotMiddle
        val tabSlotRight by viewModel.tabSlotRight
        val tabSlots = remember(tabSlotLeft, tabSlotMiddle, tabSlotRight) {
            listOf(tabSlotLeft, tabSlotMiddle, tabSlotRight)
        }
        val currentSlotKey = tabSlots.getOrElse(selectedTab) { "RECENTS" }

        BackHandler(enabled = isDialpadVisible || isSettingsVisible) {
            if (isDialpadVisible) {
                isDialpadVisible = false
            } else if (isSettingsVisible) {
                isSettingsVisible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            if (showProminentDisclosure) {
                AlertDialog(
                    onDismissRequest = { showProminentDisclosure = false },
                    title = { Text("Permissions & Local Data Privacy", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Secure Dialer requires access to your Contacts and Call Logs to display call history, identify incoming callers, and allow dialing. All data is processed 100% locally on your device and is never uploaded or shared.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showProminentDisclosure = false
                                val perms = mutableListOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.WRITE_CALL_LOG,
                                    Manifest.permission.CALL_PHONE
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            }
                        ) {
                            Text("Continue")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showProminentDisclosure = false }) {
                            Text("Not Now")
                        }
                    }
                )
            }

            if (showSimSelectDialog) {
                SimSelectDialog(
                    context = context,
                    pendingCallNumber = pendingCallNumber,
                    onDismiss = { showSimSelectDialog = false },
                    onSimSelected = { simLabel ->
                        showSimSelectDialog = false
                        callingContactName = pendingCallName
                        callingContactNumber = pendingCallNumber
                        isCallActive = true
                        CallManager.placeCall(context, pendingCallNumber, simLabel)
                    }
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (!isDefaultDialer) {
                    DefaultDialerWarningCard(onShowRestrictedSettings = onShowRestrictedSettings)
                }

                val isRowSwipeEnabled by viewModel.isRowSwipeEnabled

                AnimatedVisibility(
                    visible = currentSlotKey != "DIALPAD" && !isCallHistoryDetailsOpen,
                    enter = expandVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
                    ) + fadeIn(animationSpec = tween(120)),
                    exit = shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
                    ) + fadeOut(animationSpec = tween(120))
                ) {
                    HeaderSearchBar(
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onSettingsClick = { isSettingsVisible = true }
                    )
                }

                // FIXED: Standard, accessible 3-page pager eliminates infinite carousel jumps and TalkBack bugs
                val pagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, tabSlots.size - 1)) { tabSlots.size }

                LaunchedEffect(pagerState.currentPage) {
                    if (selectedTab != pagerState.currentPage) {
                        selectedTab = pagerState.currentPage
                    }
                }

                LaunchedEffect(selectedTab) {
                    if (pagerState.currentPage != selectedTab) {
                        pagerState.animateScrollToPage(selectedTab)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val currentSlot = tabSlots.getOrElse(pagerState.currentPage) { "RECENTS" }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = !isRowSwipeEnabled || currentSlot != "RECENTS"
                    ) { page ->
                        when (tabSlots.getOrElse(page) { "RECENTS" }) {
                            "RECENTS" -> {
                                val allCallHistory by viewModel.allCallHistoryFlow.collectAsStateWithLifecycle()
                                RecentsTabContent(
                                    viewModel = viewModel,
                                    callRecords = allCallHistory,
                                    onCallClick = { initiateCall(it.name, it.number, it.label) },
                                    onDeleteRecord = { id -> viewModel.deleteCallLog(id) },
                                    hasPermission = hasCallLogPermission,
                                    isLoading = isLoadingPermissions,
                                    onRequestPermission = { showProminentDisclosure = true }
                                )
                            }
                            "CONTACTS" -> {
                                val contactsPaged = viewModel.contactsPaged.collectAsLazyPagingItems()
                                val favoriteContacts by viewModel.favoriteContacts.collectAsStateWithLifecycle()
                                ContactsTabContent(
                                    viewModel = viewModel,
                                    contactsPaged = contactsPaged,
                                    favoriteContacts = favoriteContacts,
                                    onCallClick = { initiateCall(it.name, it.number, it.label) },
                                    onAddContactClick = { isAddContactDialogVisible = true },
                                    onToggleFavorite = { contact -> viewModel.toggleFavorite(contact.number, !contact.favorite) },
                                    hasPermission = hasContactsPermission,
                                    isLoading = isLoadingPermissions,
                                    onRequestPermission = { showProminentDisclosure = true },
                                    onEditContact = {
                                        oldContactToEdit = it
                                        editContactName = it.name
                                        editContactNumber = it.number
                                        editContactLabel = it.label
                                        isEditContactDialogVisible = true
                                    },
                                    onDeleteContact = { viewModel.deleteContact(it) }
                                )
                            }
                            "DIALPAD" -> {
                                val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
                                val voicemailNumber by viewModel.voicemailNumber
                                val speedDialEntities by viewModel.speedDialFlow.collectAsStateWithLifecycle()
                                val speedDialMap = remember(speedDialEntities) { speedDialEntities.associate { it.key to it.number } }
                                val dialpadMatches by viewModel.dialpadMatches.collectAsStateWithLifecycle()

                                DialpadTabContent(
                                    inputValue = dialpadInput,
                                    onValueChange = {
                                        if (it.length > dialpadInput.length) {
                                            if (dialpadTonesEnabled) playDtmf(it.last().toString())
                                        }
                                        viewModel.onDialpadInputChange(it)
                                    },
                                    onCallClick = {
                                        if (it.isNotEmpty()) {
                                            initiateCall("Unknown", it)
                                            viewModel.onDialpadInputChange("")
                                        }
                                    },
                                    onSpeedDialCall = { initiateCall("Speed Dial", it) },
                                    voicemailNumber = voicemailNumber,
                                    speedDialMap = speedDialMap,
                                    dialpadMatches = dialpadMatches,
                                    onCollapseClick = {},
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                if (isCallActive && isCallMinimized) {
                    val isDynamicIsland = viewModel.isDynamicIslandEnabled.value
                    val speakerOnly = viewModel.isDynamicIslandSpeakerOnly.value
                    val isSpeakerOn = systemAudioState?.route == android.telecom.CallAudioState.ROUTE_SPEAKER

                    if (isDynamicIsland && (!speakerOnly || isSpeakerOn)) {
                        DynamicIslandPill(
                            callerName = callingContactName,
                            callerNumber = callingContactNumber,
                            callState = systemCallState,
                            audioState = systemAudioState,
                            onExpandToFullScreen = { isCallMinimized = false },
                            onHangUp = {
                                CallManager.disconnect()
                                isCallActive = false
                            }
                        )
                    } else {
                        MinimizedCallBanner(
                            contactName = callingContactName,
                            contactNumber = callingContactNumber,
                            callState = systemCallState,
                            onExpand = { isCallMinimized = false },
                            onHangUp = {
                                CallManager.disconnect()
                                isCallActive = false
                            }
                        )
                    }
                }

                if (!WindowInsets.isImeVisible) {
                    BottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { targetTab ->
                            selectedTab = targetTab
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetTab)
                            }
                        },
                        tabSlots = tabSlots
                    )
                }
            }

            val isFakeCallActive by viewModel.isFakeCallActive
            val fakeCallerName by viewModel.fakeCallerName
            val fakeCallerNumber by viewModel.fakeCallerNumber
            val fakeCallState by viewModel.fakeCallState

            AnimatedVisibility(
                visible = isSettingsVisible && !isFakeCallActive && (!isCallActive || isCallMinimized),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(150)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(150, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(120))
            ) {
                SettingsPanel(
                    viewModel = viewModel,
                    onClose = { isSettingsVisible = false }
                )
            }

            AnimatedVisibility(
                visible = (isCallActive && !isCallMinimized) || isFakeCallActive,
                enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.95f, animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.95f, animationSpec = tween(100))
            ) {
                val allContacts by viewModel.allContactsFlow.collectAsStateWithLifecycle()
                val quickResponsesEntities by viewModel.quickResponsesFlow.collectAsStateWithLifecycle()
                val quickResponses = remember(quickResponsesEntities) { quickResponsesEntities.map { it.message } }

                ActiveCallScreen(
                    contactName = if (isFakeCallActive) fakeCallerName else callingContactName,
                    contactNumber = if (isFakeCallActive) fakeCallerNumber else callingContactNumber,
                    preferredSim = preferredSim,
                    quickResponses = quickResponses,
                    onHangUp = {
                        if (isFakeCallActive) {
                            try {
                                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(9999)
                            } catch (_: Exception) {}
                            val durationSeconds = if (fakeCallState == "ACTIVE" && viewModel.fakeCallStartTimestamp > 0L) {
                                (System.currentTimeMillis() - viewModel.fakeCallStartTimestamp) / 1000
                            } else {
                                0L
                            }
                            val callType = if (fakeCallState == "ACTIVE") CallType.INCOMING else CallType.MISSED
                            viewModel.logFakeCall(fakeCallerName, fakeCallerNumber, callType, durationSeconds)
                            viewModel.fakeCallStartTimestamp = 0L
                            viewModel.isFakeCallActive.value = false
                            viewModel.fakeCallState.value = "DISCONNECTED"
                        } else {
                            CallManager.disconnect()
                        }
                    },
                    onAnswer = { CallManager.answer() },
                    onQuickDecline = {
                        if (isFakeCallActive) {
                            try {
                                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(9999)
                            } catch (_: Exception) {}
                            viewModel.fakeCallStartTimestamp = 0L
                            viewModel.isFakeCallActive.value = false
                            viewModel.fakeCallState.value = "DISCONNECTED"
                        } else {
                            CallManager.disconnect()
                        }
                    },
                    isIncoming = if (isFakeCallActive) (fakeCallState == "RINGING") else (systemCallState == android.telecom.Call.STATE_RINGING),
                    contacts = allContacts,
                    callState = if (isFakeCallActive) {
                        if (fakeCallState == "ACTIVE") android.telecom.Call.STATE_ACTIVE else android.telecom.Call.STATE_RINGING
                    } else {
                        systemCallState
                    },
                    recordingEnabled = viewModel.recordingEnabled.value,
                    autoTuneVolume = viewModel.autoTuneRecordingVolume.value,
                    recordingChimeEnabled = viewModel.recordingChimeEnabled.value,
                    alwaysRecordEnabled = viewModel.isAutoRecordCallsEnabled.value,
                    callNotesEnabled = viewModel.isCallNotesEnabled.value,
                    onSaveRecording = { duration, filePath ->
                        viewModel.saveCallRecording(
                            CallRecording(
                                name = if (isFakeCallActive) fakeCallerName else callingContactName,
                                number = if (isFakeCallActive) fakeCallerNumber else callingContactNumber,
                                timestamp = System.currentTimeMillis().toString(),
                                duration = duration,
                                filePath = filePath
                            )
                        )
                    },
                    onSaveNote = { content ->
                        viewModel.saveCallNote(if (isFakeCallActive) fakeCallerNumber else callingContactNumber, content)
                    },
                    onMinimize = if (isFakeCallActive) null else { { isCallMinimized = true } },
                    avatarShapeType = viewModel.avatarShapeType.value,
                    isPocketProtectionEnabled = viewModel.isPocketProtectionEnabled.value,
                    isFake = isFakeCallActive,
                    fakeState = fakeCallState,
                    onFakeAnswer = {
                        try {
                            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(9999)
                        } catch (_: Exception) {}
                        viewModel.fakeCallStartTimestamp = System.currentTimeMillis()
                        viewModel.fakeCallState.value = "ACTIVE"
                    },
                    onFakeHangUp = {
                        try {
                            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(9999)
                        } catch (_: Exception) {}
                        val durationSeconds = if (fakeCallState == "ACTIVE" && viewModel.fakeCallStartTimestamp > 0L) {
                            (System.currentTimeMillis() - viewModel.fakeCallStartTimestamp) / 1000
                        } else {
                            0L
                        }
                        val callType = if (fakeCallState == "ACTIVE") CallType.INCOMING else CallType.MISSED
                        viewModel.logFakeCall(fakeCallerName, fakeCallerNumber, callType, durationSeconds)
                        viewModel.fakeCallStartTimestamp = 0L
                        viewModel.isFakeCallActive.value = false
                        viewModel.fakeCallState.value = "DISCONNECTED"
                    }
                )
            }

            MainScreenContactDialogs(viewModel = viewModel)

            val pendingRecNote = viewModel.pendingPostCallRecordingNote.value
            if (pendingRecNote != null) {
                var noteInput by remember(pendingRecNote.id) { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { viewModel.dismissPostCallRecordingNote() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text(stringResource(R.string.jot_call_note_title)) },
                    text = {
                        Column {
                            Text(
                                text = "${pendingRecNote.name.ifEmpty { pendingRecNote.number }} (${pendingRecNote.duration}s recording)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                singleLine = false
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.savePostCallRecordingNote(
                                    pendingRecNote.id,
                                    pendingRecNote.number,
                                    noteInput.trim()
                                )
                                Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(stringResource(R.string.btn_save_note))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissPostCallRecordingNote() }
                        ) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            }

            if (isDialpadVisible) {
                val backdropAlpha by animateFloatAsState(
                    targetValue = 0.5f,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    label = "backdrop_alpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = backdropAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isDialpadVisible = false
                        }
                )
            }

            AnimatedVisibility(
                visible = isDialpadVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                ) + fadeIn(animationSpec = tween(150)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(animationSpec = tween(100)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
                val voicemailNumber by viewModel.voicemailNumber
                val speedDialEntities by viewModel.speedDialFlow.collectAsStateWithLifecycle()
                val speedDialMap = remember(speedDialEntities) { speedDialEntities.associate { it.key to it.number } }

                DialpadOverlay(
                    inputValue = dialpadInput,
                    onValueChange = {
                        if (it.length > dialpadInput.length) {
                            if (dialpadTonesEnabled) playDtmf(it.last().toString())
                        }
                        viewModel.onDialpadInputChange(it)
                    },
                    onClose = { isDialpadVisible = false },
                    onCallClick = { num ->
                        if (num.isNotEmpty()) {
                            initiateCall("Unknown", num)
                            viewModel.onDialpadInputChange("")
                            isDialpadVisible = false
                        }
                    },
                    onSpeedDialCall = { num ->
                        initiateCall("Speed Dial", num)
                        isDialpadVisible = false
                    },
                    speedDialMap = speedDialMap,
                    voicemailNumber = voicemailNumber,
                    viewModel = viewModel
                )
            }
        }
    }
}