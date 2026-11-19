package com.padkeys.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private val state: PadKeysState by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BoxWithConstraints(Modifier.fillMaxSize().background(C.Bg)) {
                    // Ang buong UI ay dinisenyo sa 720dp na taas. Ini-scale ito para
                    // pumasok sa taas ng screen, at ang lapad ay umaangkop gamit ang weights.
                    val scaled = Density(density = constraints.maxHeight / 720f, fontScale = 1f)
                    CompositionLocalProvider(LocalDensity provides scaled) {
                        PadKeysScreen(state)
                    }
                }
            }
        }

        // Fullscreen: itago ang status at navigation bars (swipe para lumabas).
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onStart() {
        super.onStart()
        state.startAudio()
    }

    override fun onStop() {
        state.stopAudio()
        super.onStop()
    }
}
