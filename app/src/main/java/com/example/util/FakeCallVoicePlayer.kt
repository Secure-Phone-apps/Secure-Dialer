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

package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

object FakeCallVoicePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isPlaying = false
    private var currentVoiceType: String = "NONE"
    private var isMuted = false
    private var isSpeaker = false

    fun isVoicePlaying(): Boolean = isPlaying

    fun playVoice(
        context: Context,
        voiceType: String,
        customAudioUri: String? = null,
        isSpeakerOn: Boolean = false,
        onPlaybackCompleted: (() -> Unit)? = null
    ) {
        stop()

        currentVoiceType = voiceType
        isSpeaker = isSpeakerOn
        isPlaying = true

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        applyAudioRouting(audioManager, isSpeakerOn)

        when (voiceType) {
            "CUSTOM" -> {
                if (!customAudioUri.isNullOrEmpty()) {
                    playCustomAudio(context, customAudioUri, onPlaybackCompleted)
                } else {
                    stop()
                }
            }
            "OFFICE", "FAMILY", "FRIEND" -> {
                playScriptTts(context, voiceType, onPlaybackCompleted)
            }
            else -> {
                isPlaying = false
            }
        }
    }

    private fun playCustomAudio(
        context: Context,
        audioUriString: String,
        onPlaybackCompleted: (() -> Unit)?
    ) {
        try {
            val uri = Uri.parse(audioUriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                setOnCompletionListener {
                    this@FakeCallVoicePlayer.isPlaying = false
                    onPlaybackCompleted?.invoke()
                }
                start()
            }
        } catch (_: Exception) {
            this@FakeCallVoicePlayer.isPlaying = false
        }
    }

    private fun playScriptTts(
        context: Context,
        voiceType: String,
        onPlaybackCompleted: (() -> Unit)?
    ) {
        val script = getScript(voiceType)
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    tts.language = Locale.getDefault()
                    tts.setPitch(1.0f)
                    tts.setSpeechRate(1.0f)

                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isPlaying = true
                        }

                        override fun onDone(utteranceId: String?) {
                            isPlaying = false
                            onPlaybackCompleted?.invoke()
                        }

                        override fun onError(utteranceId: String?) {
                            isPlaying = false
                        }
                    })

                    val params = Bundle().apply {
                        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
                    }

                    tts.speak(script, TextToSpeech.QUEUE_FLUSH, params, "FAKE_CALL_SPEECH")
                }
            } else {
                isPlaying = false
            }
        }
    }

    fun setSpeaker(context: Context, speakerOn: Boolean) {
        isSpeaker = speakerOn
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        applyAudioRouting(audioManager, speakerOn)
    }

    fun setMute(muted: Boolean) {
        isMuted = muted
        mediaPlayer?.let {
            if (muted) {
                it.setVolume(0f, 0f)
            } else {
                it.setVolume(1f, 1f)
            }
        }
        if (muted) {
            textToSpeech?.stop()
        }
    }

    private fun applyAudioRouting(audioManager: AudioManager?, speakerOn: Boolean) {
        if (audioManager == null) return
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = speakerOn
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null

        isPlaying = false
    }

    fun getScript(voiceType: String): String {
        return when (voiceType) {
            "OFFICE" -> "Hey, sorry to disturb you, but we have a critical situation at the office right now. We really need you to step out and join the conference line immediately. Can you excuse yourself and call me back right away?"
            "FAMILY" -> "Hey! I am so glad I caught you. Something just came up at home and we need your help right now. Could you please wrap things up and head over as soon as possible?"
            "FRIEND" -> "Hey, are you still stuck there? I figured you might need an escape excuse. Just nod, pretend this is urgent, and get out of there. Call me back once you are free!"
            else -> ""
        }
    }
}
