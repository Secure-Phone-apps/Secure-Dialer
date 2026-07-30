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
    var isCallMinimized by remember { mutableStateOf(false) }

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
                    val savedCnap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.getSavedCnapName(context, systemCallerNumber)
                    }
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
        if (blockedNumbers.contains(number)) {
            Toast.makeText(context, context.getString(R.string.call_blocked_toast), Toast.LENGTH_LONG).show()
            return
        }
        
        var resolvedName = name
        if (resolvedName == "Unknown" || resolvedName.isEmpty()) {
            val contactName = getContactNameFromNumber(context, number)
            if (contactName != null) {
                resolvedName = contactName
            }
        }
        
        if (preferredSim == "Ask") {
            pendingCallName = resolvedName
            pendingCallNumber = number
            showSimSelectDialog = true
            
            if (resolvedName == "Unknown" || resolvedName.isEmpty()) {
                coroutineScope.launch {
                    val savedCnap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.getSavedCnapName(context, number)
                    }
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
            
            if (resolvedName == "Unknown" || resolvedName.isEmpty()) {
                coroutineScope.launch {
                    val savedCnap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.getSavedCnapName(context, number)
                    }
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showSimSelectDialog) {
                Dialog(
                    onDismissRequest = { showSimSelectDialog = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { showSimSelectDialog = false },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {}
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp, 4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = stringResource(R.string.select_sim_card),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = stringResource(R.string.choose_sim_card, pendingCallNumber),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                val simList = remember { com.example.util.MultiSimManager.getActiveSimAccounts(context) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    simList.take(2).forEachIndexed { index, sim ->
                                        val simLabel = "SIM ${index + 1}"
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showSimSelectDialog = false
                                                    callingContactName = pendingCallName
                                                    callingContactNumber = pendingCallNumber
                                                    isCallActive = true
                                                    CallManager.placeCall(context, pendingCallNumber, simLabel)
                                                },
                                            color = if (index == 0) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            },
                                            contentColor = if (index == 0) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            tonalElevation = 2.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SimCard,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = if (index == 0) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.secondary
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = simLabel,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = sim.carrierName.ifEmpty { sim.displayName },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                TextButton(
                                    onClick = { showSimSelectDialog = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (!isDefaultDialer) {
                    DefaultDialerWarningCard(onShowRestrictedSettings = onShowRestrictedSettings)
                }

                AnimatedVisibility(
                    visible = selectedTab != 2 && !isCallHistoryDetailsOpen,
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

                val pagerState = rememberPagerState(initialPage = selectedTab) { 3 }
                LaunchedEffect(pagerState.currentPage) {
                    if (selectedTab != pagerState.currentPage) {
                        selectedTab = pagerState.currentPage
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> {
                                val allCallHistory by viewModel.allCallHistoryFlow.collectAsState()
                                RecentsTabContent(
                                    viewModel = viewModel,
                                    callRecords = allCallHistory,
                                    onCallClick = { it -> initiateCall(it.name, it.number, it.label) },
                                    onDeleteRecord = { id -> viewModel.deleteCallLog(id) },
                                    hasPermission = hasCallLogPermission, isLoading = isLoadingPermissions,
                                    onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE)) }
                                )
                            }
                            1 -> {
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
                                    onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) },
                                    onEditContact = { it -> oldContactToEdit = it; editContactName = it.name; editContactNumber = it.number; editContactLabel = it.label; isEditContactDialogVisible = true },
                                    onDeleteContact = { it -> viewModel.deleteContact(it.number) }
                                )
                            }
                            2 -> {
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
                        onHangUp = { CallManager.disconnect(); isCallActive = false }
                    )
                }

                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { targetTab ->
                        selectedTab = targetTab
                        coroutineScope.launch {
                            pagerState.scrollToPage(targetTab)
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = isCallActive && !isCallMinimized,
                enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.95f, animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.95f, animationSpec = tween(100))
            ) {
                val allContacts by viewModel.allContactsFlow.collectAsState()
                val quickResponsesEntities by viewModel.quickResponsesFlow.collectAsState()
                val quickResponses = remember(quickResponsesEntities) { quickResponsesEntities.map { it.message } }

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
                    },
                    onMinimize = { isCallMinimized = true }
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

            MainScreenContactDialogs(viewModel = viewModel)
        }
    }
}

@Composable
fun MinimizedCallBanner(
    contactName: String,
    contactNumber: String,
    callState: Int,
    onExpand: () -> Unit,
    onHangUp: () -> Unit
) {
    val activeStartTimestamp by CallManager.activeStartTimestamp.collectAsState()
    var tickTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(callState) {
        if (callState == android.telecom.Call.STATE_ACTIVE) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                tickTrigger++
            }
        }
    }

    val callDuration = remember(activeStartTimestamp, tickTrigger, callState) {
        if (callState == android.telecom.Call.STATE_ACTIVE) {
            val start = if (activeStartTimestamp > 0L) activeStartTimestamp else System.currentTimeMillis()
            val durationMs = System.currentTimeMillis() - start
            (durationMs / 1000).coerceAtLeast(0L).toInt()
        } else {
            0
        }
    }

    val formattedTime = remember(callDuration) {
        val mins = callDuration / 60
        val secs = callDuration % 60
        "%02d:%02d".format(mins, secs)
    }

    val statusText = when (callState) {
        android.telecom.Call.STATE_RINGING -> "Incoming..."
        android.telecom.Call.STATE_DIALING -> "Dialing..."
        android.telecom.Call.STATE_CONNECTING -> "Connecting..."
        android.telecom.Call.STATE_HOLDING -> "On Hold"
        else -> formattedTime
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing indicator
                Box(contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scale"
                    )
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                            .background(
                                color = if (callState == android.telecom.Call.STATE_RINGING) Color.Red else Color.Green,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (callState == android.telecom.Call.STATE_RINGING) Color.Red else Color.Green
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = contactName.ifEmpty { contactNumber },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand Call Screen",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onHangUp,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

