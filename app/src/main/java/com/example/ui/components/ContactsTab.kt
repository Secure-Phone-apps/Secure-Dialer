package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.model.Contact
import androidx.compose.ui.res.stringResource
import com.example.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.launch

@Composable
fun ContactsTabContent(
    contactsPaged: LazyPagingItems<Contact>,
    onCallClick: (Contact) -> Unit,
    onAddContactClick: () -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {},
    onEditContact: (Contact) -> Unit = {},
    onDeleteContact: (Contact) -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val alphabet = ('A'..'Z').toList() + '#'

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!hasPermission && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_required),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.contacts_perm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(stringResource(R.string.enable_contacts_perm))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.contacts_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddContactClick,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_contact_button")
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_new_contact), style = MaterialTheme.typography.labelLarge)
                }
            }

            if (contactsPaged.itemCount == 0 && contactsPaged.loadState.refresh is LoadState.NotLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateIllustration(
                        title = stringResource(R.string.no_contacts_title),
                        subtitle = stringResource(R.string.no_contacts_subtitle)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 24.dp)
                ) {
                    items(
                        count = contactsPaged.itemCount,
                        key = contactsPaged.itemKey { it.number },
                        contentType = contactsPaged.itemContentType { "contact" }
                    ) { index ->
                        val contact = contactsPaged[index]
                        if (contact != null) {
                            val firstLetter = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            val prevContact = if (index > 0) contactsPaged[index - 1] else null
                            val prevLetter = prevContact?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                            
                            if (firstLetter != prevLetter) {
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                                )
                            }
                            ContactRow(
                                contact = contact,
                                onCallClick = onCallClick,
                                onToggleFavorite = onToggleFavorite,
                                onEditContact = onEditContact,
                                onDeleteContact = onDeleteContact
                            )
                        }
                    }

                    if (contactsPaged.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        // A-Z Scroller Rail
        if (contactsPaged.itemCount > 0) {
            val haptic = LocalHapticFeedback.current
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(32.dp)
                    .padding(vertical = 60.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, _ ->
                            val index = (change.position.y / size.height * alphabet.size)
                                .toInt()
                                .coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[index].toString()
                            
                            // Find first contact starting with this letter
                            for (i in 0 until contactsPaged.itemCount) {
                                val c = contactsPaged[i]
                                if (c != null && (c.name.startsWith(letter, ignoreCase = true) || (letter == "#" && !c.name[0].isLetter()))) {
                                    coroutineScope.launch {
                                        listState.scrollToItem(i)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    break
                                }
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                alphabet.forEach { char ->
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            val letter = char.toString()
                            for (i in 0 until contactsPaged.itemCount) {
                                val c = contactsPaged[i]
                                if (c != null && (c.name.startsWith(letter, ignoreCase = true) || (letter == "#" && !c.name[0].isLetter()))) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(i)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    break
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    contact: Contact,
    onCallClick: (Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isExpanded) {
        if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    } else {
        if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isExpanded = !isExpanded
            },
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Column(
                        modifier = Modifier.offset(x = (-8).dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "${contact.label} • ${contact.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingContent = null,
                leadingContent = {
                    Surface(
                        modifier = Modifier
                            .offset(x = (-8).dp)
                            .size(44.dp),
                        shape = CircleShape,
                        color = contact.avatarBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = contact.avatarTextColor
                            )
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite(contact)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (contact.favorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    ContactActionItem(
                        icon = Icons.Default.Call,
                        label = "Call",
                        onClick = { onCallClick(contact) },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    ContactActionItem(
                        icon = Icons.Default.Email,
                        label = "Message",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${contact.number}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open messages", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    ContactActionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit",
                        onClick = { onEditContact(contact) },
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    ContactActionItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = { onDeleteContact(contact) },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ContactActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(64.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

data class Country(val code: String, val name: String, val prefix: String, val flag: String)

val COUNTRIES = listOf(
    Country("IN", "India", "+91", "🇮🇳"),
    Country("US", "United States", "+1", "🇺🇸"),
    Country("GB", "United Kingdom", "+44", "🇬🇧"),
    Country("CA", "Canada", "+1", "🇨🇦"),
    Country("AU", "Australia", "+61", "🇦🇺"),
    Country("DE", "Germany", "+49", "🇩🇪"),
    Country("FR", "France", "+33", "🇫🇷"),
    Country("IT", "Italy", "+39", "🇮🇹"),
    Country("ES", "Spain", "+34", "🇪🇸"),
    Country("JP", "Japan", "+81", "🇯🇵"),
    Country("CN", "China", "+86", "🇨🇳"),
    Country("BR", "Brazil", "+55", "🇧🇷"),
    Country("RU", "Russia", "+7", "🇷🇺"),
    Country("ZA", "South Africa", "+27", "🇿🇦"),
    Country("SG", "Singapore", "+65", "🇸🇬"),
    Country("MY", "Malaysia", "+60", "🇲🇾"),
    Country("ID", "Indonesia", "+62", "🇮🇩"),
    Country("AE", "United Arab Emirates", "+971", "🇦🇪"),
    Country("SA", "Saudi Arabia", "+966", "🇸🇦"),
    Country("PK", "Pakistan", "+92", "🇵🇰"),
    Country("BD", "Bangladesh", "+880", "🇧🇩"),
    Country("LK", "Sri Lanka", "+94", "🇱🇰"),
    Country("NP", "Nepal", "+977", "🇳🇵"),
    Country("MX", "Mexico", "+52", "🇲🇽"),
    Country("NZ", "New Zealand", "+64", "🇳🇿"),
    Country("NL", "Netherlands", "+31", "🇳🇱"),
    Country("CH", "Switzerland", "+41", "🇨🇭"),
    Country("SE", "Sweden", "+46", "🇸🇪"),
    Country("NO", "Norway", "+47", "🇳🇴")
)

@Composable
fun AddContactDialog(
    initialName: String,
    initialNumber: String,
    initialLabel: String,
    initialEmail: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String, label: String, email: String) -> Unit
) {
    val context = LocalContext.current
    val countryIso = remember {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            tm?.networkCountryIso?.uppercase() ?: tm?.simCountryIso?.uppercase() ?: java.util.Locale.getDefault().country.uppercase()
        } catch (e: Exception) {
            java.util.Locale.getDefault().country.uppercase()
        }
    }
    
    val defaultCountry = remember(countryIso) {
        COUNTRIES.firstOrNull { it.code == countryIso } ?: COUNTRIES.first { it.code == "IN" }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var rawNumberInput by remember { mutableStateOf("") }
    var selectedLabel by remember { mutableStateOf(initialLabel) }
    var emailInput by remember { mutableStateOf(initialEmail) }

    LaunchedEffect(initialName) {
        val trimmed = initialName.trim()
        if (trimmed.isNotEmpty()) {
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.isNotEmpty()) {
                firstName = parts[0]
                if (parts.size > 1) {
                    lastName = parts.subList(1, parts.size).joinToString(" ")
                }
            }
        }
    }

    LaunchedEffect(initialNumber) {
        if (initialNumber.startsWith("+")) {
            val matchingCountry = COUNTRIES
                .filter { initialNumber.startsWith(it.prefix) }
                .maxByOrNull { it.prefix.length }
            if (matchingCountry != null) {
                selectedCountry = matchingCountry
                rawNumberInput = initialNumber.substring(matchingCountry.prefix.length)
            } else {
                selectedCountry = defaultCountry
                rawNumberInput = initialNumber
            }
        } else {
            selectedCountry = defaultCountry
            rawNumberInput = initialNumber
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialName.isEmpty()) stringResource(R.string.add_contact_dialog_title) else stringResource(R.string.edit_contact_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text(stringResource(R.string.label_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_first_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(stringResource(R.string.label_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_last_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expandedCountryMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        OutlinedButton(
                            onClick = { expandedCountryMenu = true },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "${selectedCountry?.flag ?: "🌐"} ${selectedCountry?.prefix ?: ""}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Country",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expandedCountryMenu,
                            onDismissRequest = { expandedCountryMenu = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            COUNTRIES.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(country.flag)
                                            Text(country.name)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(country.prefix, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        selectedCountry = country
                                        expandedCountryMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = rawNumberInput,
                        onValueChange = { rawNumberInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                )

                Column {
                    Text(
                        text = "Label",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Mobile", "Work", "Home").forEach { option ->
                            FilterChip(
                                selected = selectedLabel == option,
                                onClick = { selectedLabel = option },
                                label = { Text(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = "${firstName.trim()} ${lastName.trim()}".trim()
                    val prefix = selectedCountry?.prefix ?: ""
                    val finalNumber = if (rawNumberInput.startsWith("+")) {
                        rawNumberInput.trim()
                    } else {
                        "$prefix${rawNumberInput.trim()}"
                    }
                    onConfirm(finalName, finalNumber, selectedLabel, emailInput.trim())
                },
                enabled = firstName.trim().isNotEmpty() && rawNumberInput.trim().isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
