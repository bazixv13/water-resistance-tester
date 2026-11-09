package com.raywdude.waterresistance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.raywdude.waterresistance.ui.PressingScreen
import com.raywdude.waterresistance.ui.ResultScreen
import com.raywdude.waterresistance.ui.theme.SealTestTheme
import kotlin.math.sin

/**
 * Debug-only renderer for screens that are otherwise hard to reach from adb.
 *
 * A real result needs a two-finger press, which adb cannot inject, so this activity feeds
 * [ResultScreen] and [PressingScreen] hand-built state. It lives in the debug source set and is
 * never part of a release build.
 */
class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mode = intent.getStringExtra("mode") ?: "result-pass"

        setContent {
            SealTestTheme(dynamicColor = true) {
                Scaffold(containerColor = MaterialTheme.colorScheme.surface) { _ ->
                    when (mode) {
                        "press" -> PressingScreen(
                            state = demoPressing(),
                            onFingersChanged = {},
                        )

                        "result-fail", "result-long" -> ResultScreen(
                            state = demoResult(
                                passed = false,
                                noisy = mode == "result-long",
                            ),
                            onRestart = {},
                            modifier = Modifier.fillMaxSize(),
                        )

                        else -> ResultScreen(
                            state = demoResult(passed = true),
                            onRestart = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private fun demoPressing(): TestUiState {
        val peak = 1.08f
        return TestUiState(
            phase = TestPhase.Pressing,
            currentPressure = 1009.31f,
            baselineAverage = 1008.21f,
            baselineStdDev = 0.14f,
            calibrationProgress = 1f,
            pressProgress = 0.55f,
            secondsRemaining = 3,
            delta = 1.01f,
            peakDelta = peak,
            fingers = 2,
            history = demoHistory(peak),
        )
    }

    private fun demoResult(passed: Boolean, noisy: Boolean = false): TestUiState {
        val peak = if (passed) 1.82f else 0.42f
        return TestUiState(
            phase = TestPhase.Result,
            currentPressure = 1009.02f,
            baselineAverage = 1008.24f,
            baselineStdDev = 0.18f,
            calibrationProgress = 1f,
            pressProgress = 1f,
            secondsRemaining = 0,
            delta = peak,
            peakDelta = peak,
            passed = passed,
            baselineWasNoisy = noisy,
            history = demoHistory(peak),
        )
    }

    /** A plausible rising trace ending at [peak], stamped on the graph's monotonic clock. */
    private fun demoHistory(peak: Float): List<TraceSample> {
        val now = System.nanoTime() / 1_000_000
        val count = 620
        val raw = List(count) { i ->
            val t = i / (count - 1f)
            val rise = peak * (1f - (1f - t) * (1f - t))
            val wobble = sin(i * 0.045f) * 0.012f
            rise + wobble
        }
        // Mimic the ViewModel's write-time smoothing, so the demo graph shows what the app
        // actually draws rather than raw high-frequency sensor jitter.
        val smoothed = List(count) { i ->
            val from = (i - 3).coerceAtLeast(0)
            val to = (i + 3).coerceAtMost(count - 1)
            var sum = 0f
            for (j in from..to) sum += raw[j]
            sum / (to - from + 1)
        }
        return List(count) { i ->
            TraceSample(timeMs = now - ((count - 1 - i) * 8L), value = smoothed[i])
        }
    }
}
