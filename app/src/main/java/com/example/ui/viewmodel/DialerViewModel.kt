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

    private val recordingsCoordinator = DialerRecordingsCoordinator(
        repository = repository,
        prefs = prefs,
        scope = viewModelScope,
        onRecordingCreated = { pendingPostCallRecordingNote.value = it }
    )

    private val spamAndBackupCoordinator = DialerSpamAndBackupCoordinator(
        application = application,
        repository = repository,
        scope = viewModelScope,
        onDataChanged = { syncData() }
    )

    private val settingsCoordinator = DialerSettingsCoordinator(
        repository = repository,
        prefs = prefs,
        scope = viewModelScope
    )

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

    var pendingExternalNumber = mutableStateOf<String?>(null)

    fun handleExternalNumber(number: String) {
        if (number.isNotBlank()) {
            pendingExternalNumber.value = number
            onDialpadInputChange(number)
            selectTabBySlotKey("DIALPAD")
        }
    }

    fun flushPendingExternalNumber() {
        val pending = pendingExternalNumber.value
        if (!pending.isNullOrBlank()) {
            onDialpadInputChange(pending)
            selectTabBySlotKey("DIALPAD")
            pendingExternalNumber.value = null
        }
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

    val contactNameFormat = mutableStateOf(
        try {
            ContactNameFormat.valueOf(prefs.getString("contact_name_format", ContactNameFormat.FIRST_NAME_FIRST.name) ?: ContactNameFormat.FIRST_NAME_FIRST.name)
        } catch (_: Exception) {
            ContactNameFormat.FIRST_NAME_FIRST
        }
    )
    private val _contactNameFormatFlow = MutableStateFlow(contactNameFormat.value)

    fun setContactNameFormat(format: ContactNameFormat) {
        contactNameFormat.value = format
        _contactNameFormatFlow.value = format
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putString("contact_name_format", format.name).apply()
            repository.dao.insertSetting(AppSetting("contact_name_format", format.name))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val contactsPaged: Flow<PagingData<Contact>> = combine(
        _searchQueryFlow.debounce(100), 
        _selectedAccountFilterFlow,
        _contactNameFormatFlow
    ) { query, account, format ->
        Triple(query, account, format == ContactNameFormat.LAST_NAME_FIRST)
    }.flatMapLatest { (query, account, isLastNameFirst) ->
        repository.getContactsPaged(query, account, isLastNameFirst)
    }.cachedIn(viewModelScope)

    val callHistoryPaged: Flow<PagingData<CallRecord>> = repository.getCallHistoryPaged().cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteContacts: StateFlow<List<Contact>> = _contactNameFormatFlow.flatMapLatest { format ->
        repository.getFavoriteContacts(format == ContactNameFormat.LAST_NAME_FIRST)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allContactsFlow: StateFlow<List<Contact>> = _contactNameFormatFlow.flatMapLatest { format ->
        repository.getAllContactsFlow(format == ContactNameFormat.LAST_NAME_FIRST)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCallHistoryFlow: StateFlow<List<CallRecord>> = repository.getAllCallHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val dialpadMatches: StateFlow<List<DialpadMatch>> = combine(
        allContactsFlow,
        allCallHistoryFlow,
        _dialpadInputFlow.debounce(100),
        _contactNameFormatFlow
    ) { contacts, recents, query, nameFormat ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val cleanQuery = query.trim()
            val cleanQueryDigits = cleanQuery.filter { it.isDigit() || it == '+' }
            val normalizedQuery = java.text.Normalizer.normalize(cleanQuery, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .lowercase(java.util.Locale.ROOT)

            val matchedContacts = contacts.asSequence().filter { contact ->
                if (contact.number.isBlank()) return@filter false

                val normalizedName = java.text.Normalizer.normalize(contact.name, java.text.Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .lowercase(java.util.Locale.ROOT)
                val cleanContactNumber = contact.number.filter { it.isDigit() || it == '+' }
                val t9 = if (contact.t9Mapping.isNotBlank()) contact.t9Mapping else com.example.util.T9HighlightHelper.nameToT9(contact.name)

                normalizedName.contains(normalizedQuery) ||
                (cleanQueryDigits.isNotEmpty() && cleanContactNumber.contains(cleanQueryDigits)) ||
                contact.number.contains(cleanQuery, ignoreCase = true) ||
                t9.contains(cleanQuery, ignoreCase = true)
            }.distinctBy { it.number }
            .map { contact ->
                DialpadMatch(
                    number = contact.number,
                    name = formatContactName(contact.name, nameFormat),
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
                    if (record.number.isBlank() || record.number in contactNumbers) return@filter false

                    val normalizedName = java.text.Normalizer.normalize(record.name, java.text.Normalizer.Form.NFD)
                        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                        .lowercase(java.util.Locale.ROOT)
                    val cleanRecNumber = record.number.filter { it.isDigit() || it == '+' }
                    val t9 = com.example.util.T9HighlightHelper.nameToT9(record.name)

                    normalizedName.contains(normalizedQuery) ||
                    (cleanQueryDigits.isNotEmpty() && cleanRecNumber.contains(cleanQueryDigits)) ||
                    record.number.contains(cleanQuery, ignoreCase = true) ||
                    t9.contains(cleanQuery, ignoreCase = true)
                }.distinctBy { it.number }
                .map { record ->
                    DialpadMatch(
                        number = record.number,
                        name = if (record.name == "Unknown" || record.name.isBlank()) record.name else formatContactName(record.name, nameFormat),
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
    var avatarShapeType = mutableStateOf(prefs.getString("avatar_shape_type", "squircle") ?: "squircle")
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
    var fakeCallAudioUri = mutableStateOf<String?>(null)
    var fakeCallVoiceType = mutableStateOf("NONE")
    var fakeCallState = mutableStateOf("RINGING")
    var fakeCallStartTimestamp = 0L
    var dialpadTonesEnabled = mutableStateOf(prefs.getBoolean("dialpad_tones_enabled", true))
    var vibrateOnClickEnabled = mutableStateOf(prefs.getBoolean("vibrate_on_click_enabled", true))
    var flipToSilenceEnabled = mutableStateOf(prefs.getBoolean("flip_to_silence_enabled", false))
    var isHideCallerIdEnabled = mutableStateOf(prefs.getBoolean("is_hide_caller_id_enabled", false))
    var clirPrefix = mutableStateOf(prefs.getString("clir_prefix", "#31#") ?: "#31#")

    // DUAL SIM FIX: Default to "Ask" on first launch so user is never locked out of SIM 2
    var preferredSim = mutableStateOf(prefs.getString("preferred_sim", "Ask") ?: "Ask")
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
            val savedSim = repository.getPreferredSim()
            preferredSim.value = if (savedSim.isNotBlank()) savedSim else "Ask"
            voicemailNumber.value = repository.getVoicemailNumber()

            try {
                val settings = repository.dao.getAllSettingsList().associate { it.key to it.value }
                val editor = prefs.edit()

                settings["dialpad_tones_enabled"]?.toBooleanStrictOrNull()?.let {
                    dialpadTonesEnabled.value = it
                    editor.putBoolean("dialpad_tones_enabled", it)
                }
                settings["vibrate_on_click_enabled"]?.toBooleanStrictOrNull()?.let {
                    vibrateOnClickEnabled.value = it
                    editor.putBoolean("vibrate_on_click_enabled", it)
                    com.example.util.RichHapticEngine.setHapticsEnabled(it)
                } ?: run {
                    com.example.util.RichHapticEngine.setHapticsEnabled(vibrateOnClickEnabled.value)
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
        settingsCoordinator.updatePreferredSim(sim)
    }

    fun updateDarkTheme(dark: Boolean) {
        isDarkTheme.value = dark
        settingsCoordinator.updateDarkTheme(dark)
    }

    fun updateAmoledMode(amoled: Boolean) {
        isAmoledMode.value = amoled
        settingsCoordinator.updateAmoledMode(amoled, isDarkTheme.value) { isDarkTheme.value = true }
    }

    fun updateCustomColorHex(hex: String) {
        customColorHex.value = hex
        settingsCoordinator.updateCustomColorHex(hex)
    }

    fun updateM3Expressive(expressive: Boolean) {
        isM3Expressive.value = expressive
        settingsCoordinator.updateM3Expressive(expressive)
    }

    fun updateAvatarShapeType(shapeType: String) {
        avatarShapeType.value = shapeType
        settingsCoordinator.updateAvatarShapeType(shapeType)
    }

    fun updateUseDynamicColor(dynamic: Boolean) {
        useDynamicColor.value = dynamic
        settingsCoordinator.updateUseDynamicColor(dynamic)
    }

    fun updateThemeColor(color: String) {
        themeColor.value = color
        settingsCoordinator.updateThemeColor(color)
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
        settingsCoordinator.updateSettingBoolean("dialpad_tones_enabled", enabled)
    }

    fun updateVibrateOnClickEnabled(enabled: Boolean) {
        vibrateOnClickEnabled.value = enabled
        settingsCoordinator.updateVibrateOnClickEnabled(enabled)
    }

    fun updateFlipToSilenceEnabled(enabled: Boolean) {
        flipToSilenceEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("flip_to_silence_enabled", enabled)
    }

    fun updateCallWaitingEnabled(enabled: Boolean) {
        callWaitingEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("call_waiting_enabled", enabled)
    }

    fun updateHideCallerIdEnabled(enabled: Boolean) {
        isHideCallerIdEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_hide_caller_id_enabled", enabled)
    }

    fun updateClirPrefix(prefix: String) {
        val sanitized = prefix.trim().ifBlank { "#31#" }
        clirPrefix.value = sanitized
        settingsCoordinator.updateSettingString("clir_prefix", sanitized)
    }

    fun updateRecordingEnabled(enabled: Boolean) {
        recordingEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("recording_enabled", enabled)
    }

    fun updateAutoTuneRecordingVolume(enabled: Boolean) {
        autoTuneRecordingVolume.value = enabled
        settingsCoordinator.updateSettingBoolean("auto_tune_recording_volume", enabled)
    }

    fun updateRecordingChimeEnabled(enabled: Boolean) {
        recordingChimeEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("recording_chime_enabled", enabled)
    }

    fun updateBiometricLockEnabled(enabled: Boolean) {
        isBiometricLockEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_biometric_lock_enabled", enabled)
    }

    fun updateRecordingsBiometricLockEnabled(enabled: Boolean) {
        isRecordingsBiometricLockEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_recordings_biometric_lock_enabled", enabled)
    }

    fun updateAutoRecordCallsEnabled(enabled: Boolean) {
        isAutoRecordCallsEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_auto_record_calls_enabled", enabled)
    }

    fun updateDynamicIslandEnabled(enabled: Boolean) {
        isDynamicIslandEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_dynamic_island_enabled", enabled)
    }

    fun updateDynamicIslandSpeakerOnly(speakerOnly: Boolean) {
        isDynamicIslandSpeakerOnly.value = speakerOnly
        settingsCoordinator.updateSettingBoolean("is_dynamic_island_speaker_only", speakerOnly)
    }

    fun updateAutoExportRecordingsEnabled(enabled: Boolean) {
        isAutoExportRecordingsEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_auto_export_recordings_enabled", enabled)
    }

    fun updateRecordingCompressionProfile(profile: RecordingCompressionProfile) {
        recordingCompressionProfile.value = profile
        settingsCoordinator.updateRecordingCompressionProfile(profile)
    }

    fun updatePocketProtectionEnabled(enabled: Boolean) {
        isPocketProtectionEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_pocket_protection_enabled", enabled)
    }

    fun updateCallbackRemindersEnabled(enabled: Boolean) {
        isCallbackRemindersEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_callback_reminders_enabled", enabled)
    }

    fun updateCallNotesEnabled(enabled: Boolean) {
        isCallNotesEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_call_notes_enabled", enabled)
    }

    fun updateFakeCallSimulatorEnabled(enabled: Boolean) {
        isFakeCallSimulatorEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_fake_call_simulator_enabled", enabled)
    }

    fun updateDashboardMode(mode: String) {
        dashboardMode.value = mode
        settingsCoordinator.updateSettingString("dashboard_mode", mode)
    }

    fun updateCallLogDashboardEnabled(enabled: Boolean) {
        isCallLogDashboardEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_call_log_dashboard_enabled", enabled)
    }

    fun updateCallLogFiltersEnabled(enabled: Boolean) {
        isCallLogFiltersEnabled.value = enabled
        settingsCoordinator.updateSettingBoolean("is_call_log_filters_enabled", enabled)
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
        settingsCoordinator.updateVoicemailNumber(num)
    }

    fun addBlockedNumber(num: String) = spamAndBackupCoordinator.addBlockedNumber(num)
    fun removeBlockedNumber(num: String) = spamAndBackupCoordinator.removeBlockedNumber(num)

    fun writeTextToUri(uri: Uri, content: String, onResult: (Boolean) -> Unit) =
        spamAndBackupCoordinator.writeTextToUri(uri, content, onResult)

    fun readTextFromUri(uri: Uri, onResult: (String?) -> Unit) =
        spamAndBackupCoordinator.readTextFromUri(uri, onResult)

    fun exportBlockedNumbers(onResult: (String) -> Unit) =
        spamAndBackupCoordinator.exportBlockedNumbers(onResult)

    fun importBlockedNumbers(rawData: String, onResult: (Int) -> Unit) =
        spamAndBackupCoordinator.importBlockedNumbers(rawData, onResult)

    fun exportBackup(password: String = "", onResult: (String) -> Unit) =
        spamAndBackupCoordinator.exportBackup(password, onResult)

    fun importBackup(rawData: String, password: String = "", onResult: (Boolean) -> Unit) =
        spamAndBackupCoordinator.importBackup(rawData, password, onResult)

    fun exportContactsVcf(onResult: (String) -> Unit) =
        spamAndBackupCoordinator.exportContactsVcf(onResult)

    fun importContactsVcf(rawData: String, onResult: (Boolean) -> Unit) =
        spamAndBackupCoordinator.importContactsVcf(rawData, onResult)

    fun saveSpeedDial(key: Int, num: String, name: String) =
        spamAndBackupCoordinator.saveSpeedDial(key, num, name)

    fun deleteSpeedDial(key: Int) =
        spamAndBackupCoordinator.deleteSpeedDial(key)

    fun addQuickResponse(msg: String) =
        spamAndBackupCoordinator.addQuickResponse(msg)

    fun deleteQuickResponse(resp: QuickResponse) =
        spamAndBackupCoordinator.deleteQuickResponse(resp)

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

    fun saveCallNote(number: String, note: String) = spamAndBackupCoordinator.saveCallNote(number, note)
    suspend fun getCallNote(number: String): CallNote? = spamAndBackupCoordinator.getCallNote(number)
    fun deleteCallNote(number: String) = spamAndBackupCoordinator.deleteCallNote(number)
    fun deleteCallNoteById(id: Long) = spamAndBackupCoordinator.deleteCallNoteById(id)
    fun getCallNotesForNumberFlow(number: String): Flow<List<CallNote>> = spamAndBackupCoordinator.getCallNotesForNumberFlow(number)

    fun toggleCallRecording(context: Context, phoneNumber: String, callerName: String = "Unknown") =
        recordingsCoordinator.toggleCallRecording(context, phoneNumber, callerName, isAutoExportRecordingsEnabled.value, isCallNotesEnabled.value)

    fun saveCallRecording(recording: CallRecording) =
        recordingsCoordinator.saveCallRecording(recording, isAutoExportRecordingsEnabled.value, isCallNotesEnabled.value)

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

    fun deleteCallRecording(id: Int) = recordingsCoordinator.deleteCallRecording(id)
    fun updateCallRecordingNote(id: Int, note: String) = recordingsCoordinator.updateCallRecordingNote(id, note)

    fun syncRecordingsFromDisk(context: Context, onComplete: ((Int) -> Unit)? = null) =
        recordingsCoordinator.syncRecordingsFromDisk(context, onComplete)

    fun recoverRecordingsFromDisk(context: Context, onComplete: ((Int) -> Unit)? = null) =
        recordingsCoordinator.recoverRecordingsFromDisk(context, onComplete)

    fun exportRecordingToDownloads(context: Context, filePath: String): Boolean =
        recordingsCoordinator.exportRecordingToDownloads(context, filePath)

    fun exportAllRecordingsToDownloads(context: Context): Int =
        recordingsCoordinator.exportAllRecordingsToDownloads(context)

    fun cleanupCorruptOrEmptyRecordings(context: Context): Int =
        recordingsCoordinator.cleanupCorruptOrEmptyRecordings(context)

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
        settingsCoordinator.updateSettingBoolean("flash_alerts_enabled", enabled)
    }

    fun addSpamNumber(number: String, label: String = "Spam") =
        spamAndBackupCoordinator.addSpamNumber(number, label)

    fun deleteSpamNumber(spam: SpamNumber) =
        spamAndBackupCoordinator.deleteSpamNumber(spam)

    fun clearAllSpam() =
        spamAndBackupCoordinator.clearAllSpam()

    fun importSpamNumbersFromCsv(csvContent: String, onResult: (Int) -> Unit) =
        spamAndBackupCoordinator.importSpamNumbersFromCsv(csvContent, onResult)

    fun exportSpamNumbersToCsv(onResult: (String) -> Unit) =
        spamAndBackupCoordinator.exportSpamNumbersToCsv(onResult)

    fun addReminder(number: String, name: String, triggerTime: Long, note: String = "", onResult: (Long) -> Unit = {}) =
        spamAndBackupCoordinator.addReminder(number, name, triggerTime, note, onResult)

    fun updateReminder(reminder: CallReminder) =
        spamAndBackupCoordinator.updateReminder(reminder)

    fun deleteReminder(reminder: CallReminder) =
        spamAndBackupCoordinator.deleteReminder(reminder)

    fun deleteReminderById(id: Int) =
        spamAndBackupCoordinator.deleteReminderById(id)

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