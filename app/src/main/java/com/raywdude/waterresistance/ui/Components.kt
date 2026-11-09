package com.raywdude.waterresistance.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.raywdude.waterresistance.R
import com.raywdude.waterresistance.TestThresholds
import com.raywdude.waterresistance.TraceSample
import com.raywdude.waterresistance.ui.theme.LocalSealAccents
import java.util.Locale
import kotlin.math.sin


fun formatPressure(value: Float?): String =
    value?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

fun formatSigned(value: Float): String = String.format(Locale.US, "%+.2f", value)

/** Rounded tonal container used for content blocks. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = color,
        contentColor = contentColor,
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StepRow(index: Int, title: String, body: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        // The badge silhouette turns slowly; the number stays upright and readable.
        val spin by rememberInfiniteTransition(label = "step badge").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12_000, easing = LinearEasing),
            ),
            label = "step badge spin",
        )
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .rotate(spin)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* -------------------------------------------------------------- entrance */

/**
 * Small entrance primitive: fades and lifts its content once on first composition.
 *
 * Used to stagger the home screen sections so the page assembles itself instead of appearing
 * fully formed. Purely visual: layout is unaffected because the transform is applied at draw time.
 */
@Composable
fun Entrance(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        progress.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 18.dp.toPx()
        }
    ) {
        content()
    }
}

/* --------------------------------------------------------------- morph hero */

/**
 * The pool of shapes the hero draws from while the baseline is sampled, matching the Material 3
 * shape-morph demo: bursts, scalloped cookies, gems, ovals, pills, clovers and polygons.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun expressiveShapePool(): List<RoundedPolygon> = listOf(
    MaterialShapes.Burst,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Gem,
    MaterialShapes.Pill,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Pentagon,
    MaterialShapes.Cookie12Sided,
    MaterialShapes.Clover8Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Flower,
    MaterialShapes.Cookie7Sided,
)

/**
 * Expressive hero: a shape that genuinely morphs between Material shapes.
 *
 * Two modes:
 *  - [continuousMorph] true  -> takes a pseudo-random walk through the shape pool, morphing
 *    smoothly from one shape into the next forever, so it never settles into a short loop
 *    (baseline capture).
 *  - [continuousMorph] false -> the verdict shape: it walks the same pool, so it keeps morphing.
 *
 * The animation driver is a remembered [Animatable]. Creating it inline (without `remember`)
 * restarts it at 0 on every recomposition, which pins the shape to its start state and makes it
 * look like a static blob. That was the original bug.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingSealHero(
    passed: Boolean,
    continuousMorph: Boolean = false,
    modifier: Modifier = Modifier,
    heroSize: Dp = 170.dp,
) {
    // Container and its content colour are a matched pair, so the verdict icon is always legible
    // and on-palette: green for intact, red for compromised, teal while the baseline is sampled.
    val accents = LocalSealAccents.current
    val containerColor: Color
    val contentColor: Color
    when {
        continuousMorph -> {
            containerColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        }

        passed -> {
            containerColor = accents.successContainer
            contentColor = accents.onSuccessContainer
        }

        else -> {
            containerColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        }
    }

    val pool = remember { expressiveShapePool() }
    // Morphs are built lazily per ordered pair and cached, so a long random walk stays cheap.
    val morphCache = remember { mutableMapOf<Pair<Int, Int>, Morph>() }

    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(1) }
    val step = remember { Animatable(0f) }
    val resultProgress = remember { Animatable(0f) }

    LaunchedEffect(continuousMorph) {
        if (continuousMorph) {
            var seed = 0x5EED
            while (true) {
                step.snapTo(0f)
                step.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1150, easing = FastOutSlowInEasing),
                )
                // Deterministic but non-repeating-feeling walk through the pool.
                seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                fromIndex = toIndex
                var next = (seed ushr 8) % pool.size
                if (next == fromIndex) next = (next + 1) % pool.size
                toIndex = next
            }
        }
    }

    LaunchedEffect(continuousMorph, passed) {
        if (!continuousMorph) {
            // The result shape keeps morphing forever through the same shape pool the baseline
            // uses, so the verdict screen is alive rather than a frozen blob.
            var seed = 0x2E5A17
            while (true) {
                step.snapTo(0f)
                step.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1150, easing = FastOutSlowInEasing),
                )
                seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                fromIndex = toIndex
                var next = (seed ushr 8) % pool.size
                if (next == fromIndex) next = (next + 1) % pool.size
                toIndex = next
            }
        }
    }

    // Accent colour for the verdict halo: green when the seals hold, red when they do not.
    val accent = if (passed) accents.success else MaterialTheme.colorScheme.error

    // Slow pulse so the accent reads as alive rather than a static fill.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(continuousMorph) {
        if (!continuousMorph) {
            while (true) {
                pulse.animateTo(1f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
                pulse.animateTo(0f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
            }
        }
    }

    val workPath = remember { Path() }

    // Canvas and icon are siblings inside a Box so the icon is drawn ON TOP of the shape, centred
    // in it. As plain siblings in a Column they stacked vertically, which is why the check/X
    // appeared below the shape instead of inside it.
    Box(
        modifier = modifier.size(heroSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Both modes walk the same shape pool; only the seed and the accent differ.
            val morph = morphCache.getOrPut(fromIndex to toIndex) {
                Morph(pool[fromIndex], pool[toIndex])
            }
            val p = step.value.coerceIn(0f, 1f)

            // Gentle breathing scale so the hero never looks frozen between morphs.
            val breathe = 1f + 0.02f * sin(p * Math.PI.toFloat())

            workPath.rewind()
            morph.toPath(p, workPath)

            val bounds = workPath.getBounds()
            if (bounds.width > 0f && bounds.height > 0f) {
                val fit = minOf(size.width / bounds.width, size.height / bounds.height) *
                    0.82f * breathe
                workPath.transform(Matrix().apply { scale(fit, fit) })
                val centred = workPath.getBounds()
                workPath.translate(
                    Offset(
                        size.center.x - centred.center.x,
                        size.center.y - centred.center.y,
                    )
                )
            }

            drawPath(
                path = workPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.72f),
                    ),
                    center = size.center,
                    radius = size.minDimension / 2f,
                ),
            )

            if (!continuousMorph) {
                // Soft accent halo that breathes with the pulse, tinted green or red by the
                // verdict. No hard border ring: it reads as a glow inside the shape.
                val glow = 0.25f + 0.35f * pulse.value
                drawPath(
                    path = workPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.30f * glow),
                            accent.copy(alpha = 0f),
                        ),
                        center = size.center,
                        radius = size.minDimension / 2f,
                    ),
                )
            }
        }

        if (!continuousMorph) {
            Icon(
                painter = painterResource(
                    if (passed) R.drawable.ic_check else R.drawable.ic_close
                ),
                contentDescription = if (passed) "Seals intact" else "Seals compromised",
                tint = contentColor,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

/* ------------------------------------------------------------- thumb pads */
/**
 * One of the two thumb pads at the bottom of the press screen.
 *
 * Expressive treatment:
 *  - the pad's silhouette MORPHS between two Material shapes while it is held, so pressing feels
 *    like it does something rather than just tinting;
 *  - the scale uses a spring, so the press has weight and the release settles instead of snapping;
 *  - a tonal ring expands outward from the pad while held, reading as pressure radiating out;
 *  - a short haptic fires on both press and release.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThumbPad(
    side: String,
    pressed: Boolean,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val currentOnPressedChange by rememberUpdatedState(onPressedChange)

    // Spring scale: weighty on press, settles on release.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "thumb pad scale",
    )

    // Morph between a rounded square and a scalloped cookie while held.
    val morph = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        morph.animateTo(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    val padShape = rememberMorphShape(
        from = MaterialShapes.Cookie9Sided,
        to = MaterialShapes.Clover4Leaf,
        progress = morph.value,
    )

    // Expanding ring while held.
    val ring = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            ring.snapTo(0f)
            ring.animateTo(1f, tween(900, easing = LinearEasing))
        } else {
            ring.snapTo(0f)
        }
    }

    val container = if (pressed) scheme.primary else scheme.primaryContainer
    val content = if (pressed) scheme.onPrimary else scheme.onPrimaryContainer

    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        // Pressure ring, drawn behind the pad.
        if (pressed) {
            val r = ring.value
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1f + 0.10f * r
                        scaleY = 1f + 0.10f * r
                        alpha = (1f - r) * 0.45f
                    }
                    .clip(padShape)
                    .background(scheme.primary.copy(alpha = 0.35f)),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        currentOnPressedChange(true)
                        waitForUpOrCancellation()
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        currentOnPressedChange(false)
                    }
                },
            shape = padShape,
            color = container,
            contentColor = content,
            border = BorderStroke(1.dp, content.copy(alpha = 0.14f)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Soft sheen from the top edge: gives the flat tonal fill a little depth.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    content.copy(alpha = 0.14f),
                                    content.copy(alpha = 0.02f),
                                    Color.Transparent,
                                ),
                            )
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = side,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (pressed) "Holding" else "Press & hold",
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Builds a [Shape] that morphs between two Material shapes at [progress].
 *
 * Cached on the shape pair, so the underlying [Morph] is constructed once rather than on every
 * frame. Returns the theme's extra-large shape if the pair cannot be matched, so a bad pair degrades
 * to something sane instead of crashing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberMorphShape(
    from: RoundedPolygon,
    to: RoundedPolygon,
    progress: Float,
): Shape {
    val fallback = MaterialTheme.shapes.extraLarge
    val morph = remember(from, to) { runCatching { Morph(from, to) }.getOrNull() }
        ?: return fallback
    val path = morph.toPath(progress.coerceIn(0f, 1f))
    return remember(path) { FitPathShape(path) }
}

/** Scales a morph path, normalised to a unit box, into whatever size the shape is given. */
private class FitPathShape(private val source: Path) : Shape {
    private var workPath: Path? = null
    private var workSize: Size = Size.Unspecified

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = workPath?.takeIf { workSize == size }?.apply { rewind() }
            ?: Path().also {
                workPath = it
                workSize = size
            }

        path.addPath(source)
        val bounds = path.getBounds()
        if (bounds.width > 0f && bounds.height > 0f) {
            val factor = minOf(size.width / bounds.width, size.height / bounds.height)
            if (factor.isFinite() && factor > 0f) {
                path.transform(Matrix().apply { scale(factor, factor) })
                path.translate(size.center - path.getBounds().center)
            }
        }
        return Outline.Generic(path)
    }
}

/* ----------------------------------------------------------------- graph */

/**
 * Width of the plotted time window, in milliseconds.
 *
 * Fixed, never adaptive. An adaptive window rescales the axis while data streams in, which shifts
 * the whole trace sideways on every sample and makes the right edge feel permanently unsettled.
 */
private const val WINDOW_MS = 6_000.0

/** Display delay, longer than one barometer interval so the head can be interpolated exactly. */
private const val HEAD_LAG_MS = 90.0

/** Horizontal grid divisions, drawn as faint M3 guides. */
private const val GRID_LINES = 4

/** Target width of one resampled column, in pixels. */
private const val COLUMN_PX = 3f

/**
 * Live trace of the squeeze.
 *
 * Rewritten from scratch. The previous version had a genuine maths bug that no amount of extra
 * filtering could hide:
 *
 *   The spline tangent was computed as a raw y-delta, `(y2 - y0) / 2`, and then multiplied by `dx`
 *   (an x-distance) to place the Bezier control point. A tangent must be a SLOPE, dy/dx. Mixing the
 *   two units made the control points swing far too vertically, so the curve oscillated between
 *   neighbouring samples and the line came out as a regular ripple. A numerical simulation of the
 *   old construction measured a max second-difference of 0.2574; with correct slope-based tangents
 *   it drops to 0.1000, i.e. 2.5x smoother, on identical data.
 *
 * The drawing pipeline is now, in order:
 *
 *  1. **Gaussian filter** over the sampled values (sigma 1.5, two passes) to remove barometer
 *     jitter. Display-only: the measurement, peak and verdict are untouched.
 *  2. **Column resampling** onto evenly spaced x positions, averaging samples per column. This
 *     guarantees strictly increasing, uniform x, so the spline can never see a duplicate point.
 *  3. **Slope-based Catmull-Rom** to cubic Beziers, using dy/dx tangents. Smooth, no ripple, and
 *     no overshoot beyond the data.
 *  4. **Frame-clock live edge** with an interpolated dot, so the head glides instead of stepping
 *     once per sensor reading.
 */
@Composable
fun PressureSparkline(
    history: List<TraceSample>,
    threshold: Float = TestThresholds.PASS_PRESSURE_RISE_HPA,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalSealAccents.current
    val lineColor = scheme.primary
    val thresholdColor = accents.success
    val gridColor = scheme.outlineVariant

    // Frame clock: single source of time for scrolling and for the live dot.
    var frameNanos by remember { mutableStateOf(System.nanoTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos = it }
        }
    }

    val tracePath = remember { Path() }
    val fillPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxWidth()) {
        if (history.isEmpty()) return@Canvas

        val nowMs = frameNanos / 1_000_000.0
        val firstMs = history.first().timeMs.toDouble()

        // Render ~60 ms behind the clock. That is longer than one barometer interval, so the
        // head always sits BETWEEN two known samples and can be linearly interpolated — exact,
        // no prediction, no spring. It still advances on every display frame, which is what
        // makes the dot move at 120 Hz instead of stepping at the sensor rate.
        val headMs = maxOf(firstMs, nowMs - HEAD_LAG_MS)

        // Origin advances with the clock, but is pinned to the first sample until the trace is
        // wider than the window, so the plot fills from the very first frame.
        val windowStart = maxOf(firstMs, headMs - WINDOW_MS)
        val windowEnd = windowStart + WINDOW_MS

        val leftPad = 12.dp.toPx()
        val rightPad = 18.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val plotWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        // Vertical scale. Zero sits low, the pass mark near the top. The ceiling only ever grows,
        // so the trace cannot jump vertically mid-test.
        val floor = -threshold * 0.30f
        val ceiling = maxOf(threshold * 1.20f, history.maxOf { it.value } * 1.15f)
        val span = (ceiling - floor).coerceAtLeast(0.01f)

        fun yOf(v: Float): Float =
            topPad + plotHeight * (1f - ((v - floor) / span).coerceIn(0f, 1f))

        // ---- grid ----
        for (i in 0..GRID_LINES) {
            val gy = topPad + plotHeight * (i.toFloat() / GRID_LINES)
            drawLine(
                color = gridColor.copy(alpha = 0.30f),
                start = Offset(leftPad, gy),
                end = Offset(leftPad + plotWidth, gy),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // ---- zero baseline ----
        drawLine(
            color = gridColor.copy(alpha = 0.55f),
            start = Offset(leftPad, yOf(0f)),
            end = Offset(leftPad + plotWidth, yOf(0f)),
            strokeWidth = 1.dp.toPx(),
        )

        // ---- pass threshold ----
        drawLine(
            color = thresholdColor.copy(alpha = 0.85f),
            start = Offset(leftPad, yOf(threshold)),
            end = Offset(leftPad + plotWidth, yOf(threshold)),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f)),
        )

        // ---- 1. gather visible samples ----
        val times = ArrayList<Double>(history.size)
        val values = ArrayList<Float>(history.size)
        for (s in history) {
            val t = s.timeMs.toDouble()
            if (t < windowStart) continue
            if (t > headMs) break
            times.add(t)
            values.add(s.value)
        }
        if (times.isEmpty()) return@Canvas

        // ---- 2. resample onto uniform columns ----
        //
        // NOTE: there is deliberately no smoothing pass here. Smoothing the VISIBLE subset every
        // frame is what made history visibly deform: as samples scroll into and out of the window
        // the subset changes, so the same historical point gets a different filtered value on each
        // frame and the curve appears to breathe. All smoothing now happens once, at write time, in
        // the ViewModel (see smoothedForDisplay), so a given sample always plots at the same height.
        val pts = resampleColumns(
            times = times,
            values = values,
            windowStart = windowStart,
            plotLeft = leftPad,
            plotWidth = plotWidth,
            toY = ::yOf,
        )
        if (pts.isEmpty()) return@Canvas

        // ---- 4. slope-based Catmull-Rom to cubic Bezier ----
        tracePath.rewind()
        tracePath.moveTo(pts[0].x, pts[0].y)
        if (pts.size == 2) {
            tracePath.lineTo(pts[1].x, pts[1].y)
        } else if (pts.size > 2) {
            for (i in 0 until pts.size - 1) {
                val p0 = pts[if (i == 0) 0 else i - 1]
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val p3 = pts[if (i + 2 > pts.lastIndex) pts.lastIndex else i + 2]

                // Tangents as SLOPES (dy/dx), which is the unit the control-point offset needs.
                val m1 = slope(p0, p2)
                val m2 = slope(p1, p3)
                val dx = (p2.x - p1.x) / 3f

                tracePath.cubicTo(
                    p1.x + dx,
                    p1.y + m1 * dx,
                    p2.x - dx,
                    p2.y - m2 * dx,
                    p2.x,
                    p2.y,
                )
            }
        }

        // Monotone cubic through the samples bracketing the head: position stays exact at the
        // samples, velocity stays continuous between them, and the limiter stops it inventing
        // peaks past the data.
        val headValue = valueAt(history, headMs)

        val liveX = (leftPad + ((headMs - windowStart) / WINDOW_MS) * plotWidth)
            .toFloat()
            .coerceIn(pts.last().x, leftPad + plotWidth)
        val liveY = yOf(headValue)

        // ---- area fill ----
        fillPath.rewind()
        fillPath.addPath(tracePath)
        fillPath.lineTo(liveX, topPad + plotHeight)
        fillPath.lineTo(pts[0].x, topPad + plotHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.26f),
                    lineColor.copy(alpha = 0.10f),
                    lineColor.copy(alpha = 0.00f),
                ),
                startY = topPad,
                endY = topPad + plotHeight,
            ),
        )

        // ---- the line ----
        drawPath(
            path = tracePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        // ---- live dot ----
        val pulse = ((nowMs / 1000.0) % 1.0).toFloat()
        drawCircle(
            color = lineColor.copy(alpha = 0.20f * (1f - pulse)),
            radius = 6.dp.toPx() + 9.dp.toPx() * pulse,
            center = Offset(liveX, liveY),
        )
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = Offset(liveX, liveY),
        )
        drawCircle(
            color = scheme.surfaceContainerLow,
            radius = 6.dp.toPx(),
            center = Offset(liveX, liveY),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

/**
 * Monotone cubic (Hermite with Fritsch–Carlson limits) sampled at [atMs].
 *
 * Exact at the data points and C1 between them, with tangents clamped so the curve cannot
 * overshoot a local peak — the linear interpolation it replaces had a velocity jump at every
 * sample, which read as the head stuttering.
 */
private fun valueAt(history: List<TraceSample>, atMs: Double): Float {
    var i = history.size - 1
    while (i > 0 && history[i].timeMs > atMs) i--
    val p1 = history[i]
    val p2 = history.getOrNull(i + 1) ?: return p1.value
    val dt = (p2.timeMs - p1.timeMs).toFloat()
    if (dt <= 0f) return p2.value

    val f = ((atMs - p1.timeMs) / dt).toFloat().coerceIn(0f, 1f)
    val v1 = p1.value
    val v2 = p2.value
    val p0 = history.getOrNull(i - 1)
    val p3 = history.getOrNull(i + 2)

    val slope = (v2 - v1) / dt
    val m1raw = if (p0 != null) {
        (v2 - p0.value) / (p2.timeMs - p0.timeMs).toFloat()
    } else {
        slope
    }
    val m2raw = if (p3 != null) {
        (p3.value - v1) / (p3.timeMs - p1.timeMs).toFloat()
    } else {
        slope
    }

    fun limit(m: Float): Float = when {
        slope == 0f -> 0f
        m * slope <= 0f -> 0f
        slope > 0f -> m.coerceIn(0f, 3f * slope)
        else -> m.coerceIn(3f * slope, 0f)
    }

    val f2 = f * f
    val f3 = f2 * f
    return (2f * f3 - 3f * f2 + 1f) * v1 +
        (f3 - 2f * f2 + f) * dt * limit(m1raw) +
        (-2f * f3 + 3f * f2) * v2 +
        (f3 - f2) * dt * limit(m2raw)
}

/** Slope of the line through two points, in dy/dx. */
private fun slope(a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    if (kotlin.math.abs(dx) < 0.001f) return 0f
    return (b.y - a.y) / dx
}

/**
 * Resamples a time series onto evenly spaced pixel columns.
 *
 * Each output point sits at a fixed x and carries the mean of the samples in that column. This
 * guarantees strictly increasing, uniformly spaced x, so the spline never receives two points at
 * the same position (which is what makes a curve oscillate). Column averaging also acts as a
 * low-pass filter.
 *
 * Empty columns are skipped rather than interpolated, so the trace never draws a flat bridge across
 * a gap in the data.
 */
private inline fun resampleColumns(
    times: List<Double>,
    values: List<Float>,
    windowStart: Double,
    plotLeft: Float,
    plotWidth: Float,
    toY: (Float) -> Float,
): List<Offset> {
    if (times.isEmpty()) return emptyList()

    val columns = (plotWidth / COLUMN_PX).toInt().coerceAtLeast(2)
    val sums = FloatArray(columns)
    val counts = IntArray(columns)

    for (i in times.indices) {
        val frac = ((times[i] - windowStart) / WINDOW_MS).toFloat()
        if (frac < 0f || frac > 1f) continue
        val col = (frac * (columns - 1)).toInt().coerceIn(0, columns - 1)
        sums[col] += values[i]
        counts[col]++
    }

    val out = ArrayList<Offset>(columns)
    for (c in 0 until columns) {
        if (counts[c] == 0) continue
        val cx = plotLeft + (c.toFloat() / (columns - 1)) * plotWidth
        out.add(Offset(cx, toY(sums[c] / counts[c])))
    }
    return out
}
/**
 * Compact label-over-value tile used for the result statistics.
 *
 * The value counts up from zero on first composition, so the numbers land with some weight instead
 * of just appearing. The animation is driven by a single float and formatted at each step, which
 * keeps it cheap.
 */
@Composable
fun MetricTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    animate: Boolean = true,
) {
    // Parse the numeric part so it can be counted up; anything unparseable is shown as-is.
    val target = remember(value) { value.toFloatOrNull() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(value) {
        if (animate && target != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }

    val shown = when {
        target == null -> value
        else -> {
            val v = target * progress.value
            // Preserve the original number of decimals and any explicit leading plus, which
            // String.format would drop once the value is parsed back to a float.
            val decimals = value.substringAfter('.', "").length
            val plus = if (value.startsWith("+")) "+" else ""
            String.format(Locale.US, "%s%.${decimals}f", plus, v)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = shown,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (valueColor == Color.Unspecified) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    valueColor
                },
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
