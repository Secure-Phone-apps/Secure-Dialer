/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.telecom.Call
import java.util.IdentityHashMap

/**
 * Platform-agnostic contract representing a Telecom call leg so termination logic
 * can be unit-tested deterministically across all OEM and Android OS state transitions.
 */
interface TelecomCallHandle {
    val identityKey: Any
    val state: Int
    fun disconnectCall()
    fun rejectCall(rejectWithMessage: Boolean = false, textMessage: String? = null)
}

/**
 * Production wrapper around [android.telecom.Call] using object identity semantics.
 */
class PlatformTelecomCallHandle(val call: Call) : TelecomCallHandle {
    override val identityKey: Any get() = call
    override val state: Int get() = call.state

    override fun disconnectCall() {
        call.disconnect()
    }

    override fun rejectCall(rejectWithMessage: Boolean, textMessage: String?) {
        call.reject(rejectWithMessage, textMessage)
    }
}

/**
 * Coordinates outgoing-call cancellation latches, multi-list call sweeps, and
 * VoLTE/IMS/CSFB state-transition disconnect retries across Android 9 through Android 17.
 */
class OutgoingCallDisconnectCoordinator(
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val latchTtlMs: Long = 12_000L
) {
    private val lock = Any()
    private val disconnectRequested = IdentityHashMap<Any, Long>()
    private val preCancelledKeys = IdentityHashMap<Any, Boolean>()

    @Volatile
    private var outgoingPlacementDeadlineMs: Long = 0L

    @Volatile
    private var pendingCancelDeadlineMs: Long = 0L

    fun onOutgoingCallPlaced() {
        val now = clockMs()
        synchronized(lock) {
            outgoingPlacementDeadlineMs = now + latchTtlMs
            pendingCancelDeadlineMs = 0L
        }
    }

    fun clearPlacementWindow() {
        synchronized(lock) {
            outgoingPlacementDeadlineMs = 0L
            pendingCancelDeadlineMs = 0L
        }
    }

    fun isInPreAttachPlacementWindow(): Boolean {
        val now = clockMs()
        synchronized(lock) {
            if (outgoingPlacementDeadlineMs > 0L && now <= outgoingPlacementDeadlineMs) {
                return true
            }
            outgoingPlacementDeadlineMs = 0L
            return false
        }
    }

    fun hasPendingOutgoingCancel(): Boolean {
        val now = clockMs()
        synchronized(lock) {
            if (pendingCancelDeadlineMs > 0L && now <= pendingCancelDeadlineMs) {
                return true
            }
            pendingCancelDeadlineMs = 0L
            return false
        }
    }

    /**
     * Arms the pre-`onCallAdded` cancel latch when the user taps Hang Up before Telecom
     * attaches a `Call` object to `InCallService`.
     */
    fun armPreAttachCancel() {
        val now = clockMs()
        synchronized(lock) {
            pendingCancelDeadlineMs = now + latchTtlMs
            outgoingPlacementDeadlineMs = 0L
        }
    }

    /**
     * Evaluates whether a newly attached call in `InCallService.onCallAdded` matches a pending
     * pre-attach hang-up request and must be terminated immediately without surfacing UI.
     */
    fun shouldAutoDisconnectOnAdd(handle: TelecomCallHandle, isOutgoing: Boolean): Boolean {
        val now = clockMs()
        synchronized(lock) {
            if (isOutgoing) {
                outgoingPlacementDeadlineMs = 0L
            }
            val latchActive = pendingCancelDeadlineMs > 0L && now <= pendingCancelDeadlineMs
            if (latchActive && isOutgoing) {
                pendingCancelDeadlineMs = 0L
                disconnectRequested[handle.identityKey] = now
                preCancelledKeys[handle.identityKey] = true
                return true
            }
            if (!latchActive) {
                pendingCancelDeadlineMs = 0L
            }
            return false
        }
    }

    fun isPreCancelled(key: Any): Boolean {
        synchronized(lock) {
            return preCancelledKeys[key] == true
        }
    }

    fun isDisconnectRequested(key: Any): Boolean {
        synchronized(lock) {
            return disconnectRequested.containsKey(key)
        }
    }

    fun hasAnyPendingDisconnects(): Boolean {
        synchronized(lock) {
            return disconnectRequested.isNotEmpty()
        }
    }

    fun markDisconnectRequested(handles: Collection<TelecomCallHandle>) {
        val now = clockMs()
        synchronized(lock) {
            handles.forEach { handle ->
                if (handle.state != Call.STATE_DISCONNECTED) {
                    disconnectRequested[handle.identityKey] = now
                }
            }
        }
    }

    fun clearCall(key: Any) {
        synchronized(lock) {
            disconnectRequested.remove(key)
            preCancelledKeys.remove(key)
        }
    }

    /**
     * Called when Telecom delivers a state change for a tracked call.
     * Returns `true` if the user already requested disconnect and the modem/IMS stack moved the call
     * into a non-disconnecting active/setup state (e.g., `CONNECTING` -> `DIALING`), signaling
     * that `disconnect()` must be re-issued immediately.
     */
    fun onCallStateUpdated(handle: TelecomCallHandle, newState: Int): Boolean {
        synchronized(lock) {
            if (newState == Call.STATE_DISCONNECTED) {
                disconnectRequested.remove(handle.identityKey)
                preCancelledKeys.remove(handle.identityKey)
                return false
            }
            if (!disconnectRequested.containsKey(handle.identityKey)) {
                return false
            }
            return newState != Call.STATE_DISCONNECTING
        }
    }

    fun shouldRetryOnDetailsChange(handle: TelecomCallHandle, currentState: Int): Boolean {
        synchronized(lock) {
            return disconnectRequested.containsKey(handle.identityKey) &&
                currentState != Call.STATE_DISCONNECTED &&
                currentState != Call.STATE_DISCONNECTING
        }
    }

    /**
     * Unions all tracked calls and ground-truth `InCallService.calls` by object identity,
     * preserving unrelated incoming waiting calls when hanging up the primary call.
     */
    fun collectDisconnectTargets(
        trackedCalls: List<TelecomCallHandle>,
        serviceCalls: List<TelecomCallHandle>,
        currentCall: TelecomCallHandle?,
        waitingCall: TelecomCallHandle?
    ): List<TelecomCallHandle> {
        val uniqueByIdentity = IdentityHashMap<Any, TelecomCallHandle>()
        val ordered = mutableListOf<TelecomCallHandle>()

        fun addIfUnique(handle: TelecomCallHandle) {
            if (handle.state == Call.STATE_DISCONNECTED) return
            if (!uniqueByIdentity.containsKey(handle.identityKey)) {
                uniqueByIdentity[handle.identityKey] = handle
                ordered.add(handle)
            }
        }

        currentCall?.let(::addIfUnique)
        trackedCalls.forEach(::addIfUnique)
        serviceCalls.forEach(::addIfUnique)

        val hasDistinctPrimary = ordered.any {
            waitingCall == null || it.identityKey !== waitingCall.identityKey
        }
        return if (hasDistinctPrimary && waitingCall != null && waitingCall.state == Call.STATE_RINGING) {
            ordered.filter { it.identityKey !== waitingCall.identityKey }
        } else {
            ordered
        }
    }

    fun terminateCallHandle(handle: TelecomCallHandle): Boolean {
        return try {
            if (handle.state == Call.STATE_RINGING) {
                handle.rejectCall(false, null)
            } else {
                handle.disconnectCall()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun reset() {
        synchronized(lock) {
            disconnectRequested.clear()
            preCancelledKeys.clear()
            outgoingPlacementDeadlineMs = 0L
            pendingCancelDeadlineMs = 0L
        }
    }
}
