package com.raywdude.waterresistance

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Result haptics.
 *
 * The in-flow feedback (pad press, timer ticks, threshold) goes through Compose's
 * [androidx.compose.ui.hapticfeedback.HapticFeedback], which respects the system haptics setting
 * and needs no permission. A finished test deserves more than a single click, though, so this
 * plays a short composed pattern: a rising double-click when the seals hold, and two low thuds
 * when they do not. On older releases where composed primitives are unavailable it falls back to
 * a plain waveform.
 */
object SealHaptics {

    /** Plays the verdict pattern once. Safe to call on devices without a vibrator. */
    fun result(context: Context, passed: Boolean) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator() || !vibrator.hasAmplitudeControl()) {
            // Devices without amplitude control still get the rhythm, just not the dynamics.
            runCatching {
                vibrator.vibrate(
                    if (passed) longArrayOf(0, 24, 40, 52) else longArrayOf(0, 70, 55, 70),
                    -1,
                )
            }
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val effect = if (passed) {
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 55)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 95)
                        .compose()
                } else {
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.7f, 130)
                        .compose()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    vibrator.vibrate(
                        effect,
                        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH),
                    )
                } else {
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    if (passed) longArrayOf(0, 24, 40, 52) else longArrayOf(0, 70, 55, 70),
                    -1,
                )
            }
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
