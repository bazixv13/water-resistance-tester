package com.raywdude.waterresistance

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * State tick interval. 8 ms keeps the progress bar and the live trace advancing on every display
 * frame of a 120 Hz panel instead of stepping at 30 Hz.
 */
private const val FRAME_INTERVAL_MS = 8L

class TestViewModel(application: Application) : AndroidViewModel(application) {

    private val sensor = PressureSensor(application)
    private val historyStore = HistoryStore(application)

    private val _history = MutableStateFlow(historyStore.load())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _state = MutableStateFlow(
        TestUiState(phase = if (sensor.isAvailable) TestPhase.Ready else TestPhase.Unsupported)
    )
    val state: StateFlow<TestUiState> = _state.asStateFlow()

    /**
     * The finished test, frozen. The result screen reads from this instead of the live state, so
     * leaving the screen can never flash the "not intact" copy on the way out.
     */
    private val _lastResult = MutableStateFlow<TestUiState?>(null)
    val lastResult: StateFlow<TestUiState?> = _lastResult.asStateFlow()

    private var testJob: Job? = null

    /** Fingers on the pads drive the press clock: time only counts while both are down. */
    fun onFingersChanged(count: Int) {
        if (_state.value.fingers != count) {
            _state.update { it.copy(fingers = count) }
        }
    }

    fun startTest() {
        if (!sensor.isAvailable) return
        testJob?.cancel()

        testJob = viewModelScope.launch {
            var phase = TestPhase.Calibrating
            val calibration = ArrayList<Float>(256)
            val smoothing = ArrayDeque<Float>()
            val trace = ArrayDeque<TraceSample>()
            var traceSnapshot: List<TraceSample> = emptyList()
            var average = 0f
            var stdDev = 0f
            var latest = 0f
            var smoothed = 0f
            var emaInitialised = false
            var peak = 0f
            val startedAt = SystemClock.elapsedRealtime()
            var lastTick = startedAt
            var touchTimeMs = 0L

            _state.value = TestUiState(phase = TestPhase.Calibrating)

            // Readings only feed the maths; the ticker below owns the clock and the UI state, so
            // a quiet barometer can never stall the countdown.
            val reader = launch {
                sensor.readings().collect { pressure ->
                    latest = pressure
                    when (phase) {
                        TestPhase.Calibrating -> {
                            calibration += pressure
                            average = calibration.average().toFloat()
                            stdDev = standardDeviation(calibration, average)
                        }

                        TestPhase.Pressing -> {
                            // Two-stage smoothing:
                            //  1. a short moving average kills single-sample spikes;
                            //  2. an exponential moving average on top of that removes the
                            //     residual staircase the moving average leaves behind, which is
                            //     what made the trace look steppy at the barometer's low rate.
                            smoothing.addLast(pressure - average)
                            while (smoothing.size > TestThresholds.SMOOTHING_WINDOW) {
                                smoothing.removeFirst()
                            }
                            val windowed = smoothing.average().toFloat()
                            smoothed = if (emaInitialised) {
                                smoothed + TestThresholds.EMA_ALPHA * (windowed - smoothed)
                            } else {
                                emaInitialised = true
                                windowed
                            }
                            peak = max(peak, smoothed)
                            // Stamped on System.nanoTime, the same clock withFrameNanos reports,
                            // so the graph can place samples against the live frame edge.
                            // The barometer delivers ~20 readings a second. Record the
                            // segment between the previous reading and this one as ~8 ms
                            // steps, so the trace itself is display-rate: the dot then moves
                            // on every frame without the graph having to extrapolate.
                            val nowMs = System.nanoTime() / 1_000_000
                            val last = trace.lastOrNull()
                            if (last != null && nowMs > last.timeMs) {
                                val span = nowMs - last.timeMs
                                val steps = maxOf(1, (span / 8).toInt())
                                for (i in 1..steps) {
                                    val f = i.toFloat() / steps
                                    trace.addLast(
                                        TraceSample(
                                            timeMs = last.timeMs + span * i / steps,
                                            value = last.value + (smoothed - last.value) * f,
                                        )
                                    )
                                }
                            } else {
                                trace.addLast(TraceSample(nowMs, smoothed))
                            }
                            while (trace.size > TestThresholds.HISTORY_SIZE) trace.removeFirst()
                            // Smooth ONCE here, at write time, over the whole buffered trace.
                            //
                            // Doing this in the composable instead (over only the visible subset)
                            // made history visibly deform: the subset changes as samples scroll
                            // in and out of the window, so the same historical point was filtered
                            // differently on every frame and the curve appeared to breathe.
                            // Smoothing the full buffer once means each sample's plotted value is
                            // fixed the moment it is recorded and never changes again.
                            traceSnapshot = smoothTrace(trace)
                        }

                        else -> Unit
                    }
                }
            }

            while (phase != TestPhase.Result) {
                val now = SystemClock.elapsedRealtime()
                val frameMs = now - lastTick
                lastTick = now

                if (phase == TestPhase.Calibrating) {
                    val elapsed = now - startedAt
                    _state.update {
                        it.copy(
                            phase = TestPhase.Calibrating,
                            currentPressure = latest,
                            baselineAverage = average,
                            baselineStdDev = stdDev,
                            calibrationProgress =
                                (elapsed.toFloat() / TestThresholds.CALIBRATION_DURATION_MS)
                                    .coerceIn(0f, 1f),
                        )
                    }
                    if (elapsed >= TestThresholds.CALIBRATION_DURATION_MS) {
                        phase = TestPhase.Pressing
                        touchTimeMs = 0L
                    }
                } else {
                    // Count only the time both thumbs are actually on their pads, so lifting a
                    // thumb pauses the test instead of eating into the six seconds.
                    if (_state.value.fingers >= 2) {
                        touchTimeMs += frameMs
                    }
                    val remaining =
                        (TestThresholds.PRESS_DURATION_MS - touchTimeMs).coerceAtLeast(0L)
                    val threshold = TestThresholds.passThreshold(stdDev)
                    val passed = peak >= threshold
                    // Finish the moment the target is crossed; no need to hold for the rest of
                    // the window once the verdict cannot change.
                    val done = passed || remaining == 0L

                    val next = _state.value.copy(
                        phase = if (done) TestPhase.Result else TestPhase.Pressing,
                        currentPressure = latest,
                        baselineAverage = average,
                        baselineStdDev = stdDev,
                        calibrationProgress = 1f,
                        pressProgress =
                            (touchTimeMs.toFloat() / TestThresholds.PRESS_DURATION_MS)
                                .coerceIn(0f, 1f),
                        secondsRemaining = ceil(remaining / 1000f).toInt(),
                        delta = smoothed,
                        peakDelta = peak,
                        history = traceSnapshot,
                        passed = passed,
                        baselineWasNoisy = stdDev > TestThresholds.NOISY_BASELINE_HPA,
                    )
                    _state.value = next

                    if (done) {
                        phase = TestPhase.Result
                        // Freeze the verdict before anything can reset the live state.
                        _lastResult.value = next.copy(phase = TestPhase.Result)
                        historyStore.add(
                            HistoryEntry(
                                epochMillis = System.currentTimeMillis(),
                                peakDelta = peak,
                                passThreshold = threshold,
                                passed = passed,
                            )
                        )
                        _history.value = historyStore.load()
                    }
                }

                delay(FRAME_INTERVAL_MS)
            }

            reader.cancel()
        }
    }

    fun clearHistory() {
        historyStore.clear()
        _history.value = historyStore.load()
    }

    fun removeHistory(entry: HistoryEntry) {
        historyStore.remove(entry)
        _history.value = historyStore.load()
    }

    /** Back to the start screen. The last result is kept so the exit transition stays truthful. */
    fun reset() {
        testJob?.cancel()
        testJob = null
        _state.value = TestUiState(
            phase = if (sensor.isAvailable) TestPhase.Ready else TestPhase.Unsupported
        )
    }

    override fun onCleared() {
        testJob?.cancel()
        super.onCleared()
    }
}

private fun standardDeviation(values: List<Float>, mean: Float): Float {
    if (values.size < 2) return 0f
    var sum = 0.0
    for (value in values) {
        val distance = (value - mean).toDouble()
        sum += distance * distance
    }
    return sqrt(sum / values.size).toFloat()
}

/**
 * Gaussian smoothing over the whole trace buffer, applied once per new sample.
 *
 * This is display-only: [TestUiState.peakDelta], the pass threshold and the verdict all come from
 * the unsmoothed `smoothed`/`peak` values, so the measurement is untouched. Only the drawn shape is
 * smoothed.
 *
 * Because it runs over the entire buffer rather than the visible slice, a sample's smoothed value
 * is stable for the rest of the test: the curve cannot deform as it scrolls.
 */
private fun smoothTrace(samples: ArrayDeque<TraceSample>): List<TraceSample> {
    if (samples.size < 3) return samples.toList()

    val kernel = GAUSSIAN_KERNEL
    val radius = kernel.size / 2
    val n = samples.size
    val values = FloatArray(n) { samples[it].value }

    // ONE pass of a sigma-1.0 Gaussian.
    //
    // Two passes of sigma 1.5 was over-filtering: it removed the jitter but also rounded off real
    // structure in the curve, so the drawn shape no longer matched what the barometer actually
    // measured. One lighter pass takes the edge off the noise and leaves the genuine shape alone;
    // the column resampling in the graph handles the rest of the visual smoothing.
    var current = values
    repeat(1) {
        val out = FloatArray(n)
        for (i in 0 until n) {
            var acc = 0f
            for (k in kernel.indices) {
                val idx = (i + k - radius).coerceIn(0, n - 1)
                acc += current[idx] * kernel[k]
            }
            out[i] = acc
        }
        current = out
    }

    return List(n) { i -> TraceSample(samples[i].timeMs, current[i]) }
}

/** Normalised 1D Gaussian, sigma 1.0, radius 3. */
private val GAUSSIAN_KERNEL: FloatArray = run {
    val sigma = 1.0f
    val radius = 3
    val raw = FloatArray(radius * 2 + 1) { i ->
        val d = (i - radius).toFloat()
        kotlin.math.exp(-(d * d) / (2f * sigma * sigma))
    }
    val sum = raw.sum()
    FloatArray(raw.size) { raw[it] / sum }
}
