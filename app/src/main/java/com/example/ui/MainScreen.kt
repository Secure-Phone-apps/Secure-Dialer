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
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.CallManager
import com.example.getContactNameFromNumber
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: DialerViewModel,
    onShowRestrictedSettings: () -> Unit,
    isDefaultDialer: Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Localized state declarations for MainScreen
    val preferredSim by viewModel.preferredSim
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsState()
    val blockedNumbers = remember(blockedNumbersEntities) { blockedNumbersEntities.map { it.number } }

    var selectedTab by viewModel.selectedTab
    val searchQuery by viewModel.searchQuery
    val isCallHistoryDetailsOpen by viewModel.isCallHistoryDetailsOpen
    var isDialpadVisible by viewModel.isDialpadVisible
    var dialpadInput by viewModel.dialpadInput
    var isSettingsVisible by viewModel.isSettingsVisible
    var isCallActive by viewModel.isCallActive
    var isCallMinimized by viewModel.isCallMinimized

    // Automatically dismiss keyboard and clear focus on tab changes, settings visibility changes, and call transitions
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
    var newContactName by viewModel.newContactName
    var newContactNumber by viewModel.newContactNumber
    var newContactLabel by viewModel.newContactLabel
    var isEditContactDialogVisible by viewModel.isEditContactDialogVisible
    var oldContactToEdit by viewModel.oldContactToEdit
    var editContactName by viewModel.editContactName
    var editContactNumber by viewModel.editContactNumber
    var editContactLabel by viewModel.editContactLabel
    
    // Permission state
    var hasContactsPermission by viewModel.hasContactsPermission
    var hasCallLogPermission by viewModel.hasCallLogPermission
    var hasNotificationPermission by viewModel.hasNotificationPermission
    var isLoadingPermissions by viewModel.isLoadingPermissions
    var showProminentDisclosure by remember { mutableStateOf(false) }

    // Real-time Telecom Call observers
    val systemActiveCall by CallManager.currentCall.collectAsState()
    val systemCallState by CallManager.callState.collectAsState()
    val systemCallerNumber by CallManager.callerNumber.collectAsState()
    val systemCallerCnapName by CallManager.callerCnapName.collectAsState()

    // DTMF Tone Generator
    val toneGenerator = remember {
        try { ToneGenerator(AudioManager.STREAM_DTMF, 80) } catch (e: Exception) { null }
    }
    DisposableEffect(Unit) {
        onDispose { try { toneGenerator?.release() } catch (e: Exception) { e.printStackTrace() } }
    }

    fun playDtmf(key: String) {
        toneGenerator?.let { tg ->
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
                    val savedCnap = com.example.getSavedCnapName(context, systemCallerNumber)
                    callingContactName = savedCnap ?: systemCallerNumber
                }
            }
        } else {
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
        val memoryCnap = com.example.ContactCache.getCnapName(number)
        
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
                    val savedCnap = com.example.getSavedCnapName(context, number)
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
                    val savedCnap = com.example.getSavedCnapName(context, number)
                    if (!savedCnap.isNullOrBlank()) {
                        callingContactName = savedCnap
                    }
                }
            }
        }
    }

    val isDark by viewModel.isDarkTheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
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

                val tabSlotLeft by viewModel.tabSlotLeft
                val tabSlotMiddle by viewModel.tabSlotMiddle
                val tabSlotRight by viewModel.tabSlotRight
                val isRowSwipeEnabled by viewModel.isRowSwipeEnabled
                val tabSlots = listOf(tabSlotLeft, tabSlotMiddle, tabSlotRight)
                val currentSlotKey = tabSlots.getOrElse(selectedTab) { "RECENTS" }

                AnimatedVisibility(
                    visible = currentSlotKey != "DIALPAD" && !isCallHistoryDetailsOpen,
                    enter = expandVertically(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    ) + fadeIn(animationSpec = tween(120)),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    ) + fadeOut(animationSpec = tween(120))
                ) {
                    HeaderSearchBar(
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onSettingsClick = { isSettingsVisible = true }
                    )
                }

                val virtualPageCount = 3000
                val initialVirtualPage = 1500
                val pagerState = rememberPagerState(initialPage = initialVirtualPage + selectedTab) { virtualPageCount }
                
                LaunchedEffect(pagerState.currentPage) {
                    val activeIndex = pagerState.currentPage % tabSlots.size
                    if (selectedTab != activeIndex) {
                        selectedTab = activeIndex
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val currentVirtualSlot = tabSlots.getOrElse(pagerState.currentPage % tabSlots.size) { "RECENTS" }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = !isRowSwipeEnabled || currentVirtualSlot != "RECENTS"
                    ) { page ->
                        val slotIndex = page % tabSlots.size
                        val slotKey = tabSlots.getOrElse(slotIndex) { "RECENTS" }
                        when (slotKey) {
                            "RECENTS" -> {
                                val allCallHistory by viewModel.allCallHistoryFlow.collectAsState()
                                RecentsTabContent(
                                    viewModel = viewModel,
                                    callRecords = allCallHistory,
                                    onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                                    onDeleteRecord = { id -> viewModel.deleteCallLog(id) },
                                    hasPermission = hasCallLogPermission, isLoading = isLoadingPermissions,
                                    onRequestPermission = { showProminentDisclosure = true }
                                )
                            }
                            "CONTACTS" -> {
                                val contactsPaged = viewModel.contactsPaged.collectAsLazyPagingItems()
                                val favoriteContacts by viewModel.favoriteContacts.collectAsState()
                                ContactsTabContent(
                                    viewModel = viewModel,
                                    contactsPaged = contactsPaged,
                                    favoriteContacts = favoriteContacts,
                                    onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                                    onAddContactClick = { isAddContactDialogVisible = true },
                                    onToggleFavorite = { contact -> viewModel.toggleFavorite(contact.number, !contact.favorite) },
                                    hasPermission = hasContactsPermission, isLoading = isLoadingPermissions,
                                    onRequestPermission = { showProminentDisclosure = true },
                                    onEditContact = { it -> oldContactToEdit = it; editContactName = it.name; editContactNumber = it.number; editContactLabel = it.label; isEditContactDialogVisible = true },
                                    onDeleteContact = { it -> viewModel.deleteContact(it) }
                                )
                            }
                            "DIALPAD" -> {
                                val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
                                val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
                                val voicemailNumber by viewModel.voicemailNumber
                                val speedDialEntities by viewModel.speedDialFlow.collectAsState()
                                val speedDialMap = remember(speedDialEntities) { speedDialEntities.associate { it.key to it.number } }
                                val dialpadMatches by viewModel.dialpadMatches.collectAsState()

                                DialpadTabContent(
                                    inputValue = dialpadInput,
                                    onValueChange = {
                                        if (it.length > dialpadInput.length) {
                                            if (dialpadTonesEnabled) playDtmf(it.last().toString())
                                            if (vibrateOnClickEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        viewModel.onDialpadInputChange(it)
                                    } ,
                                    onCallClick = { it -> if (it.isNotEmpty()) { initiateCall("Unknown", it); viewModel.onDialpadInputChange("") } },
                                    onSpeedDialCall = { it -> initiateCall("Speed Dial", it) },
                                    voicemailNumber = voicemailNumber, speedDialMap = speedDialMap,
                                    dialpadMatches = dialpadMatches,
                                    onCollapseClick = {},
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                if (isCallActive && isCallMinimized) {
                    MinimizedCallBanner(
                        contactName = callingContactName,
                        contactNumber = callingContactNumber,
                        callState = systemCallState,
                        onExpand = { isCallMinimized = false },
                        onHangUp = {
                            CallManager.disconnect()
                            if (CallManager.calls.value.none { it != CallManager.currentCall.value && it.state != android.telecom.Call.STATE_DISCONNECTED }) {
                                isCallActive = false
                            }
                        }
                    )
                }

                if (!WindowInsets.isImeVisible) {
                    BottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { targetTab ->
                            val currentVirtualPage = pagerState.currentPage
                            val currentTab = currentVirtualPage % tabSlots.size
                            val tabDiff = targetTab - currentTab
                            val shortestDiff = when (tabDiff) {
                                2 -> -1
                                -2 -> 1
                                else -> tabDiff
                            }
                            val targetVirtualPage = currentVirtualPage + shortestDiff
                            selectedTab = targetTab
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetVirtualPage)
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
                    animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(150)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(150, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(120))
            ) {
                SettingsPanel(
                    viewModel = viewModel, onClose = { isSettingsVisible = false }
                )
            }

            AnimatedVisibility(
                visible = (isCallActive && !isCallMinimized) || isFakeCallActive,
                enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.95f, animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.95f, animationSpec = tween(100))
            ) {
                val allContacts by viewModel.allContactsFlow.collectAsState()
                val quickResponsesEntities by viewModel.quickResponsesFlow.collectAsState()
                val quickResponses = remember(quickResponsesEntities) { quickResponsesEntities.map { it.message } }

                ActiveCallScreen(
                    contactName = if (isFakeCallActive) fakeCallerName else callingContactName,
                    contactNumber = if (isFakeCallActive) fakeCallerNumber else callingContactNumber,
                    preferredSim = preferredSim,
                    quickResponses = quickResponses,
                    onHangUp = {
                        if (isFakeCallActive) {
                            try {
                                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                                nm?.cancel(9999)
                            } catch (_: Exception) {}
                            val durationSeconds = if (fakeCallState == "ACTIVE" && viewModel.fakeCallStartTimestamp > 0L) {
                                (System.currentTimeMillis() - viewModel.fakeCallStartTimestamp) / 1000
                            } else {
                                0L
                            }
                            val callType = if (fakeCallState == "ACTIVE") com.example.model.CallType.INCOMING else com.example.model.CallType.MISSED
                            viewModel.logFakeCall(fakeCallerName, fakeCallerNumber, callType, durationSeconds)
                            viewModel.fakeCallStartTimestamp = 0L
                            viewModel.isFakeCallActive.value = false
                            viewModel.fakeCallState.value = "DISCONNECTED"
                        } else {
                            CallManager.disconnect()
                            if (CallManager.calls.value.none { it != CallManager.currentCall.value && it.state != android.telecom.Call.STATE_DISCONNECTED }) {
                                isCallActive = false
                            }
                        }
                    },
                    onAnswer = { CallManager.answer() },
                    onQuickDecline = {
                        if (isFakeCallActive) {
                            try {
                                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                                nm?.cancel(9999)
                            } catch (_: Exception) {}
                            val durationSeconds = if (fakeCallState == "ACTIVE" && viewModel.fakeCallStartTimestamp > 0L) {
                                (System.currentTimeMillis() - viewModel.fakeCallStartTimestamp) / 1000
                            } else {
                                0L
                            }
                            val callType = if (fakeCallState == "ACTIVE") com.example.model.CallType.INCOMING else com.example.model.CallType.MISSED
                            viewModel.logFakeCall(fakeCallerName, fakeCallerNumber, callType, durationSeconds)
                            viewModel.fakeCallStartTimestamp = 0L
                            viewModel.isFakeCallActive.value = false
                            viewModel.fakeCallState.value = "DISCONNECTED"
                        } else {
                            CallManager.disconnect()
                            if (CallManager.calls.value.none { it != CallManager.currentCall.value && it.state != android.telecom.Call.STATE_DISCONNECTED }) {
                                isCallActive = false
                            }
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
                    onSaveRecording = { duration, filePath ->
                        viewModel.saveCallRecording(
                            com.example.model.CallRecording(
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
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                            nm?.cancel(9999)
                        } catch (_: Exception) {}
                        viewModel.fakeCallStartTimestamp = System.currentTimeMillis()
                        viewModel.fakeCallState.value = "ACTIVE" 
                    },
                    onFakeHangUp = { 
                        try {
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                            nm?.cancel(9999)
                        } catch (_: Exception) {}
                        val durationSeconds = if (fakeCallState == "ACTIVE" && viewModel.fakeCallStartTimestamp > 0L) {
                            (System.currentTimeMillis() - viewModel.fakeCallStartTimestamp) / 1000
                        } else {
                            0L
                        }
                        val callType = if (fakeCallState == "ACTIVE") com.example.model.CallType.INCOMING else com.example.model.CallType.MISSED
                        viewModel.logFakeCall(fakeCallerName, fakeCallerNumber, callType, durationSeconds)
                        viewModel.fakeCallStartTimestamp = 0L
                        viewModel.isFakeCallActive.value = false 
                        viewModel.fakeCallState.value = "DISCONNECTED" 
                    }
                )
            }

            MainScreenContactDialogs(viewModel = viewModel)
        }
    }
}

