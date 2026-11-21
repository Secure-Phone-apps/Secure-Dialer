/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ContainerWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

enum class ContainerHeightClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Immutable
data class AdaptiveLayoutMetrics(
    val widthClass: ContainerWidthClass = ContainerWidthClass.COMPACT,
    val heightClass: ContainerHeightClass = ContainerHeightClass.MEDIUM,
    val maxWidth: Dp = 411.dp,
    val maxHeight: Dp = 800.dp,
    val fontScale: Float = 1.0f,
    val useNavigationRail: Boolean = false,
    val useTwoPaneInCall: Boolean = false,
    val useTwoPaneDialpad: Boolean = false,
    val isVeryNarrowWidth: Boolean = false,
    val isTightVerticalSpace: Boolean = false,
    val dialButtonHeight: Dp = 58.dp,
    val dialActionHeight: Dp = 60.dp,
    val overlayDialButtonHeight: Dp = 52.dp,
    val overlayCallButtonHeight: Dp = 64.dp,
    val inCallAvatarSize: Dp = 120.dp,
    val inCallControlButtonHeight: Dp = 68.dp,
    val inCallKeypadButtonHeight: Dp = 60.dp,
    val contentMaxWidth: Dp = 840.dp
)

val LocalAdaptiveLayout = compositionLocalOf { AdaptiveLayoutMetrics() }

fun computeAdaptiveLayoutMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    fontScale: Float = 1.0f
): AdaptiveLayoutMetrics {
    val widthClass = when {
        maxWidth >= 840.dp -> ContainerWidthClass.EXPANDED
        maxWidth >= 600.dp -> ContainerWidthClass.MEDIUM
        else -> ContainerWidthClass.COMPACT
    }
    val heightClass = when {
        maxHeight >= 900.dp -> ContainerHeightClass.EXPANDED
        maxHeight >= 480.dp -> ContainerHeightClass.MEDIUM
        else -> ContainerHeightClass.COMPACT
    }

    val isLandscapePhoneOrMultiWindow = maxWidth >= 480.dp && maxHeight < 480.dp
    val useNavigationRail = widthClass != ContainerWidthClass.COMPACT || isLandscapePhoneOrMultiWindow
    val useTwoPaneInCall = maxWidth >= 640.dp && maxHeight >= 300.dp
    val useTwoPaneDialpad = (maxWidth >= 620.dp && maxHeight < 680.dp) || widthClass == ContainerWidthClass.EXPANDED

    val isVeryNarrowWidth = maxWidth < 340.dp
    val isTightVerticalSpace = maxHeight < 580.dp || (fontScale >= 1.35f && maxHeight < 720.dp)

    val dialButtonHeight = when {
        maxHeight < 420.dp -> 44.dp
        fontScale >= 1.35f && maxHeight >= 520.dp -> 56.dp
        maxHeight < 560.dp -> 50.dp
        isTightVerticalSpace -> 52.dp
        else -> 58.dp
    }
    val dialActionHeight = when {
        maxHeight < 420.dp -> 48.dp
        isTightVerticalSpace -> 52.dp
        else -> 60.dp
    }
    val overlayDialButtonHeight = when {
        maxHeight < 460.dp -> 44.dp
        fontScale >= 1.35f && maxHeight >= 520.dp -> 54.dp
        maxHeight < 600.dp -> 48.dp
        else -> 52.dp
    }
    val overlayCallButtonHeight = when {
        maxHeight < 460.dp -> 48.dp
        maxHeight < 600.dp -> 54.dp
        else -> 64.dp
    }

    val inCallAvatarSize = when {
        maxHeight < 460.dp -> 72.dp
        isTightVerticalSpace -> 88.dp
        else -> 120.dp
    }
    val inCallControlButtonHeight = when {
        maxHeight < 420.dp -> 50.dp
        isTightVerticalSpace && fontScale >= 1.35f && maxHeight >= 560.dp -> 74.dp
        isTightVerticalSpace && fontScale >= 1.35f -> 66.dp
        isTightVerticalSpace -> 58.dp
        else -> 68.dp
    }
    val inCallKeypadButtonHeight = when {
        maxHeight < 440.dp -> 44.dp
        fontScale >= 1.35f && maxHeight >= 500.dp -> 54.dp
        isTightVerticalSpace -> 50.dp
        else -> 60.dp
    }

    val contentMaxWidth = when (widthClass) {
        ContainerWidthClass.EXPANDED -> 840.dp
        ContainerWidthClass.MEDIUM -> 680.dp
        ContainerWidthClass.COMPACT -> maxWidth
    }

    return AdaptiveLayoutMetrics(
        widthClass = widthClass,
        heightClass = heightClass,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        fontScale = fontScale,
        useNavigationRail = useNavigationRail,
        useTwoPaneInCall = useTwoPaneInCall,
        useTwoPaneDialpad = useTwoPaneDialpad,
        isVeryNarrowWidth = isVeryNarrowWidth,
        isTightVerticalSpace = isTightVerticalSpace,
        dialButtonHeight = dialButtonHeight,
        dialActionHeight = dialActionHeight,
        overlayDialButtonHeight = overlayDialButtonHeight,
        overlayCallButtonHeight = overlayCallButtonHeight,
        inCallAvatarSize = inCallAvatarSize,
        inCallControlButtonHeight = inCallControlButtonHeight,
        inCallKeypadButtonHeight = inCallKeypadButtonHeight,
        contentMaxWidth = contentMaxWidth
    )
}
