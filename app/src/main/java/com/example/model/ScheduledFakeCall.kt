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

package com.example.model

import androidx.compose.runtime.Immutable
import org.json.JSONObject
import java.util.UUID

@Immutable
data class ScheduledFakeCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val triggerTimeMillis: Long,
    val audioUri: String? = null,
    val audioVoiceType: String = "NONE" // "NONE", "OFFICE", "FAMILY", "FRIEND", "CUSTOM"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("number", number)
            put("triggerTimeMillis", triggerTimeMillis)
            put("audioUri", audioUri ?: "")
            put("audioVoiceType", audioVoiceType)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ScheduledFakeCall {
            return ScheduledFakeCall(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Unknown"),
                number = json.optString("number", ""),
                triggerTimeMillis = json.optLong("triggerTimeMillis", 0L),
                audioUri = json.optString("audioUri", "").takeIf { it.isNotEmpty() },
                audioVoiceType = json.optString("audioVoiceType", "NONE")
            )
        }
    }
}
