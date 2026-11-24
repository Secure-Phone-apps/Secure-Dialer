/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Coordinates in-call audio routing, user-intent tracking, and Bluetooth SCO / LE Audio / Wired Headset
 * disconnect recovery across Android 9–17 (API 28–37+) and aggressive OEM HAL stacks.
 */
object CallAudioRouteCoordinator {

    private const val USER_INTENT_WINDOW_MS = 2500L

    // Mirror CallEndpoint.TYPE_* constants for safe SDK-agnostic evaluation on Android 9-13 (API 28-33)
    const val ENDPOINT_TYPE_EARPIECE = 1
    const val ENDPOINT_TYPE_BLUETOOTH = 2
    const val ENDPOINT_TYPE_WIRED_HEADSET = 3
    const val ENDPOINT_TYPE_SPEAKER = 4
    const val ENDPOINT_TYPE_STREAMING = 5

    @Volatile
    private var lastUserRequestedRoute: Int? = null

    @Volatile
    private var lastUserRequestedAtMs: Long = 0L

    fun noteUserRequestedRoute(route: Int, nowMs: Long = System.currentTimeMillis()) {
        lastUserRequestedRoute = route
        lastUserRequestedAtMs = nowMs
    }

    fun clearUserIntent() {
        lastUserRequestedRoute = null
        lastUserRequestedAtMs = 0L
    }

    fun resolveDefaultFallbackRoute(supportedRouteMask: Int): Int {
        return when {
            (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET) != 0 ->
                CallAudioState.ROUTE_WIRED_HEADSET
            (supportedRouteMask and CallAudioState.ROUTE_EARPIECE) != 0 || supportedRouteMask == 0 ->
                CallAudioState.ROUTE_EARPIECE
            (supportedRouteMask and CallAudioState.ROUTE_SPEAKER) != 0 ->
                CallAudioState.ROUTE_SPEAKER
            else -> CallAudioState.ROUTE_EARPIECE
        }
    }

    /**
     * Determines whether an automatic audio-route recovery transition should fire when Bluetooth
     * (HFP/SCO/LE Audio) or a Wired Headset disconnects unexpectedly during an active call.
     *
     * Respects intentional user switches (e.g. tapping Speaker or Earpiece) and prefers a connected
     * wired headset before falling back to Earpiece (or Speaker on earpiece-less tablets).
     */
    fun selectRecoveryRoute(
        previousRoute: Int,
        currentRoute: Int,
        supportedRouteMask: Int,
        userRequestedRoute: Int? = lastUserRequestedRoute,
        userRequestAgeMs: Long = System.currentTimeMillis() - lastUserRequestedAtMs
    ): Int? {
        val isRecentUserSwitch = userRequestedRoute != null &&
            userRequestAgeMs in 0L..USER_INTENT_WINDOW_MS &&
            currentRoute == userRequestedRoute

        if (isRecentUserSwitch) return null

        // Never override when the user explicitly kept Speaker active
        if (currentRoute == CallAudioState.ROUTE_SPEAKER && userRequestedRoute == CallAudioState.ROUTE_SPEAKER) {
            return null
        }

        // Case 1: Unexpected drop off Bluetooth (buds powered off, out of range, SCO failure)
        if (previousRoute == CallAudioState.ROUTE_BLUETOOTH && currentRoute != CallAudioState.ROUTE_BLUETOOTH) {
            val wiredSupported = (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET) != 0
            if (currentRoute == CallAudioState.ROUTE_WIRED_HEADSET) return null
            if (currentRoute == CallAudioState.ROUTE_SPEAKER && !wiredSupported &&
                userRequestedRoute == CallAudioState.ROUTE_SPEAKER
            ) {
                return null
            }
            val preferredFallback = resolveDefaultFallbackRoute(supportedRouteMask)
            return if (currentRoute != preferredFallback) preferredFallback else null
        }

        // Case 2: Wired headset unplugged during call
        if (previousRoute == CallAudioState.ROUTE_WIRED_HEADSET &&
            currentRoute != CallAudioState.ROUTE_WIRED_HEADSET &&
            currentRoute != CallAudioState.ROUTE_BLUETOOTH &&
            currentRoute != CallAudioState.ROUTE_SPEAKER
        ) {
            val preferredFallback = resolveDefaultFallbackRoute(
                supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET.inv()
            )
            return if (currentRoute != preferredFallback) preferredFallback else null
        }

        return null
    }

    fun selectScoDropRecoveryRoute(
        currentRoute: Int,
        supportedRouteMask: Int
    ): Int? {
        if (currentRoute == CallAudioState.ROUTE_SPEAKER || currentRoute == CallAudioState.ROUTE_WIRED_HEADSET) {
            return null
        }
        val maskWithoutBt = supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH.inv()
        return resolveDefaultFallbackRoute(maskWithoutBt)
    }

    fun mapEndpointTypeToRoute(endpointType: Int): Int {
        return when (endpointType) {
            ENDPOINT_TYPE_BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
            ENDPOINT_TYPE_WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
            ENDPOINT_TYPE_SPEAKER -> CallAudioState.ROUTE_SPEAKER
            ENDPOINT_TYPE_EARPIECE -> CallAudioState.ROUTE_EARPIECE
            else -> CallAudioState.ROUTE_EARPIECE
        }
    }

    fun mapRouteToEndpointType(route: Int): Int {
        return when (route) {
            CallAudioState.ROUTE_BLUETOOTH -> ENDPOINT_TYPE_BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> ENDPOINT_TYPE_WIRED_HEADSET
            CallAudioState.ROUTE_SPEAKER -> ENDPOINT_TYPE_SPEAKER
            else -> ENDPOINT_TYPE_EARPIECE
        }
    }

    fun computeSupportedRouteMask(endpointTypes: Iterable<Int>): Int {
        var mask = 0
        for (type in endpointTypes) {
            when (type) {
                ENDPOINT_TYPE_EARPIECE -> mask = mask or CallAudioState.ROUTE_EARPIECE
                ENDPOINT_TYPE_BLUETOOTH -> mask = mask or CallAudioState.ROUTE_BLUETOOTH
                ENDPOINT_TYPE_WIRED_HEADSET -> mask = mask or CallAudioState.ROUTE_WIRED_HEADSET
                ENDPOINT_TYPE_SPEAKER -> mask = mask or CallAudioState.ROUTE_SPEAKER
            }
        }
        return mask
    }

    /**
     * Releases any stuck legacy SCO or API 31+ communication-device lock held by OEM audio HALs
     * after a Bluetooth headset abruptly powers off or drops out of range.
     */
    fun clearStaleBluetoothHal(context: Context?) {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                am.clearCommunicationDevice()
            } catch (_: Exception) {}
        }
        try {
            @Suppress("DEPRECATION")
            am.stopBluetoothSco()
        } catch (_: Exception) {}
        try {
            @Suppress("DEPRECATION")
            if (am.isBluetoothScoOn) {
                am.isBluetoothScoOn = false
            }
        } catch (_: Exception) {}
    }

    fun isCommunicationBluetoothDevice(deviceType: Int): Boolean {
        return deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            deviceType == 26 || // AudioDeviceInfo.TYPE_BLE_HEADSET (API 31)
            deviceType == 27 || // AudioDeviceInfo.TYPE_BLE_SPEAKER (API 31)
            deviceType == AudioDeviceInfo.TYPE_HEARING_AID
    }

    fun isWiredHeadsetDevice(deviceType: Int): Boolean {
        return deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            deviceType == AudioDeviceInfo.TYPE_USB_HEADSET
    }

    /**
     * Routes call audio using [InCallService.requestCallEndpointChange] on Android 14–17 (API 34+)
     * when a matching [CallEndpoint] is known, and falls back to [InCallService.setAudioRoute] on
     * Android 9–13 (API 28–33) or if endpoint metadata is unavailable.
     */
    fun routeAudio(
        inCallService: InCallService?,
        targetRoute: Int,
        availableEndpoints: List<Any> = emptyList(),
        isUserAction: Boolean = true
    ) {
        if (isUserAction) {
            noteUserRequestedRoute(targetRoute)
        }
        if (inCallService == null) {
            val ctx = com.example.CallManager.appContext
            val am = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                try {
                    if (targetRoute == CallAudioState.ROUTE_SPEAKER) {
                        am.isSpeakerphoneOn = true
                    } else if (targetRoute == CallAudioState.ROUTE_EARPIECE) {
                        am.isSpeakerphoneOn = false
                    }
                } catch (_: Exception) {}
            }
            return
        }

        if (targetRoute != CallAudioState.ROUTE_BLUETOOTH) {
            // Defensively clear stuck SCO flags when switching away from Bluetooth on OEM HALs
            try {
                clearStaleBluetoothHal(inCallService)
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val targetEndpointType = mapRouteToEndpointType(targetRoute)
            val switched = Api34EndpointBridge.tryRequestEndpoint(
                inCallService = inCallService,
                endpointsRaw = availableEndpoints,
                targetEndpointType = targetEndpointType
            )
            if (switched) return
        }

        try {
            @Suppress("DEPRECATION")
            inCallService.setAudioRoute(targetRoute)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    object Api34EndpointBridge {

        fun synthesizeCallAudioState(
            previousState: CallAudioState?,
            currentEndpoint: CallEndpoint,
            availableEndpoints: List<CallEndpoint>
        ): CallAudioState {
            val isMuted = previousState?.isMuted ?: false
            val currentRoute = mapEndpointTypeToRoute(currentEndpoint.endpointType)
            val mask = computeSupportedRouteMask(availableEndpoints.map { it.endpointType })
                .let { if (it == 0) currentRoute else (it or currentRoute) }
            @Suppress("DEPRECATION")
            return CallAudioState(isMuted, currentRoute, mask)
        }

        fun tryRequestEndpoint(
            inCallService: InCallService,
            endpointsRaw: List<Any>,
            targetEndpointType: Int
        ): Boolean {
            val endpoints = endpointsRaw.filterIsInstance<CallEndpoint>()
            val match = endpoints.firstOrNull { it.endpointType == targetEndpointType } ?: return false
            return try {
                inCallService.requestCallEndpointChange(
                    match,
                    ContextCompat.getMainExecutor(inCallService),
                    object : OutcomeReceiver<Void, CallEndpointException> {
                        override fun onResult(result: Void?) {}
                        override fun onError(error: CallEndpointException) {
                            try {
                                @Suppress("DEPRECATION")
                                inCallService.setAudioRoute(mapEndpointTypeToRoute(targetEndpointType))
                            } catch (_: Exception) {}
                        }
                    }
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
