package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.paging.*
import com.example.DialerRepository
import com.example.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DialerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DialerRepository(application)

    // Search Query
    var searchQuery = mutableStateOf("")
    private val _searchQueryFlow = MutableStateFlow("")

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
        _searchQueryFlow.value = newQuery
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val contactsPaged: Flow<PagingData<Contact>> = _searchQueryFlow
        .flatMapLatest { query -> repository.getContactsPaged(query) }
        .cachedIn(viewModelScope)

    val callHistoryPaged: Flow<PagingData<CallRecord>> = repository.getCallHistoryPaged()
        .cachedIn(viewModelScope)

    val favoriteContacts: StateFlow<List<Contact>> = repository.getFavoriteContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContactsFlow: StateFlow<List<Contact>> = repository.getAllContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = repository.context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

    // UI State
    var isDialpadVisible = mutableStateOf(false)
    var dialpadInput = mutableStateOf("")
    var isSettingsVisible = mutableStateOf(false)
    var isDarkTheme = mutableStateOf(prefs.getBoolean("is_dark_theme", true))
    var isM3Expressive = mutableStateOf(prefs.getBoolean("is_m3_expressive", true))
    var themeColor = mutableStateOf(prefs.getString("theme_color", "classic_slate") ?: "classic_slate")
    var defaultTab = mutableIntStateOf(prefs.getInt("default_tab", 0).coerceIn(0, 1))
    var callWaitingEnabled = mutableStateOf(prefs.getBoolean("call_waiting_enabled", true))
    var recordingEnabled = mutableStateOf(prefs.getBoolean("recording_enabled", false))
    var selectedTab = mutableIntStateOf(prefs.getInt("default_tab", 0).coerceIn(0, 1))
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

    fun updateM3Expressive(expressive: Boolean) {
        isM3Expressive.value = expressive
        prefs.edit().putBoolean("is_m3_expressive", expressive).apply()
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
        
        // Real-time sync observation
        repository.startObservingChanges {
            syncData()
        }
    }

    fun syncData() {
        viewModelScope.launch {
            repository.syncContacts()
            repository.syncCallLogs()
        }
    }

    fun addContact(name: String, number: String, label: String, email: String = "") {
        viewModelScope.launch {
            repository.addContact(name, number, label, email)
        }
    }

    fun saveCallNote(number: String, note: String) {
        viewModelScope.launch {
            repository.saveCallNote(number, note)
        }
    }

    suspend fun getCallNote(number: String): CallNote? {
        return repository.getCallNote(number)
    }

    fun deleteCallNote(number: String) {
        viewModelScope.launch {
            repository.deleteCallNote(number)
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

    fun deleteContact(number: String) {
        viewModelScope.launch {
            repository.deleteContact(number)
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
}
