package com.raywdude.waterresistance.ui

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raywdude.waterresistance.TestViewModel
import com.raywdude.waterresistance.ui.theme.SealTestTheme
import dev.chrisbanes.haze.HazeState

internal const val SUPPORT_URL = "https://github.com/raywdude/water-resistance-tester"

/**
 * The start screen, hosted by its own activity.
 *
 * This activity registers NO back callback of any kind. That is the whole point: with nothing
 * intercepting back, the platform runs its own back-to-home animation — the window shrinks and
 * rounds toward the launcher icon while you drag, exactly as in the official sample video.
 *
 * The test flow lives in a separate activity ([com.raywdude.waterresistance.TestActivity]), so
 * pressing back there is a cross-activity back: the platform animates the test window away to
 * reveal THIS screen underneath. That is the "previous page visible behind" behaviour, and it only
 * happens when the app does not consume the gesture.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun StartApp(
    onStartTest: () -> Unit,
    viewModel: TestViewModel = viewModel(),
) {
    SealTestTheme {
        val history by viewModel.history.collectAsStateWithLifecycle()
        var helpVisible by rememberSaveable { mutableStateOf(false) }
        var historyVisible by rememberSaveable { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current
        val hazeState = remember { HazeState() }

        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val osLabel = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
        ) { _ ->
            ReadyScreen(
                deviceLabel = deviceLabel,
                osLabel = osLabel,
                hazeState = hazeState,
                onStart = onStartTest,
                onHistory = { historyVisible = true },
                onHelp = { helpVisible = true },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (helpVisible) {
            ModalBottomSheet(onDismissRequest = { helpVisible = false }) {
                HelpSheetContent(onSupport = { uriHandler.openUri(SUPPORT_URL) })
            }
        }

        if (historyVisible) {
            ModalBottomSheet(onDismissRequest = { historyVisible = false }) {
                HistorySheetContent(
                    entries = history,
                    onClear = viewModel::clearHistory,
                    onDelete = viewModel::removeHistory,
                )
            }
        }
    }
}
