package com.raywdude.waterresistance

import kotlin.math.max

/** Which screen the flow is on. */
enum class TestPhase { Unsupported, Ready, Calibrating, Pressing, Result }

object TestThresholds {
    /** How long the ambient baseline is sampled. */
    const val CALIBRATION_DURATION_MS = 3_500L

    /** How long the display is pressed; every reading in the window counts. */
    const val PRESS_DURATION_MS = 6_000L

    /** Pressure rise that counts as sealed, before the baseline noise floor is taken into account. */
    const val PASS_PRESSURE_RISE_HPA = 1.5f

    /** Baseline fluctuation above which the environment is considered too drafty to trust. */
    const val NOISY_BASELINE_HPA = 0.6f

    /** How many readings are averaged together to reject single-sample spikes. */
    const val SMOOTHING_WINDOW = 5

    /**
     * Weight of each new reading in the exponential moving average applied on top of the moving
     * average. Lower is smoother but laggier; 0.35 keeps the trace fluid without visibly trailing
     * the real pressure.
     */
    const val EMA_ALPHA = 0.35f

    /** Enough points for a full six-second graph window at up to 200 Hz. */
    const val HISTORY_SIZE = 1200

    /** A rise has to clear the baseline noise by this many standard deviations to count. */
    const val NOISE_FLOOR_SIGMA = 3f

    /**
     * Effective pass mark: the fixed threshold, raised when the baseline itself was restless so
     * a shaky room cannot fake a pass.
     */
    fun passThreshold(baselineStdDev: Float?): Float =
        max(PASS_PRESSURE_RISE_HPA, (baselineStdDev ?: 0f) * NOISE_FLOOR_SIGMA)
}

/** One smoothed pressure reading, stamped on the monotonic clock. */
data class TraceSample(
    val timeMs: Long,
    val value: Float,
)

data class TestUiState(
    val phase: TestPhase = TestPhase.Ready,
    val currentPressure: Float? = null,
    val baselineAverage: Float? = null,
    val baselineStdDev: Float? = null,
    val calibrationProgress: Float = 0f,
    val pressProgress: Float = 0f,
    val secondsRemaining: Int = (TestThresholds.PRESS_DURATION_MS / 1000).toInt(),
    val delta: Float = 0f,
    val peakDelta: Float = 0f,
    /** How many fingers are currently down on the press pad. Purely visual feedback. */
    val fingers: Int = 0,
    val history: List<TraceSample> = emptyList(),
    val passed: Boolean = false,
    val baselineWasNoisy: Boolean = false,
) {
    val passThreshold: Float get() = TestThresholds.passThreshold(baselineStdDev)
}

/** A finished test, kept on the device. */
data class HistoryEntry(
    val epochMillis: Long,
    val peakDelta: Float,
    val passThreshold: Float,
    val passed: Boolean,
)
