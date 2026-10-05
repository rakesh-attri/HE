package com.example.ui.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundHelper {
  private const val TAG = "SoundHelper"

  /**
   * Plays a distinct, loud 3-second urgent emergency tone for high-priority society notices.
   */
  fun playUrgentAlertTone(durationMs: Int = 3000) {
    CoroutineScope(Dispatchers.Default).launch {
      try {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, durationMs)
        delay(durationMs.toLong() + 300L)
        toneGenerator.release()
      } catch (e: Exception) {
        Log.e(TAG, "Failed playing urgent alert tone: ${e.message}")
      }
    }
  }
}
