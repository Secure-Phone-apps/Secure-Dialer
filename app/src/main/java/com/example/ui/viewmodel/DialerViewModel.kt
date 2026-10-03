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
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import androidx.paging.*
import com.example.*
import com.example.model.*
import com.example.util.CallAudioRecorder
import com.example.util.RecordingCompressionProfile
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

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
    var dialpadTextFieldValue = mutableStateOf(TextFieldValue(""))

    // Details Screen state
    var isCallHistoryDetailsOpen = mutableStateOf(false)

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
        _searchQueryFlow.value = newQuery
    }

    fun onAccountFilterChange(accountName: String) {
        selectedAccountFilter.value = accountName
        _selectedAccountFilterFlow.value = accountName
        prefs.edit().putString("selected_account_filter", accountName).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("selected_account_filter", accountName))
            } catch (_: Exception) {}
        }
    }

    fun updateDefaultContactAccount(accountName: String, accountType: String) {
        defaultContactAccountName.value = accountName
        defaultContactAccountType.value = accountType
        prefs.edit()
            .putString("default_contact_account_name", accountName)
            .putString("default_contact_account_type", accountType)
            .apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("default_contact_account_name", accountName))
                repository.dao.insertSetting(AppSetting("default_contact_account_type", accountType))
            } catch (_: Exception) {}
        }
    }

    fun onDialpadTextFieldValueChange(newValue: TextFieldValue) {
        val text = newValue.text
        val safeStart = newValue.selection.min.coerceIn(0, text.length)
        val safeEnd = newValue.selection.max.coerceIn(0, text.length)
        val sanitizedValue = if (safeStart != newValue.selection.start || safeEnd != newValue.selection.end) {
            newValue.copy(selection = TextRange(safeStart, safeEnd))
        } else {
            newValue
        }
        dialpadTextFieldValue.value = sanitizedValue
        dialpadInput.value = text
        _dialpadInputFlow.value = text
    }

    fun onDialpadInputChange(newInput: String) {
        val currentTfv = dialpadTextFieldValue.value
        if (currentTfv.text != newInput) {
            val newSelection = TextRange(newInput.length)
            dialpadTextFieldValue.value = TextFieldValue(text = newInput, selection = newSelection)
        }
        dialpadInput.value = newInput
        _dialpadInputFlow.value = newInput
    }

    fun insertDialpadDigit(digit: String) {
        val current = dialpadTextFieldValue.value
        val text = current.text
        val start = current.selection.min.coerceIn(0, text.length)
        val end = current.selection.max.coerceIn(0, text.length)
        val newText = text.replaceRange(start, end, digit)
        val newCursorPos = (start + digit.length).coerceIn(0, newText.length)
        onDialpadTextFieldValueChange(TextFieldValue(text = newText, selection = TextRange(newCursorPos)))
    }

    fun backspaceDialpad() {
        val current = dialpadTextFieldValue.value
        val text = current.text
        if (text.isEmpty()) return

        val start = current.selection.min.coerceIn(0, text.length)
        val end = current.selection.max.coerceIn(0, text.length)

        if (start != end) {
            val newText = text.removeRange(start, end)
            onDialpadTextFieldValueChange(TextFieldValue(text = newText, selection = TextRange(start.coerceIn(0, newText.length))))
        } else if (start > 0) {
            val newText = text.removeRange(start - 1, start)
            onDialpadTextFieldValueChange(TextFieldValue(text = newText, selection = TextRange((start - 1).coerceIn(0, newText.length))))
        }
    }

    fun clearDialpad() {
        onDialpadTextFieldValueChange(TextFieldValue(""))
    }

    // PERF FIX: Replaces 100,000 nested iterations with instantaneous O(1) in-memory cache lookup
    fun isNumberUnsaved(number: String): Boolean {
        if (number.isBlank()) return false
        return ContactCache.getContact(number) == null
    }

    fun openAddContactWithNumber(number: String) {
        newContactName.value = ""
        newContactNumber.value = number
        newContactLabel.value = "Mobile"
        newContactEmail.value = ""
        isAddContactDialogVisible.value = true
    }

    fun openAddToExistingContactWithNumber(number: String, contact: Contact) {
        val currentNumbers = contact.getAllNumbers().toMutableList()
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        if (!currentNumbers.any { it.number.filter { c -> c.isDigit() || c == '+' } == cleanNumber }) {
            currentNumbers.add(LabeledNumber(number = number, label = "Mobile"))
        }
        val updatedContact = contact.copy(numbers = currentNumbers)
        oldContactToEdit.value = updatedContact
        editContactName.value = updatedContact.name
        editContactNumber.value = updatedContact.number
        editContactLabel.value = updatedContact.label
        editContactEmail.value = updatedContact.email
        isEditContactDialogVisible.value = true
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val contactsPaged: Flow<PagingData<Contact>> = combine(_searchQueryFlow.debounce(100), _selectedAccountFilterFlow) { query, account ->
        Pair(query, account)
    }.flatMapLatest { (query, account) ->
        repository.getContactsPaged(query, account)
    }.cachedIn(viewModelScope)

    val callHistoryPaged: Flow<PagingData<CallRecord>> = repository.getCallHistoryPaged().cachedIn(viewModelScope)

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
                contact.number.isNotBlank() && (
                    contact.name.contains(query, ignoreCase = true) ||
                    contact.number.contains(query, ignoreCase = true) ||
                    contact.t9Mapping.contains(query, ignoreCase = true)
                )
            }.distinctBy { it.number }
            .map { contact ->
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
                    record.number.isNotBlank() &&
                    record.number !in contactNumbers &&
                    (record.name.contains(query, ignoreCase = true) || record.number.contains(query, ignoreCase = true))
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
    }.flowOn(Dispatchers.Default)
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
    var autoTuneRecordingVolume = mutableStateOf(prefs.getBoolean("auto_tune_recording_volume", true))
    var recordingChimeEnabled = mutableStateOf(prefs.getBoolean("recording_chime_enabled", false))
    var isAutoRecordCallsEnabled = mutableStateOf(prefs.getBoolean("is_auto_record_calls_enabled", false))
    var isDynamicIslandEnabled = mutableStateOf(prefs.getBoolean("is_dynamic_island_enabled", true))
    var isDynamicIslandSpeakerOnly = mutableStateOf(prefs.getBoolean("is_dynamic_island_speaker_only", false))
    var isBiometricLockEnabled = mutableStateOf(prefs.getBoolean("is_biometric_lock_enabled", false))
    var isRecordingsBiometricLockEnabled = mutableStateOf(prefs.getBoolean("is_recordings_biometric_lock_enabled", false))
    var isAutoExportRecordingsEnabled = mutableStateOf(prefs.getBoolean("is_auto_export_recordings_enabled", true))
    var recordingCompressionProfile = mutableStateOf(
        RecordingCompressionProfile.fromKey(
            prefs.getString("recording_compression_profile", RecordingCompressionProfile.BALANCED.key)
        )
    )
    var isPocketProtectionEnabled = mutableStateOf(prefs.getBoolean("is_pocket_protection_enabled", false))
    var defaultStartupTabKey = mutableStateOf(prefs.getString("default_startup_tab_key", "RECENTS") ?: "RECENTS")
    var selectedTab = mutableIntStateOf(
        listOf(
            prefs.getString("tab_slot_left", "RECENTS") ?: "RECENTS",
            prefs.getString("tab_slot_middle", "CONTACTS") ?: "CONTACTS",
            prefs.getString("tab_slot_right", "DIALPAD") ?: "DIALPAD"
        ).indexOf(prefs.getString("default_startup_tab_key", "RECENTS") ?: "RECENTS").coerceAtLeast(0)
    )
    var flashAlertsEnabled = mutableStateOf(prefs.getBoolean("flash_alerts_enabled", false))
    var isCallbackRemindersEnabled = mutableStateOf(prefs.getBoolean("is_callback_reminders_enabled", true))
    var isCallNotesEnabled = mutableStateOf(prefs.getBoolean("is_call_notes_enabled", true))
    var isFakeCallSimulatorEnabled = mutableStateOf(prefs.getBoolean("is_fake_call_simulator_enabled", true))
    var pendingPostCallRecordingNote = mutableStateOf<CallRecording?>(null)

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
    var dialpadTonesEnabled = mutableStateOf(prefs.getBoolean("dialpad_tones_enabled", true))
    var vibrateOnClickEnabled = mutableStateOf(prefs.getBoolean("vibrate_on_click_enabled", true))
    var flipToSilenceEnabled = mutableStateOf(prefs.getBoolean("flip_to_silence_enabled", false))
    var isHideCallerIdEnabled = mutableStateOf(prefs.getBoolean("is_hide_caller_id_enabled", false))
    var clirPrefix = mutableStateOf(prefs.getString("clir_prefix", "#31#") ?: "#31#")
    var preferredSim = mutableStateOf("SIM 1")
    var voicemailNumber = mutableStateOf("+1 (555) 011-9988")

    val blockedNumbers = mutableStateListOf<String>()
    val quickResponses = mutableStateListOf<String>()
    val speedDialMap = mutableStateMapOf<Int, String>()

    var hasContactsPermission = mutableStateOf(false)
    var hasCallLogPermission = mutableStateOf(false)
    var hasNotificationPermission = mutableStateOf(false)
    var hasRecordAudioPermission = mutableStateOf(false)
    var isLoadingPermissions = mutableStateOf(true)

    var isCallActive = mutableStateOf(false)
    var isCallMinimized = mutableStateOf(false)
    var isLaunchedForCall = mutableStateOf(false)
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

            try {
                val settings = repository.dao.getAllSettingsList().associate { it.key to it.value }
                // PERF FIX: Batch 25 individual disk commits into a single non-blocking apply transaction
                val editor = prefs.edit()

                settings["dialpad_tones_enabled"]?.toBooleanStrictOrNull()?.let {
                    dialpadTonesEnabled.value = it
                    editor.putBoolean("dialpad_tones_enabled", it)
                }
                settings["vibrate_on_click_enabled"]?.toBooleanStrictOrNull()?.let {
                    vibrateOnClickEnabled.value = it
                    editor.putBoolean("vibrate_on_click_enabled", it)
                }
                settings["flip_to_silence_enabled"]?.toBooleanStrictOrNull()?.let {
                    flipToSilenceEnabled.value = it
                    editor.putBoolean("flip_to_silence_enabled", it)
                }
                settings["call_waiting_enabled"]?.toBooleanStrictOrNull()?.let {
                    callWaitingEnabled.value = it
                    editor.putBoolean("call_waiting_enabled", it)
                }
                settings["recording_enabled"]?.toBooleanStrictOrNull()?.let {
                    recordingEnabled.value = it
                    editor.putBoolean("recording_enabled", it)
                }
                settings["auto_tune_recording_volume"]?.toBooleanStrictOrNull()?.let {
                    autoTuneRecordingVolume.value = it
                    editor.putBoolean("auto_tune_recording_volume", it)
                }
                settings["recording_chime_enabled"]?.toBooleanStrictOrNull()?.let {
                    recordingChimeEnabled.value = it
                    editor.putBoolean("recording_chime_enabled", it)
                }
                settings["is_auto_record_calls_enabled"]?.toBooleanStrictOrNull()?.let {
                    isAutoRecordCallsEnabled.value = it
                    editor.putBoolean("is_auto_record_calls_enabled", it)
                }
                settings["is_dynamic_island_enabled"]?.toBooleanStrictOrNull()?.let {
                    isDynamicIslandEnabled.value = it
                    editor.putBoolean("is_dynamic_island_enabled", it)
                }
                settings["is_dynamic_island_speaker_only"]?.toBooleanStrictOrNull()?.let {
                    isDynamicIslandSpeakerOnly.value = it
                    editor.putBoolean("is_dynamic_island_speaker_only", it)
                }
                settings["is_biometric_lock_enabled"]?.toBooleanStrictOrNull()?.let {
                    isBiometricLockEnabled.value = it
                    editor.putBoolean("is_biometric_lock_enabled", it)
                }
                settings["is_recordings_biometric_lock_enabled"]?.toBooleanStrictOrNull()?.let {
                    isRecordingsBiometricLockEnabled.value = it
                    editor.putBoolean("is_recordings_biometric_lock_enabled", it)
                }
                settings["is_auto_export_recordings_enabled"]?.toBooleanStrictOrNull()?.let {
                    isAutoExportRecordingsEnabled.value = it
                    editor.putBoolean("is_auto_export_recordings_enabled", it)
                }
                settings["is_pocket_protection_enabled"]?.toBooleanStrictOrNull()?.let {
                    isPocketProtectionEnabled.value = it
                    editor.putBoolean("is_pocket_protection_enabled", it)
                }
                settings["is_callback_reminders_enabled"]?.toBooleanStrictOrNull()?.let {
                    isCallbackRemindersEnabled.value = it
                    editor.putBoolean("is_callback_reminders_enabled", it)
                }
                settings["is_call_notes_enabled"]?.toBooleanStrictOrNull()?.let {
                    isCallNotesEnabled.value = it
                    editor.putBoolean("is_call_notes_enabled", it)
                }
                settings["is_fake_call_simulator_enabled"]?.toBooleanStrictOrNull()?.let {
                    isFakeCallSimulatorEnabled.value = it
                    editor.putBoolean("is_fake_call_simulator_enabled", it)
                }
                settings["flash_alerts_enabled"]?.toBooleanStrictOrNull()?.let {
                    flashAlertsEnabled.value = it
                    editor.putBoolean("flash_alerts_enabled", it)
                }
                settings["is_hide_caller_id_enabled"]?.toBooleanStrictOrNull()?.let {
                    isHideCallerIdEnabled.value = it
                    editor.putBoolean("is_hide_caller_id_enabled", it)
                }
                settings["clir_prefix"]?.let {
                    clirPrefix.value = it
                    editor.putString("clir_prefix", it)
                }
                settings["is_call_log_dashboard_enabled"]?.toBooleanStrictOrNull()?.let {
                    isCallLogDashboardEnabled.value = it
                    editor.putBoolean("is_call_log_dashboard_enabled", it)
                }
                settings["is_call_log_filters_enabled"]?.toBooleanStrictOrNull()?.let {
                    isCallLogFiltersEnabled.value = it
                    editor.putBoolean("is_call_log_filters_enabled", it)
                }
                settings["is_row_swipe_enabled"]?.toBooleanStrictOrNull()?.let {
                    isRowSwipeEnabled.value = it
                    editor.putBoolean("is_row_swipe_enabled", it)
                }
                settings["selected_account_filter"]?.let {
                    selectedAccountFilter.value = it
                    _selectedAccountFilterFlow.value = it
                    editor.putString("selected_account_filter", it)
                }
                settings["default_contact_account_name"]?.let {
                    defaultContactAccountName.value = it
                    editor.putString("default_contact_account_name", it)
                }

                editor.apply()
            } catch (_: Exception) {}

            val currentResponses = repository.getQuickResponses().first()
            if (currentResponses.isEmpty()) {
                listOf(
                    "Can't talk right now. I'll call you later.",
                    "I'm in a meeting. What's up?",
                    "I'm driving. I'll get back to you shortly.",
                    "Sorry, I'm busy. Can I call you back?"
                ).forEach { repository.addQuickResponse(it) }
            }

            try {
                syncRecordingsFromDisk(repository.context)
            } catch (_: Exception) {}
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
        val editor = prefs.edit().putBoolean("is_amoled_mode", amoled)
        if (amoled && !isDarkTheme.value) {
            isDarkTheme.value = true
            editor.putBoolean("is_dark_theme", true)
        }
        editor.apply()
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
        val keys = listOf("RECENTS", "CONTACTS", "DIALPAD")
        val key = keys.getOrElse(tab) { "RECENTS" }
        updateDefaultStartupTabKey(key)
    }

    fun updateDefaultStartupTabKey(key: String) {
        defaultStartupTabKey.value = key
        prefs.edit().putString("default_startup_tab_key", key).apply()
        val slots = listOf(tabSlotLeft.value, tabSlotMiddle.value, tabSlotRight.value)
        selectedTab.intValue = slots.indexOf(key).coerceAtLeast(0)
    }

    fun selectTabBySlotKey(key: String) {
        val slots = listOf(tabSlotLeft.value, tabSlotMiddle.value, tabSlotRight.value)
        val index = slots.indexOf(key)
        if (index != -1) {
            selectedTab.intValue = index
        }
    }

    fun updateDialpadTonesEnabled(enabled: Boolean) {
        dialpadTonesEnabled.value = enabled
        prefs.edit().putBoolean("dialpad_tones_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("dialpad_tones_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateVibrateOnClickEnabled(enabled: Boolean) {
        vibrateOnClickEnabled.value = enabled
        prefs.edit().putBoolean("vibrate_on_click_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("vibrate_on_click_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateFlipToSilenceEnabled(enabled: Boolean) {
        flipToSilenceEnabled.value = enabled
        prefs.edit().putBoolean("flip_to_silence_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("flip_to_silence_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateCallWaitingEnabled(enabled: Boolean) {
        callWaitingEnabled.value = enabled
        prefs.edit().putBoolean("call_waiting_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("call_waiting_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateHideCallerIdEnabled(enabled: Boolean) {
        isHideCallerIdEnabled.value = enabled
        prefs.edit().putBoolean("is_hide_caller_id_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_hide_caller_id_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateClirPrefix(prefix: String) {
        val sanitized = prefix.trim().ifBlank { "#31#" }
        clirPrefix.value = sanitized
        prefs.edit().putString("clir_prefix", sanitized).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("clir_prefix", sanitized))
            } catch (_: Exception) {}
        }
    }

    fun updateRecordingEnabled(enabled: Boolean) {
        recordingEnabled.value = enabled
        prefs.edit().putBoolean("recording_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("recording_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateAutoTuneRecordingVolume(enabled: Boolean) {
        autoTuneRecordingVolume.value = enabled
        prefs.edit().putBoolean("auto_tune_recording_volume", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("auto_tune_recording_volume", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateRecordingChimeEnabled(enabled: Boolean) {
        recordingChimeEnabled.value = enabled
        prefs.edit().putBoolean("recording_chime_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("recording_chime_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateBiometricLockEnabled(enabled: Boolean) {
        isBiometricLockEnabled.value = enabled
        prefs.edit().putBoolean("is_biometric_lock_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_biometric_lock_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateRecordingsBiometricLockEnabled(enabled: Boolean) {
        isRecordingsBiometricLockEnabled.value = enabled
        prefs.edit().putBoolean("is_recordings_biometric_lock_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_recordings_biometric_lock_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateAutoRecordCallsEnabled(enabled: Boolean) {
        isAutoRecordCallsEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_record_calls_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_auto_record_calls_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateDynamicIslandEnabled(enabled: Boolean) {
        isDynamicIslandEnabled.value = enabled
        prefs.edit().putBoolean("is_dynamic_island_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_dynamic_island_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateDynamicIslandSpeakerOnly(speakerOnly: Boolean) {
        isDynamicIslandSpeakerOnly.value = speakerOnly
        prefs.edit().putBoolean("is_dynamic_island_speaker_only", speakerOnly).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_dynamic_island_speaker_only", speakerOnly.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateAutoExportRecordingsEnabled(enabled: Boolean) {
        isAutoExportRecordingsEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_export_recordings_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_auto_export_recordings_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateRecordingCompressionProfile(profile: RecordingCompressionProfile) {
        recordingCompressionProfile.value = profile
        CallAudioRecorder.setCompressionProfile(repository.context, profile)
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("recording_compression_profile", profile.key))
            } catch (_: Exception) {}
        }
    }

    fun updatePocketProtectionEnabled(enabled: Boolean) {
        isPocketProtectionEnabled.value = enabled
        prefs.edit().putBoolean("is_pocket_protection_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_pocket_protection_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateCallbackRemindersEnabled(enabled: Boolean) {
        isCallbackRemindersEnabled.value = enabled
        prefs.edit().putBoolean("is_callback_reminders_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_callback_reminders_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateCallNotesEnabled(enabled: Boolean) {
        isCallNotesEnabled.value = enabled
        prefs.edit().putBoolean("is_call_notes_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_call_notes_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateFakeCallSimulatorEnabled(enabled: Boolean) {
        isFakeCallSimulatorEnabled.value = enabled
        prefs.edit().putBoolean("is_fake_call_simulator_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_fake_call_simulator_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateDashboardMode(mode: String) {
        dashboardMode.value = mode
        prefs.edit().putString("dashboard_mode", mode).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("dashboard_mode", mode))
            } catch (_: Exception) {}
        }
    }

    fun updateCallLogDashboardEnabled(enabled: Boolean) {
        isCallLogDashboardEnabled.value = enabled
        prefs.edit().putBoolean("is_call_log_dashboard_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_call_log_dashboard_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateCallLogFiltersEnabled(enabled: Boolean) {
        isCallLogFiltersEnabled.value = enabled
        prefs.edit().putBoolean("is_call_log_filters_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("is_call_log_filters_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
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

        val slots = listOf(screen, newMiddle, newRight)
        selectedTab.intValue = slots.indexOf(defaultStartupTabKey.value).coerceAtLeast(0)
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

        val slots = listOf(newLeft, screen, newRight)
        selectedTab.intValue = slots.indexOf(defaultStartupTabKey.value).coerceAtLeast(0)
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

        val slots = listOf(newLeft, newMiddle, screen)
        selectedTab.intValue = slots.indexOf(defaultStartupTabKey.value).coerceAtLeast(0)
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
        val lastOutgoing = allCallHistoryFlow.value.firstOrNull { it.type == CallType.OUTGOING }
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

    fun writeTextToUri(uri: Uri, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = com.example.data.BackupRestoreManager.writeTextToUri(getApplication(), uri, content)
            onResult(success)
        }
    }

    fun readTextFromUri(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val content = com.example.data.BackupRestoreManager.readTextFromUri(getApplication(), uri)
            onResult(content)
        }
    }

    fun exportBlockedNumbers(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = com.example.data.BackupRestoreManager.exportBlockedNumbers(getApplication())
            onResult(data)
        }
    }

    fun importBlockedNumbers(rawData: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = com.example.data.BackupRestoreManager.importBlockedNumbers(getApplication(), rawData)
            if (count > 0) syncData()
            onResult(count)
        }
    }

    fun exportBackup(password: String = "", onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = repository.exportBackup(password)
            onResult(data)
        }
    }

    fun importBackup(rawData: String, password: String = "", onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importBackup(rawData, password)
            if (success) syncData()
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
            if (success) syncData()
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
    var isAddToExistingSheetVisible = mutableStateOf(false)
    var addToExistingPendingNumber = mutableStateOf("")
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

        syncData()
        refreshAvailableAccounts()

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
            } catch (_: Exception) {}
        }
    }

    fun syncData(force: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Serialized sync execution to eliminate SQLite database lock contention
                repository.syncContacts(force)
                repository.syncCallLogs()
            } catch (_: Exception) {}
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

    fun addContactWithDetails(
        name: String,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail> = emptyList(),
        addresses: List<LabeledAddress> = emptyList(),
        accountName: String = "",
        accountType: String = ""
    ) {
        viewModelScope.launch {
            repository.addContactWithDetails(name, numbers, emails, addresses, accountName, accountType)
            refreshAvailableAccounts()
        }
    }

    fun saveCallNote(number: String, note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            repository.saveCallNote(CallNote(number = number, note = note, lastUpdated = System.currentTimeMillis()))
        }
    }

    suspend fun getCallNote(number: String): CallNote? = repository.getCallNote(number)

    fun deleteCallNote(number: String) {
        viewModelScope.launch { repository.deleteCallNotesForNumber(number) }
    }

    fun deleteCallNoteById(id: Long) {
        viewModelScope.launch { repository.deleteCallNoteById(id) }
    }

    fun getCallNotesForNumberFlow(number: String): Flow<List<CallNote>> = repository.getCallNotesForNumberFlow(number)

    fun toggleCallRecording(context: Context, phoneNumber: String, callerName: String = "Unknown") {
        if (CallAudioRecorder.isRecording.value) {
            val result = CallAudioRecorder.stopRecording()
            val file = result.file
            if (file != null && file.exists() && file.length() > 0L) {
                val durationSec = result.durationSeconds.coerceAtLeast(1L)
                val sdf = SimpleDateFormat("MMM d, HH:mm", com.example.ui.components.getCurrentLocale(context))
                val timestamp = sdf.format(Date())
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
            CallAudioRecorder.startRecording(context, phoneNumber)
        }
    }

    fun saveCallRecording(recording: CallRecording) {
        viewModelScope.launch {
            val insertedId = repository.saveCallRecording(recording)
            if (isAutoExportRecordingsEnabled.value) {
                withContext(Dispatchers.IO) {
                    val f = File(recording.filePath)
                    if (f.exists() && f.length() > 128L) {
                        CallAudioRecorder.exportRecordingToPublicDownloads(repository.context, f)
                    }
                }
            }
            if (isCallNotesEnabled.value) {
                pendingPostCallRecordingNote.value = recording.copy(id = insertedId.toInt())
            }
        }
    }

    fun dismissPostCallRecordingNote() {
        pendingPostCallRecordingNote.value = null
    }

    fun savePostCallRecordingNote(id: Int, number: String, note: String) {
        viewModelScope.launch {
            if (id > 0) repository.updateCallRecordingNote(id, note)
            if (note.isNotBlank() && number.isNotBlank()) repository.saveCallNote(number, note)
            pendingPostCallRecordingNote.value = null
        }
    }

    fun deleteCallRecording(id: Int) {
        viewModelScope.launch { repository.deleteCallRecording(id) }
    }

    fun updateCallRecordingNote(id: Int, note: String) {
        viewModelScope.launch { repository.updateCallRecordingNote(id, note) }
    }

    fun syncRecordingsFromDisk(context: Context, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getAllCallRecordings().first()
                val existingPaths = existing.map { it.filePath }.toSet()
                val existingNames = existing.map { File(it.filePath).name }.toSet()
                val recovered = CallAudioRecorder.recoverRecordingsFromDisk(context)
                var newCount = 0
                for (rec in recovered) {
                    val f = File(rec.filePath)
                    if (f.exists() && f.length() > 128L && !existingPaths.contains(rec.filePath) && !existingNames.contains(f.name)) {
                        val contactName = getContactNameFromNumber(context, rec.number) ?: rec.name
                        repository.saveCallRecording(rec.copy(name = contactName))
                        newCount++
                    }
                }
                withContext(Dispatchers.Main) { onComplete?.invoke(newCount) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onComplete?.invoke(0) }
            }
        }
    }

    fun recoverRecordingsFromDisk(context: Context, onComplete: ((Int) -> Unit)? = null) {
        syncRecordingsFromDisk(context, onComplete)
    }

    fun exportRecordingToDownloads(context: Context, filePath: String): Boolean {
        val f = File(filePath)
        return CallAudioRecorder.exportRecordingToPublicDownloads(context, f)
    }

    fun exportAllRecordingsToDownloads(context: Context): Int = CallAudioRecorder.exportAllRecordingsToDownloads(context)

    fun cleanupCorruptOrEmptyRecordings(context: Context): Int {
        val count = CallAudioRecorder.cleanupCorruptOrEmptyFiles(context)
        syncRecordingsFromDisk(context)
        return count
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
        viewModelScope.launch { repository.deleteCallLog(id) }
    }

    fun clearAllCallLogs() {
        viewModelScope.launch { repository.clearAllCallLogs() }
    }

    fun toggleFavorite(number: String, isFavorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(number, isFavorite) }
    }

    suspend fun getCallHistoryByNumber(number: String): List<CallRecord> = repository.getCallHistoryByNumber(number)

    fun updateFlashAlertsEnabled(enabled: Boolean) {
        flashAlertsEnabled.value = enabled
        prefs.edit().putBoolean("flash_alerts_enabled", enabled).apply()
        viewModelScope.launch {
            try {
                repository.dao.insertSetting(AppSetting("flash_alerts_enabled", enabled.toString()))
            } catch (_: Exception) {}
        }
    }

    fun addSpamNumber(number: String, label: String = "Spam") {
        viewModelScope.launch { repository.addSpamNumber(number, label) }
    }

    fun deleteSpamNumber(spam: SpamNumber) {
        viewModelScope.launch { repository.deleteSpamNumber(spam) }
    }

    fun clearAllSpam() {
        viewModelScope.launch { repository.clearAllSpam() }
    }

    fun importSpamNumbersFromCsv(csvContent: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importSpamNumbersFromCsv(csvContent)
            onResult(count)
        }
    }

    fun exportSpamNumbersToCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = repository.exportSpamNumbersToCsv()
            onResult(data)
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
            ReminderScheduler.schedule(getApplication(), id.toInt(), triggerTime)
            onResult(id)
        }
    }

    fun updateReminder(reminder: CallReminder) {
        viewModelScope.launch { repository.updateReminder(reminder) }
    }

    fun deleteReminder(reminder: CallReminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            ReminderScheduler.cancel(getApplication(), reminder.id)
        }
    }

    fun deleteReminderById(id: Int) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
            ReminderScheduler.cancel(getApplication(), id)
        }
    }

    fun logCall(name: String, number: String, type: CallType, durationSeconds: Long, simSlot: Int = 1) {
        viewModelScope.launch {
            repository.insertManualCallRecord(name, number, type, durationSeconds, simSlot)
            syncData()
        }
    }

    fun logFakeCall(name: String, number: String, type: CallType, durationSeconds: Long, simSlot: Int = 1) {
        logCall(name, number, type, durationSeconds, simSlot)
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopObservingChanges()
    }
}