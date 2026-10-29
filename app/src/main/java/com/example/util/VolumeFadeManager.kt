package com.example.util

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object VolumeFadeManager {
    private const val TAG = "VolumeFadeManager"

    // Stores original volumes before fading
    private val originalVolumes = mutableMapOf<Int, Int>()

    fun startFadeOut(context: Context, durationMinutes: Int = 3, onComplete: () -> Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: run {
            onComplete()
            return
        }

        // Save original volumes
        val streams = listOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION
        )
        for (stream in streams) {
            originalVolumes[stream] = audioManager.getStreamVolume(stream)
        }

        val totalMillis = (durationMinutes * 60 * 1000L).coerceAtLeast(10_000L)
        val steps = 10
        val intervalMillis = totalMillis / steps

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (step in steps downTo 0) {
                    val fraction = step.toFloat() / steps.toFloat()
                    for (stream in streams) {
                        val maxVol = audioManager.getStreamMaxVolume(stream)
                        val orig = originalVolumes[stream] ?: maxVol
                        val targetVol = (orig * fraction).toInt()
                        try {
                            audioManager.setStreamVolume(stream, targetVol, 0)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error setting volume for stream $stream: ${e.message}")
                        }
                    }
                    if (step > 0) {
                        delay(intervalMillis)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during volume fade out", e)
            } finally {
                onComplete()
            }
        }
    }

    fun restoreVolumes(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            for ((stream, vol) in originalVolumes) {
                audioManager.setStreamVolume(stream, vol, 0)
            }
            Log.d(TAG, "Restored original audio volumes")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring volumes", e)
        }
    }
}
