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

package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.paging.*
import com.example.DialerRepository
import com.example.*
import com.example.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DialerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DialerRepository(application)
    private val prefs = repository.context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

    // Search Query
    var searchQuery = mutableStateOf("")
    private val _searchQueryFlow = MutableStateFlow("")

    // Account Source Filter & Default Contact Account
    private val initialAccountFilter = prefs.getString("selected_account_filter", "") ?: ""
    var selectedAccountFilter = mutableStateOf(initialAccountFilter)
    private val _selectedAccountFilterFlow = MutableStateFlow(initialAccountFilter)
    var availableAccounts = mutableStateListOf<ContactAccount>()
    var defaultContactAccountName = mutableStateOf(prefs.getString("default_contact_account_name", "") ?: "")
    var defaultContactAccountType = mutableStateOf(prefs.getString("default_contact_account_type", "") ?: "")

    // Dialpad Input Flow
    private val _dialpadInputFlow = MutableStateFlow("")

    // Details Screen state (to hide global search bar)
    var isCallHistoryDetailsOpen = mutableStateOf(false)

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
        _searchQueryFlow.value = newQuery
    }

    fun onAccountFilterChange(accountName: String) {
        selectedAccountFilter.value = accountName
        _selectedAccountFilterFlow.value = accountName
        prefs.edit().putString("selected_account_filter", accountName).apply()
    }

    fun updateDefaultContactAccount(accountName: String, accountType: String) {
        defaultContactAccountName.value = accountName
        defaultContactAccountType.value = accountType
        prefs.edit()
            .putString("default_contact_account_name", accountName)
            .putString("default_contact_account_type", accountType)
            .apply()
    }

    fun onDialpadInputChange(newInput: String) {
        dialpadInput.value = newInput
        _dialpadInputFlow.value = newInput
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val contactsPaged: Flow<PagingData<Contact>> = combine(_searchQueryFlow.debounce(100), _selectedAccountFilterFlow) { query, account ->
        Pair(query, account)
    }.flatMapLatest { (query, account) ->
        repository.getContactsPaged(query, account)
    }.cachedIn(viewModelScope)

    val callHistoryPaged: Flow<PagingData<CallRecord>> = repository.getCallHistoryPaged()
        .cachedIn(viewModelScope)

    val favoriteContacts: StateFlow<List<Contact>> = repository.getFavoriteContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContactsFlow: StateFlow<List<Contact>> = repository.getAllContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCallHistoryFlow: StateFlow<List<CallRecord>> = repository.getAllCallHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val dialpadMatches: StateFlow<List<DialpadMatch>> = combine(
        allContactsFlow,
        allCallHistoryFlow,
        _dialpadInputFlow.debounce(100)
    ) { contacts, recents, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val matchedContacts = contacts.asSequence().filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                contact.number.contains(query, ignoreCase = true) ||
                contact.t9Mapping.contains(query, ignoreCase = true)
            }.map { contact ->
                DialpadMatch(
                    number = contact.number,
                    name = contact.name,
                    label = contact.label,
                    avatarText = contact.avatarText,
                    avatarBgValue = contact.avatarBgValue,
                    avatarTextColorValue = contact.avatarTextColorValue,
                    isFromContacts = true,
                    isFromRecents = false,
                    photoUri = contact.photoUri
                )
            }.take(15).toList()

            if (matchedContacts.size >= 15) {
                matchedContacts
            } else {
                val contactNumbers = matchedContacts.map { it.number }.toSet()
                val matchedRecents = recents.asSequence().filter { record ->
                    record.number !in contactNumbers &&
                    (record.name.contains(query, ignoreCase = true) ||
                     record.number.contains(query, ignoreCase = true))
                }.distinctBy { it.number }
                .map { record ->
                    DialpadMatch(
                        number = record.number,
                        name = record.name,
                        label = "Recent • ${record.label}",
                        avatarText = record.avatarText,
                        avatarBgValue = record.avatarBgValue,
                        avatarTextColorValue = record.avatarTextColorValue,
                        isFromContacts = false,
                        isFromRecents = true,
                        photoUri = record.photoUri
                    )
                }.take(15 - matchedContacts.size).toList()

                matchedContacts + matchedRecents
            }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State
    var isDialpadVisible = mutableStateOf(false)
    var dialpadInput = mutableStateOf("")
    var isSettingsVisible = mutableStateOf(false)
    var isDarkTheme = mutableStateOf(prefs.getBoolean("is_dark_theme", true))
    var isAmoledMode = mutableStateOf(prefs.getBoolean("is_amoled_mode", false))
    var customColorHex = mutableStateOf(prefs.getString("custom_color_hex", "#68A500") ?: "#68A500")
    var isM3Expressive = mutableStateOf(prefs.getBoolean("is_m3_expressive", true))
    var avatarShapeType = mutableStateOf(prefs.getString("avatar_shape_type", "circular") ?: "circular")
    var themeColor = mutableStateOf(prefs.getString("theme_color", "expressive_lime") ?: "expressive_lime")
    var useDynamicColor = mutableStateOf(prefs.getBoolean("use_dynamic_color", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S))
    var defaultTab = mutableIntStateOf(prefs.getInt("default_tab", 0).coerceIn(0, 2))
    var callWaitingEnabled = mutableStateOf(prefs.getBoolean("call_waiting_enabled", true))
    var recordingEnabled = mutableStateOf(prefs.getBoolean("recording_enabled", false))
    var isBiometricLockEnabled = mutableStateOf(prefs.getBoolean("is_biometric_lock_enabled", false))
    var isPocketProtectionEnabled = mutableStateOf(prefs.getBoolean("is_pocket_protection_enabled", false))
    var selectedTab = mutableIntStateOf(prefs.getInt("default_tab", 0).coerceIn(0, 2))
    var flashAlertsEnabled = mutableStateOf(prefs.getBoolean("flash_alerts_enabled", false))

    // Dashboard & Tab Layout & Swipe Preferences
    var dashboardMode = mutableStateOf(prefs.getString("dashboard_mode", "FULL") ?: "FULL")
    var isCallLogDashboardEnabled = mutableStateOf(prefs.getBoolean("is_call_log_dashboard_enabled", true))
    var isCallLogFiltersEnabled = mutableStateOf(prefs.getBoolean("is_call_log_filters_enabled", true))
    var tabSlotLeft = mutableStateOf(prefs.getString("tab_slot_left", "RECENTS") ?: "RECENTS")
    var tabSlotMiddle = mutableStateOf(prefs.getString("tab_slot_middle", "CONTACTS") ?: "CONTACTS")
    var tabSlotRight = mutableStateOf(prefs.getString("tab_slot_right", "DIALPAD") ?: "DIALPAD")
    var isRowSwipeEnabled = mutableStateOf(prefs.getBoolean("is_row_swipe_enabled", true))
    var lastDialedNumber = mutableStateOf(prefs.getString("last_dialed_number", "") ?: "")

    // Fake Call Simulation State
    var isFakeCallActive = mutableStateOf(false)
    var fakeCallerName = mutableStateOf("Unknown")
    var fakeCallerNumber = mutableStateOf("Unknown")
    var fakeCallState = mutableStateOf("RINGING")
    var fakeCallStartTimestamp = 0L
    var dialpadTonesEnabled = mutableStateOf(true)
    var vibrateOnClickEnabled = mutableStateOf(true)
    var preferredSim = mutableStateOf("SIM 1")
    var voicemailNumber = mutableStateOf("+1 (555) 011-9988")
    
    val blockedNumbers = mutableStateListOf<String>()
    val quickResponses = mutableStateListOf<String>()
    val speedDialMap = mutableStateMapOf<Int, String>()
    
    var hasContactsPermission = mutableStateOf(false)
    var hasCallLogPermission = mutableStateOf(false)
    var hasNotificationPermission = mutableStateOf(false)
    var isLoadingPermissions = mutableStateOf(true)

    var isCallActive = mutableStateOf(false)
    var isCallMinimized = mutableStateOf(false)
    var callingContactName = mutableStateOf("")
    var callingContactNumber = mutableStateOf("")
    var isDefaultDialer = mutableStateOf(false)
    
    // Settings Flow observation
    val blockedNumbersFlow: StateFlow<List<BlockedNumber>> = repository.getBlockedNumbers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val speedDialFlow: StateFlow<List<SpeedDial>> = repository.getSpeedDial()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val quickResponsesFlow: StateFlow<List<QuickResponse>> = repository.getQuickResponses()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recordingsFlow: StateFlow<List<CallRecording>> = repository.getAllCallRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesFlow: StateFlow<List<CallNote>> = repository.getAllCallNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spamFlow: StateFlow<List<SpamNumber>> = repository.getAllSpamNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remindersFlow: StateFlow<List<CallReminder>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            preferredSim.value = repository.getPreferredSim()
            voicemailNumber.value = repository.getVoicemailNumber()
            
            // Add default quick responses if empty
            val currentResponses = repository.getQuickResponses().first()
            if (currentResponses.isEmpty()) {
                listOf(
                    "Can't talk right now. I'll call you later.",
                    "I'm in a meeting. What's up?",
                    "I'm driving. I'll get back to you shortly.",
                    "Sorry, I'm busy. Can I call you back?"
                ).forEach { repository.addQuickResponse(it) }
            }
        }
    }

    fun updatePreferredSim(sim: String) {
        preferredSim.value = sim
        viewModelScope.launch { repository.savePreferredSim(sim) }
    }

    fun updateDarkTheme(dark: Boolean) {
        isDarkTheme.value = dark
        prefs.edit().putBoolean("is_dark_theme", dark).apply()
    }

    fun updateAmoledMode(amoled: Boolean) {
        isAmoledMode.value = amoled
        prefs.edit().putBoolean("is_amoled_mode", amoled).apply()
    }

    fun updateCustomColorHex(hex: String) {
        customColorHex.value = hex
        prefs.edit().putString("custom_color_hex", hex).apply()
    }

    fun updateM3Expressive(expressive: Boolean) {
        isM3Expressive.value = expressive
        prefs.edit().putBoolean("is_m3_expressive", expressive).apply()
    }

    fun updateAvatarShapeType(shapeType: String) {
        avatarShapeType.value = shapeType
        prefs.edit().putString("avatar_shape_type", shapeType).apply()
    }

    fun updateUseDynamicColor(dynamic: Boolean) {
        useDynamicColor.value = dynamic
        prefs.edit().putBoolean("use_dynamic_color", dynamic).apply()
    }

    fun updateThemeColor(color: String) {
        themeColor.value = color
        prefs.edit().putString("theme_color", color).apply()
    }

    fun updateDefaultTab(tab: Int) {
        defaultTab.intValue = tab
        prefs.edit().putInt("default_tab", tab).apply()
    }

    fun updateCallWaitingEnabled(enabled: Boolean) {
        callWaitingEnabled.value = enabled
        prefs.edit().putBoolean("call_waiting_enabled", enabled).apply()
    }

    fun updateRecordingEnabled(enabled: Boolean) {
        recordingEnabled.value = enabled
        prefs.edit().putBoolean("recording_enabled", enabled).apply()
    }

    fun updateBiometricLockEnabled(enabled: Boolean) {
        isBiometricLockEnabled.value = enabled
        prefs.edit().putBoolean("is_biometric_lock_enabled", enabled).apply()
    }

    fun updatePocketProtectionEnabled(enabled: Boolean) {
        isPocketProtectionEnabled.value = enabled
        prefs.edit().putBoolean("is_pocket_protection_enabled", enabled).apply()
    }

    fun updateDashboardMode(mode: String) {
        dashboardMode.value = mode
        prefs.edit().putString("dashboard_mode", mode).apply()
    }

    fun updateCallLogDashboardEnabled(enabled: Boolean) {
        isCallLogDashboardEnabled.value = enabled
        prefs.edit().putBoolean("is_call_log_dashboard_enabled", enabled).apply()
    }

    fun updateCallLogFiltersEnabled(enabled: Boolean) {
        isCallLogFiltersEnabled.value = enabled
        prefs.edit().putBoolean("is_call_log_filters_enabled", enabled).apply()
    }

    fun updateTabSlotLeft(screen: String) {
        val currentLeft = tabSlotLeft.value
        val currentMiddle = tabSlotMiddle.value
        val currentRight = tabSlotRight.value

        var newMiddle = currentMiddle
        var newRight = currentRight

        if (screen == currentMiddle) {
            newMiddle = currentLeft
        } else if (screen == currentRight) {
            newRight = currentLeft
        }

        tabSlotLeft.value = screen
        tabSlotMiddle.value = newMiddle
        tabSlotRight.value = newRight

        prefs.edit()
            .putString("tab_slot_left", screen)
            .putString("tab_slot_middle", newMiddle)
            .putString("tab_slot_right", newRight)
            .apply()
    }

    fun updateTabSlotMiddle(screen: String) {
        val currentLeft = tabSlotLeft.value
        val currentMiddle = tabSlotMiddle.value
        val currentRight = tabSlotRight.value

        var newLeft = currentLeft
        var newRight = currentRight

        if (screen == currentLeft) {
            newLeft = currentMiddle
        } else if (screen == currentRight) {
            newRight = currentMiddle
        }

        tabSlotMiddle.value = screen
        tabSlotLeft.value = newLeft
        tabSlotRight.value = newRight

        prefs.edit()
            .putString("tab_slot_left", newLeft)
            .putString("tab_slot_middle", screen)
            .putString("tab_slot_right", newRight)
            .apply()
    }

    fun updateTabSlotRight(screen: String) {
        val currentLeft = tabSlotLeft.value
        val currentMiddle = tabSlotMiddle.value
        val currentRight = tabSlotRight.value

        var newLeft = currentLeft
        var newMiddle = currentMiddle

        if (screen == currentLeft) {
            newLeft = currentRight
        } else if (screen == currentMiddle) {
            newMiddle = currentRight
        }

        tabSlotRight.value = screen
        tabSlotLeft.value = newLeft
        tabSlotMiddle.value = newMiddle

        prefs.edit()
            .putString("tab_slot_left", newLeft)
            .putString("tab_slot_middle", newMiddle)
            .putString("tab_slot_right", screen)
            .apply()
    }

    fun updateRowSwipeEnabled(enabled: Boolean) {
        isRowSwipeEnabled.value = enabled
        prefs.edit().putBoolean("is_row_swipe_enabled", enabled).apply()
    }

    fun saveLastOutgoingNumber(number: String) {
        if (number.isNotBlank()) {
            lastDialedNumber.value = number
            prefs.edit().putString("last_dialed_number", number).apply()
        }
    }

    fun getLastOutgoingNumber(): String {
        if (lastDialedNumber.value.isNotBlank()) {
            return lastDialedNumber.value
        }
        val lastOutgoing = allCallHistoryFlow.value.firstOrNull { it.type == com.example.model.CallType.OUTGOING }
        return lastOutgoing?.number ?: ""
    }

    fun updateVoicemailNumber(num: String) {
        voicemailNumber.value = num
        viewModelScope.launch { repository.saveVoicemailNumber(num) }
    }

    fun addBlockedNumber(num: String) {
        viewModelScope.launch { repository.addBlockedNumber(num) }
    }

    fun removeBlockedNumber(num: String) {
        viewModelScope.launch { repository.removeBlockedNumber(num) }
    }

    // Encrypted Backup & Restore
    fun exportBackup(password: String = "", onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = repository.exportBackup(password)
            onResult(data)
        }
    }

    fun importBackup(rawData: String, password: String = "", onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importBackup(rawData, password)
            if (success) {
                syncData()
            }
            onResult(success)
        }
    }

    fun exportContactsVcf(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = com.example.data.BackupRestoreManager.exportContactsToVcf(getApplication())
            onResult(data)
        }
    }

    fun importContactsVcf(rawData: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = com.example.data.BackupRestoreManager.importContactsFromVcf(getApplication(), rawData)
            if (success) {
                syncData()
            }
            onResult(success)
        }
    }

    fun saveSpeedDial(key: Int, num: String, name: String) {
        viewModelScope.launch { repository.saveSpeedDial(key, num, name) }
    }

    fun deleteSpeedDial(key: Int) {
        viewModelScope.launch { repository.deleteSpeedDial(key) }
    }

    fun addQuickResponse(msg: String) {
        viewModelScope.launch { repository.addQuickResponse(msg) }
    }

    fun deleteQuickResponse(resp: QuickResponse) {
        viewModelScope.launch { repository.deleteQuickResponse(resp) }
    }
    
    // Helper Dialog State
    var isAddContactDialogVisible = mutableStateOf(false)
    var isEditContactDialogVisible = mutableStateOf(false)
    var newContactName = mutableStateOf("")
    var newContactNumber = mutableStateOf("")
    var newContactLabel = mutableStateOf("Mobile")
    var newContactEmail = mutableStateOf("")
    var oldContactToEdit = mutableStateOf<Contact?>(null)
    var editContactName = mutableStateOf("")
    var editContactNumber = mutableStateOf("")
    var editContactLabel = mutableStateOf("Mobile")
    var editContactEmail = mutableStateOf("")

    private var isObserving = false

    fun startDataSyncAndObservation() {
        if (isObserving) return
        isObserving = true
        
        // Initial sync
        syncData()
        refreshAvailableAccounts()
        
        // Real-time sync observation
        repository.startObservingChanges {
            syncData()
            refreshAvailableAccounts()
        }
    }

    fun refreshAvailableAccounts() {
        viewModelScope.launch {
            try {
                val accounts = repository.fetchAvailableAccounts()
                availableAccounts.clear()
                availableAccounts.addAll(accounts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncData(force: Boolean = true) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                launch { repository.syncContacts(force) }
                launch { repository.syncCallLogs() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addContact(
        name: String, 
        number: String, 
        label: String, 
        email: String = "",
        accountName: String = "",
        accountType: String = ""
    ) {
        viewModelScope.launch {
            repository.addContact(name, number, label, email, accountName, accountType)
            refreshAvailableAccounts()
        }
    }

    fun saveCallNote(number: String, note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            repository.saveCallNote(CallNote(number = number, note = note, lastUpdated = System.currentTimeMillis()))
        }
    }

    suspend fun getCallNote(number: String): CallNote? {
        return repository.getCallNote(number)
    }

    fun deleteCallNote(number: String) {
        viewModelScope.launch {
            repository.deleteCallNotesForNumber(number)
        }
    }

    fun deleteCallNoteById(id: Long) {
        viewModelScope.launch {
            repository.deleteCallNoteById(id)
        }
    }

    fun getCallNotesForNumberFlow(number: String): Flow<List<CallNote>> {
        return repository.getCallNotesForNumberFlow(number)
    }

    fun toggleCallRecording(context: android.content.Context, phoneNumber: String, callerName: String = "Unknown") {
        if (com.example.util.CallAudioRecorder.isRecording.value) {
            val result = com.example.util.CallAudioRecorder.stopRecording()
            val file = result.file
            if (file != null && file.exists() && file.length() > 0L) {
                val durationSec = result.durationSeconds.coerceAtLeast(1L)
                val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", com.example.ui.components.getCurrentLocale(context))
                val timestamp = sdf.format(java.util.Date())
                val recording = CallRecording(
                    number = phoneNumber,
                    name = if (callerName.isBlank()) phoneNumber else callerName,
                    timestamp = timestamp,
                    duration = durationSec,
                    filePath = file.absolutePath
                )
                saveCallRecording(recording)
            }
        } else {
            com.example.util.CallAudioRecorder.startRecording(context, phoneNumber)
        }
    }

    fun saveCallRecording(recording: CallRecording) {
        viewModelScope.launch {
            repository.saveCallRecording(recording)
        }
    }

    fun deleteCallRecording(id: Int) {
        viewModelScope.launch {
            repository.deleteCallRecording(id)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            refreshAvailableAccounts()
        }
    }

    fun deleteContact(number: String) {
        viewModelScope.launch {
            repository.deleteContact(number)
            refreshAvailableAccounts()
        }
    }

    fun deleteCallLog(id: Int) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
        }
    }

    fun toggleFavorite(number: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(number, isFavorite)
        }
    }

    suspend fun getCallHistoryByNumber(number: String): List<CallRecord> {
        return repository.getCallHistoryByNumber(number)
    }

    // New Custom Features Support
    fun updateFlashAlertsEnabled(enabled: Boolean) {
        flashAlertsEnabled.value = enabled
        prefs.edit().putBoolean("flash_alerts_enabled", enabled).apply()
    }

    fun addSpamNumber(number: String, label: String = "Spam") {
        viewModelScope.launch {
            repository.addSpamNumber(number, label)
        }
    }

    fun deleteSpamNumber(spam: SpamNumber) {
        viewModelScope.launch {
            repository.deleteSpamNumber(spam)
        }
    }

    fun clearAllSpam() {
        viewModelScope.launch {
            repository.clearAllSpam()
        }
    }

    fun importSpamNumbersFromCsv(csvContent: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importSpamNumbersFromCsv(csvContent)
            onResult(count)
        }
    }

    fun addReminder(number: String, name: String, triggerTime: Long, note: String = "", onResult: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val reminder = CallReminder(
                number = number,
                name = name,
                reminderTime = triggerTime,
                isCompleted = false,
                note = note
            )
            val id = repository.saveReminder(reminder)
            // Schedule via system alarm
            com.example.util.ReminderScheduler.schedule(getApplication(), id.toInt(), triggerTime)
            onResult(id)
        }
    }

    fun updateReminder(reminder: CallReminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }

    fun deleteReminder(reminder: CallReminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            com.example.util.ReminderScheduler.cancel(getApplication(), reminder.id)
        }
    }

    fun deleteReminderById(id: Int) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
            com.example.util.ReminderScheduler.cancel(getApplication(), id)
        }
    }

    fun logFakeCall(name: String, number: String, type: com.example.model.CallType, durationSeconds: Long, simSlot: Int = 1) {
        viewModelScope.launch {
            repository.insertManualCallRecord(name, number, type, durationSeconds, simSlot)
            syncData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopObservingChanges()
    }
}
