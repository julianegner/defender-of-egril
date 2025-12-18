package de.egril.defender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.egril.defender.audio.initializeAndroidAudio
import de.egril.defender.save.AndroidFileExportImport

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize context provider for file storage
        AndroidContextProvider.initialize(this)
        
        // Initialize file export/import for save files
        AndroidFileExportImport.initialize(this)
        
        // Initialize Android audio system for sound playback
        initializeAndroidAudio(this)
        
        enableEdgeToEdge()
        
        // Enable immersive fullscreen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.apply {
            // Hide system bars
            hide(WindowInsetsCompat.Type.systemBars())
            // Configure behavior when swiping to reveal system bars
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContent {
            App()
        }
    }
}
