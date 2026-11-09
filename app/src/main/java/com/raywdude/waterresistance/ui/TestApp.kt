package com.raywdude.waterresistance.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raywdude.waterresistance.TestPhase
import com.raywdude.waterresistance.TestViewModel
import com.raywdude.waterresistance.ui.theme.SealTestTheme

/**
 * The test flow, hosted by its own activity.
 *
 * Like [StartApp] this registers no back callback. Back here is therefore a cross-activity back,
 * handled entirely by the platform: the test window is animated away to reveal the start screen
 * behind it, and the predictive preview shows that start screen while the gesture is dragged.
 *
 * The flow ending on its own (a finished test, or the user tapping "Test again") is different from
 * the user pressing back, so those paths call [onFinished] explicitly rather than relying on back.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun TestApp(
    onFinished: () -> Unit,
    viewModel: TestViewModel = viewModel(),
) {
    SealTestTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
        val view = LocalView.current

        // This activity exists to run a test, so start one on entry. Without this a fresh
        // ViewModel sits in Ready and the screen would have nothing to show.
        LaunchedEffect(Unit) {
            if (state.phase == TestPhase.Ready) viewModel.startTest()
        }

        val keepAwake = state.phase == TestPhase.Calibrating || state.phase == TestPhase.Pressing
        DisposableEffect(view, keepAwake) {
            view.keepScreenOn = keepAwake
            onDispose { view.keepScreenOn = false }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
        ) { _ ->
            // Shared-element-ish phase change: the new screen scales up from just under full
            // size while the old one fades and drifts back. This is what makes calibrating ->
            // pressing -> result read as one continuous instrument rather than three swaps.
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val enter = fadeIn(tween(durationMillis = 260, delayMillis = 50)) +
                        scaleIn(
                            initialScale = if (forward) 0.94f else 1.03f,
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    val exit = fadeOut(tween(durationMillis = 180)) +
                        scaleOut(
                            targetScale = if (forward) 1.03f else 0.94f,
                            animationSpec = tween(durationMillis = 180),
                        )
                    enter togetherWith exit
                },
                label = "test phase",
            ) { phase ->
                when (phase) {
                    TestPhase.Calibrating -> CalibratingScreen(state)

                    TestPhase.Pressing -> PressingScreen(
                        state = state,
                        onFingersChanged = viewModel::onFingersChanged,
                    )

                    TestPhase.Result -> {
                        // Frozen snapshot, never the live state: this is what stopped the
                        // "unsuccessful" flash on the way back out.
                        val result = lastResult ?: state
                        ResultScreen(
                            state = result,
                            onRestart = viewModel::startTest,
                        )
                    }

                    TestPhase.Unsupported -> UnsupportedScreen(
                        onSupport = { onFinished() },
                    )

                    // Ready is the state this activity starts in, before startTest() above takes
                    // effect. It must NOT call onFinished() here or the activity would close the
                    // instant it opens; the LaunchedEffect at the top is what moves it on.
                    TestPhase.Ready -> Unit
                }
            }
        }
    }
}
