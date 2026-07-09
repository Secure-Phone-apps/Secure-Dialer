package com.example.ui.viewmodel

import android.app.Application
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
    private val repository = DialerRepository(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            application.createAttributionContext("dialer")
        } else {
            application
        }
    )

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

    // UI State
    var isDialpadVisible = mutableStateOf(false)
    var dialpadInput = mutableStateOf("")
    var isSettingsVisible = mutableStateOf(false)
    var isDarkTheme = mutableStateOf(false)
    var selectedTab = mutableIntStateOf(0)
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
    var oldContactToEdit = mutableStateOf<Contact?>(null)
    var editContactName = mutableStateOf("")
    var editContactNumber = mutableStateOf("")
    var editContactLabel = mutableStateOf("Mobile")

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

    fun addContact(name: String, number: String, label: String) {
        viewModelScope.launch {
            repository.addContact(name, number, label)
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
