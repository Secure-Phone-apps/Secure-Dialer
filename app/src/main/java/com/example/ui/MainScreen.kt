package com.example.ui

import android.Manifest
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: DialerViewModel,
    onShowRestrictedSettings: () -> Unit,
    isDefaultDialer: Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Paging Items
    val contactsPaged = viewModel.contactsPaged.collectAsLazyPagingItems()
    val callHistoryPaged = viewModel.callHistoryPaged.collectAsLazyPagingItems()

    // State from ViewModel
    val isDarkTheme by viewModel.isDarkTheme
    val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
    val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
    val preferredSim by viewModel.preferredSim
    val voicemailNumber by viewModel.voicemailNumber
    
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsState()
    val blockedNumbers = remember(blockedNumbersEntities) { blockedNumbersEntities.map { it.number } }
    
    val quickResponsesEntities by viewModel.quickResponsesFlow.collectAsState()
    val quickResponses = remember(quickResponsesEntities) { quickResponsesEntities.map { it.message } }
    
    val speedDialEntities by viewModel.speedDialFlow.collectAsState()
    val speedDialMap = remember(speedDialEntities) { speedDialEntities.associate { it.key to it.number } }

    val favoriteContacts by viewModel.favoriteContacts.collectAsState()
    val allContacts by viewModel.allContactsFlow.collectAsState()

    var selectedTab by viewModel.selectedTab
    val searchQuery by viewModel.searchQuery

    val filteredFavorites = remember(favoriteContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            favoriteContacts
        } else {
            favoriteContacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.number.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    var isDialpadVisible by viewModel.isDialpadVisible
    var dialpadInput by viewModel.dialpadInput
    var isSettingsVisible by viewModel.isSettingsVisible
    var isCallActive by viewModel.isCallActive
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

    // Real-time Telecom Call observers
    val systemActiveCall by CallManager.currentCall.collectAsState()
    val systemCallState by CallManager.callState.collectAsState()
    val systemCallerNumber by CallManager.callerNumber.collectAsState()

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifPermission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, notifPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
                    Manifest.permission.CALL_PHONE, notifPermission
                ))
            }
        }
    }

    LaunchedEffect(systemActiveCall, systemCallState, systemCallerNumber) {
        if (systemActiveCall != null) {
            isCallActive = true
            if (systemCallerNumber.isNotEmpty()) {
                callingContactNumber = systemCallerNumber
                callingContactName = getContactNameFromNumber(context, systemCallerNumber) ?: systemCallerNumber
            }
        } else {
            isCallActive = false
        }
    }

    fun initiateCall(name: String, number: String, label: String = "Mobile") {
        if (blockedNumbers.contains(number)) {
            Toast.makeText(context, context.getString(R.string.call_blocked_toast), Toast.LENGTH_LONG).show()
            return
        }
        callingContactName = name
        callingContactNumber = number
        isCallActive = true
        CallManager.placeCall(context, number, preferredSim)
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val ambientBgBrush = remember(isDark, primaryColor, bgColor, primaryContainer) {
        val startColor = bgColor
        val endColor = if (isDark) {
            primaryContainer.copy(alpha = 0.06f)
        } else {
            primaryContainer.copy(alpha = 0.12f)
        }
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(startColor, endColor)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(ambientBgBrush),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isDefaultDialer) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onShowRestrictedSettings() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, stringResource(R.string.warning), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(16.dp))
                            Text(stringResource(R.string.default_dialer_warning), color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                if (selectedTab != 3) {
                    HeaderSearchBar(
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onSettingsClick = { isSettingsVisible = true }
                    )
                }

                val pagerState = rememberPagerState(initialPage = selectedTab) { 4 }
                LaunchedEffect(selectedTab) {
                    if (pagerState.currentPage != selectedTab) {
                        pagerState.scrollToPage(page = selectedTab)
                    }
                }
                LaunchedEffect(pagerState.currentPage) {
                    if (selectedTab != pagerState.currentPage) {
                        selectedTab = pagerState.currentPage
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> FavoritesTabContent(
                                contacts = filteredFavorites,
                                onCallClick = { name, number -> initiateCall(name, number) },
                                onToggleFavorite = { contact -> viewModel.toggleFavorite(contact.number, !contact.favorite) }
                            )
                            1 -> RecentsTabContent(
                                viewModel = viewModel,
                                callRecordsPaged = callHistoryPaged,
                                onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                                onDeleteRecord = { id -> viewModel.deleteCallLog(id) },
                                hasPermission = hasCallLogPermission, isLoading = isLoadingPermissions,
                                onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE)) }
                            )
                            2 -> ContactsTabContent(
                                contactsPaged = contactsPaged,
                                onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                                onAddContactClick = { isAddContactDialogVisible = true },
                                onToggleFavorite = { contact -> viewModel.toggleFavorite(contact.number, !contact.favorite) },
                                hasPermission = hasContactsPermission, isLoading = isLoadingPermissions,
                                onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) },
                                onEditContact = { it -> oldContactToEdit = it; editContactName = it.name; editContactNumber = it.number; editContactLabel = it.label; isEditContactDialogVisible = true },
                                onDeleteContact = { it -> viewModel.deleteContact(it.number) }
                            )
                            3 -> DialpadTabContent(
                                inputValue = dialpadInput,
                                onValueChange = {
                                    if (it.length > dialpadInput.length) {
                                        if (dialpadTonesEnabled) playDtmf(it.last().toString())
                                        if (vibrateOnClickEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    dialpadInput = it
                                    viewModel.onSearchQueryChange(it)
                                },
                                onCallClick = { it -> if (it.isNotEmpty()) { initiateCall("Unknown", it); dialpadInput = "" } },
                                onSpeedDialCall = { it -> initiateCall("Speed Dial", it) },
                                voicemailNumber = voicemailNumber, speedDialMap = speedDialMap,
                                contactsPaged = contactsPaged
                            )
                        }
                    }
                }

                BottomNavBar(
                    selectedTab = selectedTab, onTabSelected = { selectedTab = it }
                )
            }

            AnimatedVisibility(
                visible = isCallActive,
                enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.95f, animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.95f, animationSpec = tween(100))
            ) {
                ActiveCallScreen(
                    contactName = callingContactName, contactNumber = callingContactNumber,
                    preferredSim = preferredSim, quickResponses = quickResponses,
                    onHangUp = { CallManager.disconnect(); isCallActive = false },
                    onAnswer = { CallManager.answer() },
                    onQuickDecline = { CallManager.disconnect(); isCallActive = false },
                    isIncoming = (systemCallState == android.telecom.Call.STATE_RINGING),
                    contacts = allContacts, callState = systemCallState,
                    recordingEnabled = viewModel.recordingEnabled.value,
                    onSaveRecording = { duration, filePath ->
                        viewModel.saveCallRecording(
                            com.example.model.CallRecording(
                                name = callingContactName,
                                number = callingContactNumber,
                                timestamp = System.currentTimeMillis().toString(),
                                duration = duration,
                                filePath = filePath
                            )
                        )
                    },
                    onSaveNote = { content ->
                        viewModel.saveCallNote(callingContactNumber, content)
                    }
                )
            }

            AnimatedVisibility(
                visible = isSettingsVisible,
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

        if (isAddContactDialogVisible) {
            AddContactDialog(
                initialName = "",
                initialNumber = "",
                initialLabel = "Mobile",
                initialEmail = "",
                onDismiss = { isAddContactDialogVisible = false },
                onConfirm = { name, number, label, email ->
                    viewModel.addContact(name, number, label, email)
                    isAddContactDialogVisible = false
                }
            )
        }

        if (isEditContactDialogVisible && oldContactToEdit != null) {
            AddContactDialog(
                initialName = oldContactToEdit!!.name,
                initialNumber = oldContactToEdit!!.number,
                initialLabel = oldContactToEdit!!.label,
                initialEmail = oldContactToEdit!!.email,
                onDismiss = { isEditContactDialogVisible = false },
                onConfirm = { name, number, label, email ->
                    // Use delete then add for simple update in this context
                    viewModel.deleteContact(oldContactToEdit!!.number)
                    viewModel.addContact(name, number, label, email)
                    isEditContactDialogVisible = false
                }
            )
        }
        }
    }
}

