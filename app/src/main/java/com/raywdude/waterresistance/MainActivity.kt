package com.raywdude.waterresistance

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raywdude.waterresistance.ui.StartApp

/**
 * The start screen.
 *
 * Deliberately registers no back callback, so back here falls through to the system and plays the
 * platform's own back-to-home animation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StartApp(
                onStartTest = {
                    startActivity(Intent(this, TestActivity::class.java))
                },
            )
        }
    }
}
