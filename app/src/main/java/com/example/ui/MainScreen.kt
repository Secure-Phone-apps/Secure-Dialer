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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
    val blockedNumbers = viewModel.blockedNumbers
    val quickResponses = viewModel.quickResponses
    val speedDialMap = viewModel.speedDialMap
    var selectedTab by viewModel.selectedTab
    val searchQuery by viewModel.searchQuery
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

    // Theme-aware styles
    val currentBrandBlue = MaterialTheme.colorScheme.primary
    val currentSoftBlueBg = MaterialTheme.colorScheme.background
    val currentActiveBluePill = MaterialTheme.colorScheme.tertiary
    val currentSearchBarBg = MaterialTheme.colorScheme.surfaceVariant
    val currentGrayText = MaterialTheme.colorScheme.onSurfaceVariant
    val currentPrimaryDarkText = MaterialTheme.colorScheme.onSurface
    val currentNavBg = MaterialTheme.colorScheme.surface

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
        hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false
        hasCallLogPermission = permissions[Manifest.permission.READ_CALL_LOG] ?: false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        }
        isLoadingPermissions = false
        if (hasContactsPermission || hasCallLogPermission) {
            viewModel.startDataSyncAndObservation()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            hasContactsPermission = true
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            hasCallLogPermission = true
        }
        
        if (hasContactsPermission || hasCallLogPermission) {
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
            Toast.makeText(context, "🚫 Call Blocked", Toast.LENGTH_LONG).show()
            return
        }
        callingContactName = name
        callingContactNumber = number
        isCallActive = true
        CallManager.placeCall(context, number)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = currentSoftBlueBg,
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
                            Icon(Icons.Default.Warning, "Warning", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(16.dp))
                            Text("Set as default dialer to enable all features.", color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                HeaderSearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSettingsClick = { isSettingsVisible = true },
                    onProfileClick = { isSettingsVisible = true }
                )

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (selectedTab) {
                        0 -> RecentsTabContent(
                            callRecordsPaged = callHistoryPaged,
                            onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                            onDeleteRecord = { id -> viewModel.deleteContact(id.toString()) }, // This should be deleteCallLog but VM needs update
                            primaryText = currentPrimaryDarkText, secondaryText = currentGrayText,
                            activePill = currentActiveBluePill, brandBlue = currentBrandBlue,
                            hasPermission = hasCallLogPermission, isLoading = isLoadingPermissions,
                            onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE)) }
                        )
                        1 -> ContactsTabContent(
                            contactsPaged = contactsPaged,
                            onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                            onAddContactClick = { isAddContactDialogVisible = true },
                            onToggleFavorite = { contact -> viewModel.toggleFavorite(contact.number, !contact.favorite) },
                            primaryText = currentPrimaryDarkText, secondaryText = currentGrayText,
                            activePill = currentActiveBluePill, brandBlue = currentBrandBlue,
                            hasPermission = hasContactsPermission, isLoading = isLoadingPermissions,
                            onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) },
                            onEditContact = { it -> oldContactToEdit = it; editContactName = it.name; editContactNumber = it.number; editContactLabel = it.label; isEditContactDialogVisible = true },
                            onDeleteContact = { it -> viewModel.deleteContact(it.number) }
                        )
                        2 -> DialpadTabContent(
                            inputValue = dialpadInput,
                            onValueChange = {
                                if (it.length > dialpadInput.length) {
                                    if (dialpadTonesEnabled) playDtmf(it.last().toString())
                                    if (vibrateOnClickEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                dialpadInput = it
                                viewModel.onSearchQueryChange(it) // T9 Search trigger
                            },
                            onCallClick = { it -> if (it.isNotEmpty()) { initiateCall("Unknown", it); dialpadInput = "" } },
                            onSpeedDialCall = { it -> initiateCall("Speed Dial", it) },
                            voicemailNumber = voicemailNumber, speedDialMap = speedDialMap,
                            activePill = currentActiveBluePill, searchBg = currentSearchBarBg,
                            primaryText = currentPrimaryDarkText, grayText = currentGrayText, 
                            contactsPaged = contactsPaged
                        )
                    }
                }

                BottomNavBar(
                    selectedTab = selectedTab, onTabSelected = { selectedTab = it }
                )
            }

            AnimatedVisibility(visible = isCallActive, enter = fadeIn(), exit = fadeOut()) {
                ActiveCallScreen(
                    contactName = callingContactName, contactNumber = callingContactNumber,
                    preferredSim = preferredSim, quickResponses = quickResponses,
                    onHangUp = { CallManager.disconnect(); isCallActive = false },
                    onAnswer = { CallManager.answer() },
                    onQuickDecline = { CallManager.disconnect(); isCallActive = false },
                    isDarkTheme = isDarkTheme, isIncoming = (systemCallState == android.telecom.Call.STATE_RINGING),
                    contacts = emptyList(), activePill = currentActiveBluePill, callState = systemCallState
                )
            }

            AnimatedVisibility(visible = isSettingsVisible, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                SettingsPanel(
                    viewModel = viewModel, onClose = { isSettingsVisible = false },
                    primaryText = currentPrimaryDarkText, secondaryText = currentGrayText,
                    navBg = currentNavBg, searchBg = currentSearchBarBg, brandBlue = currentBrandBlue, activePill = currentActiveBluePill
                )
            }

            if (isAddContactDialogVisible) {
                AddContactDialog(
                    name = newContactName, onNameChange = { newContactName = it },
                    number = newContactNumber, onNumberChange = { newContactNumber = it },
                    label = newContactLabel, onLabelChange = { newContactLabel = it },
                    onDismiss = { isAddContactDialogVisible = false },
                    onConfirm = {
                        viewModel.addContact(newContactName, newContactNumber, newContactLabel)
                        isAddContactDialogVisible = false
                    },
                    softBlueBg = currentSoftBlueBg, activeBluePill = currentActiveBluePill,
                    searchBarBg = currentSearchBarBg, primaryDarkText = currentPrimaryDarkText,
                    brandBlue = currentBrandBlue, grayText = currentGrayText
                )
            }

            if (isEditContactDialogVisible && oldContactToEdit != null) {
                AddContactDialog(
                    name = editContactName, onNameChange = { editContactName = it },
                    number = editContactNumber, onNumberChange = { editContactNumber = it },
                    label = editContactLabel, onLabelChange = { editContactLabel = it },
                    onDismiss = { isEditContactDialogVisible = false },
                    onConfirm = {
                        // Use delete then add for simple update in this mock context or add updateContact to repo
                        viewModel.deleteContact(oldContactToEdit!!.number)
                        viewModel.addContact(editContactName, editContactNumber, editContactLabel)
                        isEditContactDialogVisible = false
                    },
                    softBlueBg = currentSoftBlueBg, activeBluePill = currentActiveBluePill,
                    searchBarBg = currentSearchBarBg, primaryDarkText = currentPrimaryDarkText,
                    brandBlue = currentBrandBlue, grayText = currentGrayText
                )
            }
        }
    }
}

