package com.example.ui.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.model.CallRecord
import com.example.model.Contact

class DialerViewModel : ViewModel() {
    // State moved from MainActivity
    var searchQuery = mutableStateOf("")
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
    var isLoadingPermissions = mutableStateOf(true)

    var isCallActive = mutableStateOf(false)
    var callingContactName = mutableStateOf("")
    var callingContactNumber = mutableStateOf("")
    
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
    
    val callHistory = mutableStateListOf<CallRecord>()
    val contactsList = mutableStateListOf<Contact>()
}
