package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalM3Expressive

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
    val dialogShape = RoundedCornerShape(16.dp)
    val fieldShape = RoundedCornerShape(16.dp)
    val buttonShape = RoundedCornerShape(16.dp)
    val isExpressive = LocalM3Expressive.current

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
        shape = dialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isExpressive) 6.dp else 3.dp,
        title = {
            Text(
                text = if (initialName.isEmpty()) stringResource(R.string.add_contact_dialog_title) else stringResource(R.string.edit_contact_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
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
                        label = { Text(stringResource(R.string.label_first_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_first_name_input"),
                        shape = fieldShape
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(stringResource(R.string.label_last_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_last_name_input"),
                        shape = fieldShape
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
                            shape = fieldShape,
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
                        label = { Text(stringResource(R.string.label_phone_number)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_phone_input"),
                        shape = fieldShape
                    )
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(stringResource(R.string.label_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_email_input"),
                    shape = fieldShape,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                )

                Column {
                    Text(
                        text = stringResource(R.string.label_type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            stringResource(R.string.label_mobile) to "Mobile",
                            stringResource(R.string.label_work) to "Work",
                            stringResource(R.string.label_home) to "Home"
                        ).forEach { (displayLabel, optionKey) ->
                            FilterChip(
                                selected = selectedLabel == optionKey,
                                onClick = { selectedLabel = optionKey },
                                label = { Text(displayLabel) },
                                modifier = Modifier.weight(1f),
                                shape = fieldShape
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
                enabled = firstName.trim().isNotEmpty() && rawNumberInput.trim().isNotEmpty(),
                shape = buttonShape
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = buttonShape
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
