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

package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    viewModel: DialerViewModel,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var highlightedTitle by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (activeTab != 0) {
            activeTab = 0
        } else {
            onClose()
        }
    }

    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current
    val cardBgColor = if (isAmoled) {
        Color(0xFF000000)
    } else if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (activeTab) {
                            0 -> stringResource(R.string.settings_title)
                            1 -> stringResource(R.string.cat_appearance_color)
                            2 -> stringResource(R.string.cat_sound_gestures)
                            3 -> stringResource(R.string.cat_calling_accounts)
                            4 -> stringResource(R.string.cat_speed_dial_quick_responses)
                            5 -> stringResource(R.string.cat_call_blocking)
                            6 -> stringResource(R.string.cat_contacts_data)
                            7 -> stringResource(R.string.cat_advanced_features)
                            8 -> stringResource(R.string.cat_privacy_security_about)
                            else -> stringResource(R.string.settings_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeTab != 0) activeTab = 0 else onClose()
                    }) {
                        Icon(
                            imageVector = if (activeTab != 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(220)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(220)))
                    }
                },
                label = "settings_tab_animation",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> GeneralSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        onNavigateToTab = { tab, title ->
                            highlightedTitle = title
                            activeTab = tab
                        }
                    )
                    1 -> AppearanceSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    2 -> SoundAndGesturesSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    3 -> CallingAccountsSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    4 -> SpeedDialAndQuickResponsesSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    5 -> CallBlockingSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    6 -> ContactsAndDataSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    7 -> AdvancedFeaturesSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle
                    )
                    8 -> PrivacySecurityAboutSettings(
                        viewModel = viewModel,
                        cardBgColor = cardBgColor,
                        highlightedTitle = highlightedTitle,
                        onShowAbout = { showAboutDialog = true },
                        onShowPrivacy = { showPrivacyDialog = true }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun HighlightableCard(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    cardBgColor: Color,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isAmoled = LocalAmoledMode.current
    val cardBorder = if (isHighlighted) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else if (isAmoled) {
        BorderStroke(1.dp, Color(0xFF1E1E1E))
    } else {
        null
    }

    if (onClick != null) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = cardBorder,
            shape = shape,
            onClick = onClick,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = cardBorder,
            shape = shape,
            content = content
        )
    }
}

// FIXED: Restores case-insensitive deep-link and search card highlighting
fun isMatchTitle(title: String, highlightedTitle: String?): Boolean {
    if (highlightedTitle.isNullOrBlank() || title.isBlank()) return false
    return title.equals(highlightedTitle, ignoreCase = true) ||
            title.contains(highlightedTitle, ignoreCase = true) ||
            highlightedTitle.contains(title, ignoreCase = true)
}

@Composable
fun SettingsRowToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconBgColor: Color = Color.Transparent,
    iconTint: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current

    ListItem(
        modifier = Modifier.clickable(enabled = enabled) {
            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
            onCheckedChange(!checked)
        },
        headlineContent = {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (enabled) iconBgColor else iconBgColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (enabled) iconTint else iconTint.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
                }
            }
        } else null,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    onCheckedChange(it)
                },
                enabled = enabled,
                colors = if (isExpressive) {
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else SwitchDefaults.colors()
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsRowNav(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconBgColor: Color = Color.Transparent,
    iconTint: Color = Color.Unspecified
) {
    val context = LocalContext.current

    ListItem(
        modifier = Modifier.clickable {
            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
            onClick()
        },
        headlineContent = {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
        } else null,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun PreferenceHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
fun SettingsEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    tintColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}