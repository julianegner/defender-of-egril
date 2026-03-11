package de.egril.defender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.egril.defender.audio.initializeAndroidAudio
import de.egril.defender.audio.setAndroidContext
import de.egril.defender.iam.AndroidIamFlowProvider
import de.egril.defender.save.AndroidFileExportImport

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize context provider for file storage
        AndroidContextProvider.initialize(this)
        
        // Register activity with the OIDC auth-flow factory so Chrome Custom Tabs
        // can redirect back to the app and the login flow can be continued/resumed.
        AndroidIamFlowProvider.registerActivity(this)
        
        // Initialize file export/import for save files
        AndroidFileExportImport.initialize(this)
        
        // Initialize Android audio system for sound playback
        initializeAndroidAudio(this)
        
        // Initialize Android context for background music manager
        setAndroidContext(this)
        
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
