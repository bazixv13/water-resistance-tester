package com.raywdude.waterresistance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raywdude.waterresistance.ui.TestApp

/**
 * The test flow, in its own activity.
 *
 * This is what makes the predictive back preview work the way the official sample shows: because
 * the test lives in a separate activity and registers no back callback, pressing back is a
 * cross-activity back. The platform animates this window away to reveal [MainActivity] underneath,
 * and shows that start screen as the preview while the gesture is dragged.
 *
 * A single-activity app cannot produce that effect: any in-app back handling consumes the gesture
 * and suppresses the system animation.
 */
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestApp(
                onFinished = { finish() },
            )
        }
    }
}
