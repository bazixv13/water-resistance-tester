package com.raywdude.waterresistance.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raywdude.waterresistance.BuildConfig
import com.raywdude.waterresistance.HistoryEntry
import com.raywdude.waterresistance.HistoryStore
import com.raywdude.waterresistance.R
import com.raywdude.waterresistance.SealHaptics
import com.raywdude.waterresistance.TestThresholds
import com.raywdude.waterresistance.TestUiState
import com.raywdude.waterresistance.ui.theme.LocalSealAccents
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt


/* ------------------------------------------------------------- landing */

/**
 * Fraction of the press window after which the copy switches to "press harder".
 * 0.5 of a six-second window is the three-second mark.
 */
private const val PRESS_HARDER_AFTER = 0.5f

/**
 * Build identity injected by Gradle/CI. Shown on the start screen and in the history sheet so a
 * sideloaded APK can always be traced back to the commit that produced it.
 */
private val versionLabel: String
    get() = buildString {
        append("v").append(BuildConfig.VERSION_NAME)
        append(" (").append(BuildConfig.VERSION_CODE).append(")")
        if (BuildConfig.BUILD_CHANNEL == "nightly") append(" · nightly")
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun ReadyScreen(
    deviceLabel: String,
    osLabel: String,
    hazeState: HazeState,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Clearance for the floating action pill, which sits over the scroll content.
            Spacer(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(48.dp)
            )

            // The title is body text, not a top bar: it scrolls away with the content.
            // Rendered as one block so the two lines keep tight, even leading. It fades and lifts
            // in on first composition, which gives the screen an entrance instead of just existing.
            val titleIn = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                titleIn.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append("Water Resistance")
                    }
                    append("\n")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Tester")
                    }
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp,
                    letterSpacing = (-0.25).sp,
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = titleIn.value
                    translationY = (1f - titleIn.value) * 24.dp.toPx()
                },
            )

            Entrance(delayMillis = 60) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box {
                        // A diagonal sheen in the primary colour, so the card is not one flat fill.
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.00f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        )
                                    )
                                ),
                        )
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "Know your seals before the water does.",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Press two fingers on the display. If the seals still " +
                                    "hold, the air inside compresses and the barometer climbs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.82f
                                ),
                            )
                        }
                    }
                }
            }

            Entrance(delayMillis = 120) {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DEVICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = deviceLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ANDROID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                                text = osLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Entrance(delayMillis = 160) {
                SectionHeading(text = "How it works", modifier = Modifier.padding(top = 4.dp))
            }
            Entrance(delayMillis = 200) {
                StepRow(
                    index = 1,
                    title = "Hold still",
                    body = "The ambient pressure is sampled for a few seconds to build a baseline.",
                )
            }
            Entrance(delayMillis = 240) {
                StepRow(
                    index = 2,
                    title = "Press the display",
                    body = "Rest a thumb on each pad at the bottom of the screen and press down " +
                        "firmly. The six-second timer runs only while both thumbs stay down; " +
                        "intact seals push the inside pressure up.",
                )
            }
            Entrance(delayMillis = 280) {
                StepRow(
                    index = 3,
                    title = "Read the result",
                    body = "A peak rise of at least 1.5 hPa means the seals are doing their job.",
                )
            }

            Entrance(delayMillis = 320) {
                Text(
                    text = "Original app by Raymond Wang · Copyright © 2025",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }

            Entrance(delayMillis = 360) {
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Floating blurred pill: the only thing pinned over the scrolling content.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp, end = 16.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .hazeEffect(
                    // Bare style with no tint: the pill is mostly blur, so whatever scrolls
                    // underneath stays visible through it.
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        tints = emptyList(),
                        blurRadius = 6.dp,
                        noiseFactor = 0f,
                    ),
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.20f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onHistory) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = "Test history",
                )
            }
            IconButton(onClick = onHelp) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = "Help and FAQ",
                )
            }
        }

        // Start button glued to the bottom edge, always reachable without scrolling. The scrim
        // fades upward so content dissolves into it instead of being cut off by a hard edge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(top = 64.dp, bottom = 12.dp),
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Start the test",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/* --------------------------------------------------------- calibrating */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalibratingScreen(state: TestUiState) {
    val scheme = MaterialTheme.colorScheme
    val ringStroke = with(androidx.compose.ui.platform.LocalDensity.current) {
        Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.26f),
                        scheme.surface,
                        scheme.surface,
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.weight(0.8f))

        // The morphing seal stays the hero; the wavy ring around it is the progress. Two
        // expressive elements reinforcing the same idea, instead of a generic spinner.
        val easedCalibration = state.calibrationProgress * state.calibrationProgress
        Box(contentAlignment = Alignment.Center) {
            MorphingSealHero(
                passed = false,
                continuousMorph = true,
                modifier = Modifier.size(150.dp),
            )
            CircularWavyProgressIndicator(
                progress = { easedCalibration },
                modifier = Modifier.size(176.dp),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
                stroke = ringStroke,
                trackStroke = ringStroke,
                gapSize = 4.dp,
                amplitude = { 1f - easedCalibration * 0.35f },
            )
        }

        Text(
            text = "${(state.calibrationProgress * 100).roundToInt()}% of the baseline captured",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.primary,
        )

        Text(
            text = "Hold still",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Sampling the ambient pressure. Keep the phone away from vents and drafts.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        SectionCard(color = scheme.surfaceContainerHigh.copy(alpha = 0.75f)) {
            MetricRow(label = "Avg. pressure", value = formatPressure(state.baselineAverage))
            Spacer(Modifier.height(12.dp))
            MetricRow(label = "Std. deviation", value = formatPressure(state.baselineStdDev))
            Spacer(Modifier.height(12.dp))
            MetricRow(label = "Current", value = formatPressure(state.currentPressure))
        }

        Spacer(Modifier.weight(1.2f))
    }
}

/* ------------------------------------------------------------ pressing */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PressingScreen(
    state: TestUiState,
    onFingersChanged: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val thresholdReached = state.peakDelta >= state.passThreshold
    var leftDown by remember { mutableStateOf(false) }
    var rightDown by remember { mutableStateOf(false) }
    val fingers = (if (leftDown) 1 else 0) + (if (rightDown) 1 else 0)

    // Once the test has been running for a while without clearing the target, nudge for more force.
    val pressHarder =
        fingers == 2 && !thresholdReached && state.pressProgress >= PRESS_HARDER_AFTER

    LaunchedEffect(fingers) { onFingersChanged(fingers) }
    DisposableEffect(Unit) {
        onDispose { onFingersChanged(0) }
    }

    // The threshold is the moment worth feeling: it is stronger than the pad ticks and lands
    // before the result screen takes over.
    LaunchedEffect(thresholdReached) {
        if (thresholdReached) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }
    LaunchedEffect(pressHarder) {
        if (pressHarder) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    // A quiet tick each second the timer actually runs, so the countdown is felt, not only read.
    LaunchedEffect(state.secondsRemaining, fingers, thresholdReached) {
        if (fingers == 2 && !thresholdReached) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.22f),
                        scheme.surface,
                        scheme.surface,
                    )
                )
            )
    ) {
        // The thumb pads occupy the bottom fifth of the screen, where thumbs naturally rest.
        val padHeight = (maxHeight * 0.18f).coerceIn(104.dp, 168.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Fixed-height copy block: the text swaps between states without reflowing the
            // layout, so the graph and pads never jump while the test runs.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.height(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (pressHarder) {
                            "Press harder"
                        } else if (fingers == 2) {
                            "Keep pressing"
                        } else {
                            "Press hard with both thumbs"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (pressHarder) {
                            scheme.error
                        } else {
                            scheme.onSurface
                        },
                    )
                }
                Box(
                    modifier = Modifier.height(56.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        text = if (pressHarder) {
                            "The target is not reached yet. Try to press harder on both pads."
                        } else {
                            "Rest a thumb on each pad at the bottom of the screen and press " +
                                "down hard. The timer only advances while both thumbs stay down."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Live value, target and timer: the numbers the user is actually acting on.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatSigned(state.delta),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (thresholdReached) {
                                LocalSealAccents.current.success
                            } else {
                                scheme.onSurface
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "hPa rise",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "Target %+.1f hPa",
                            state.passThreshold,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.tertiary,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (fingers == 2) {
                        scheme.primaryContainer
                    } else {
                        scheme.surfaceContainerHigh
                    },
                    contentColor = if (fingers == 2) {
                        scheme.onPrimaryContainer
                    } else {
                        scheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        text = if (fingers == 2) {
                            "${state.secondsRemaining}s left"
                        } else {
                            "Paused · ${state.secondsRemaining}s"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // The wave amplitude collapses as the press progresses, so the indicator visually
            // settles as you approach the target instead of waving at a constant intensity.
            LinearWavyProgressIndicator(
                progress = { state.pressProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                gapSize = 0.dp,
                amplitude = { 1f - state.pressProgress * 0.6f },
            )

            Spacer(Modifier.height(14.dp))

            // The graph is the centrepiece now: it takes every pixel the rest of the layout does
            // not need, instead of sitting at a fixed 160dp above a pool of empty space.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                color = scheme.surfaceContainerLow,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.32f)),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(14.dp))
                    PressureSparkline(
                        history = state.history,
                        threshold = state.passThreshold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (fingers == 2) {
                    "Both thumbs down — hold until the target or the timer runs out."
                } else {
                    "Place a thumb on each pad; lifting one pauses the timer."
                },
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )

            // Last row in the column, so the pads sit flush with the bottom of the screen.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ThumbPad(
                    side = "Left thumb",
                    pressed = leftDown,
                    onPressedChange = { leftDown = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(padHeight),
                )
                ThumbPad(
                    side = "Right thumb",
                    pressed = rightDown,
                    onPressedChange = { rightDown = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(padHeight),
                )
            }
        }
    }
}

/* -------------------------------------------------------------- result */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ResultScreen(
    state: TestUiState,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accents = LocalSealAccents.current
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val accent = if (state.passed) accents.success else scheme.error

    // One verdict pattern per result, not on every recomposition or rotation.
    var buzzed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!buzzed) {
            buzzed = true
            SealHaptics.result(context, state.passed)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        scheme.surface,
                        scheme.surface,
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MorphingSealHero(
                passed = state.passed,
                continuousMorph = false,
                heroSize = 168.dp,
            )

            Text(
                text = if (state.passed) "Seals intact" else "Seals compromised",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (state.passed) {
                    "The air inside still compresses, so the seals are holding."
                } else {
                    "The barometer barely moved. Have the seals checked before any contact " +
                        "with water."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            SectionCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MetricTile(
                        label = "Peak rise",
                        value = formatSigned(state.peakDelta),
                        unit = "hPa",
                        modifier = Modifier.weight(1f),
                        valueColor = accent,
                    )
                    MetricTile(
                        label = "Baseline",
                        value = formatPressure(state.baselineAverage),
                        unit = "hPa",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MetricTile(
                        label = "Pass mark",
                        value = String.format(Locale.US, "%.1f", state.passThreshold),
                        unit = "hPa",
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        label = "Noise",
                        value = formatPressure(state.baselineStdDev),
                        unit = "hPa",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.baselineWasNoisy) {
                CautionNote(
                    text = "This reading was taken while the air pressure was unstable. Re-run " +
                        "the test in still air for a firmer answer.",
                )
            }

            SectionCard(color = accent.copy(alpha = 0.12f)) {
                SectionHeading(text = "What now?")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.passed) {
                        "No action needed. Re-run after a drop, repair, or screen replacement."
                    } else {
                        "Keep it away from water and have the seals checked. Re-run to confirm."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }

            Text(
                text = "An indicator, not a warranty. Seals weaken with drops and repairs.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }

            // Fade the bottom edge while there is more to scroll: the content dimming out reads
            // as "continues below" without adding another control.
            if (scrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surface,
                                )
                            )
                        ),
                )
            }
        }

        // The single next action, glued to the bottom edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.surface.copy(alpha = 0f),
                            scheme.surface.copy(alpha = 0.94f),
                            scheme.surface,
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(top = 36.dp, bottom = 12.dp),
        ) {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(text = "Test again", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}


/* --------------------------------------------------------- unsupported */

@Composable
fun UnsupportedScreen(onSupport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(120.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
        Text(
            text = "No barometer on this phone",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "This test needs the pressure sensor most flagship phones carry. Without one " +
                "there is no way to measure the air trapped behind the seals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onSupport) {
            Text(text = stringResource(R.string.support_developer))
        }
        Spacer(Modifier.weight(1f))
    }
}

/* ----------------------------------------------------------- fragments */

@Composable
private fun MetricRow(label: String, value: String, unit: String = "hPa") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CautionNote(text: String) {
    val accents = LocalSealAccents.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = accents.cautionContainer,
        contentColor = accents.onCautionContainer,
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/* ------------------------------------------------------------- help FAQ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheetContent(onSupport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        StepRow(
            index = 1,
            title = "Hold still",
            body = "The ambient pressure is sampled for a few seconds to build a baseline.",
        )
        StepRow(
            index = 2,
            title = "Press with both thumbs",
            body = "Rest a thumb on each pad at the bottom of the screen. The six-second timer " +
                "runs only while both thumbs are down, so lifting one pauses the test.",
        )
        StepRow(
            index = 3,
            title = "Read the result",
            body = String.format(
                Locale.US,
                "A peak rise of %.1f hPa or more means the seals are doing their job.",
                TestThresholds.PASS_PRESSURE_RISE_HPA,
            ),
        )

        HorizontalDivider()

        Text(
            text = "Questions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        HelpEntry(
            question = "How does this test work?",
            answer = "Every IP67/IP68 phone contains a sealed pocket of air. Pressing the " +
                "display compresses that air slightly, and the barometer inside reads the rise. " +
                "Seals that have failed let the air escape, so the reading barely moves.",
        )
        HelpEntry(
            question = "What counts as a pass?",
            answer = "A peak rise of 1.5 hPa or more, measured on a smoothed running average so " +
                "a single spike cannot pass on its own. If the baseline was restless, the pass " +
                "mark is raised to three times the baseline noise.",
        )
        HelpEntry(
            question = "Nothing happens when I press.",
            answer = "Use one thumb on each of the two pads at the bottom of the screen. The " +
                "timer stays paused until both are down, and the test ends early once the target " +
                "is reached. Remove thick screen protectors and press firmly.",
        )
        HelpEntry(
            question = "Why would a seal fail?",
            answer = "Drops that crack the frame or back glass, a repair shop that reused a " +
                "gasket, a loose SIM tray, ageing adhesive, or a device that never had an IP " +
                "rating in the first place.",
        )
        HelpEntry(
            question = "Is my phone at risk?",
            answer = "No water is involved. Barometers are passive sensors and pressing a phone " +
                "by hand is well within normal handling. Never expose the device to liquid based " +
                "on this result.",
        )
        HelpEntry(
            question = "Where does this app come from?",
            answer = "A Material 3 Expressive remake of Raymond Wang's Water Resistance Tester. " +
                "The original author states that the methodology is the same one Samsung service " +
                "technicians use; this remake follows that method and is not affiliated with Samsung.",
        )

        HorizontalDivider()
        TextButton(onClick = onSupport, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.support_developer))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HelpEntry(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ------------------------------------------------------------- history */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheetContent(
    entries: List<HistoryEntry>,
    onClear: () -> Unit,
    onDelete: (HistoryEntry) -> Unit,
) {
    val accents = LocalSealAccents.current
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.getDefault())
    }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) {
                    Text(text = "Clear all")
                }
            }
        }
        Text(
            text = if (entries.isEmpty()) {
                "No tests yet. Run one and the result shows up here."
            } else {
                "Most recent first, kept on this device. Swipe a result left to delete it."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entries.forEach { entry ->
            key(entry.epochMillis) {
                val dismissState = rememberSwipeToDismissBoxState()
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "Delete result",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    modifier = Modifier.clip(MaterialTheme.shapes.large),
                    enableDismissFromStartToEnd = false,
                    onDismiss = { direction ->
                        if (direction == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(entry)
                        }
                    },
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = if (entry.passed) {
                            accents.successContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = if (entry.passed) {
                            accents.onSuccessContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (entry.passed) R.drawable.ic_check else R.drawable.ic_close
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (entry.passed) {
                                        "Seals intact"
                                    } else {
                                        "Seals compromised"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = formatter.format(
                                        Instant.ofEpochMilli(entry.epochMillis)
                                            .atZone(ZoneId.systemDefault())
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%+.2f hPa", entry.peakDelta),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (entries.isNotEmpty()) {
                Text(
                    text = "Older entries fall off after ${HistoryStore.MAX_ENTRIES} tests.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = versionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(text = "Clear test history?") },
            text = {
                Text(text = "This removes all ${entries.size} saved results from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text(text = "Clear all")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}
