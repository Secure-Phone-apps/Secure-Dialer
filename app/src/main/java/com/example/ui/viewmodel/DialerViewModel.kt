package com.example.ui.viewmodel

import android.app.Application
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

    init {
        // Initial sync
        syncData()
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

    fun toggleFavorite(number: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(number, isFavorite)
        }
    }
}
