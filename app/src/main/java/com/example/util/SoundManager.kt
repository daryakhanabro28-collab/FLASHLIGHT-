package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

enum class SoundType {
    CLICK,
    UNLOCK,
    LOCK,
    TORCH,
    ERROR,
    SECRET_TRIGGER
}

class SoundManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing ToneGenerator", e)
        }
    }

    fun play(type: SoundType, soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (hapticEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (type) {
                        SoundType.CLICK -> vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                        SoundType.UNLOCK -> vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 60), -1))
                        SoundType.LOCK -> vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                        SoundType.TORCH -> vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                        SoundType.ERROR -> vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 40, 50), -1))
                        SoundType.SECRET_TRIGGER -> vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40, 50, 60), -1))
                    }
                }
            } catch (e: Exception) {
                // Ignore haptic errors on emulators
            }
        }

        if (soundEnabled) {
            try {
                when (type) {
                    SoundType.CLICK -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
                    SoundType.UNLOCK -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                    }
                    SoundType.LOCK -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 80)
                    SoundType.TORCH -> toneGenerator?.startTone(ToneGenerator.TONE_SUP_PIP, 40)
                    SoundType.ERROR -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                    SoundType.SECRET_TRIGGER -> toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 180)
                }
            } catch (e: Exception) {
                // Fallback to audio manager click
                try {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    am?.playSoundEffect(AudioManager.FX_KEY_CLICK)
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
