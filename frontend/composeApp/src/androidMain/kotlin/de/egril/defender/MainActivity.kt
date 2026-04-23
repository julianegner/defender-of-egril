package de.egril.defender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.egril.defender.audio.GlobalBackgroundMusicManager
import de.egril.defender.audio.initializeAndroidAudio
import de.egril.defender.audio.setAndroidContext
import de.egril.defender.audio.setSoundEffectsAppInBackground
import de.egril.defender.iam.AndroidIamFlowProvider
import de.egril.defender.save.AndroidFileExportImport
import de.egril.defender.utils.WindowCloseHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inject the backend URL baked in at build time so that the shared
        // jvmMain BackendSaveHttpHelper can read it via System.getProperty().
        // This must happen before any backend call is made.
        System.setProperty("defender.backend.url", BuildConfig.BACKEND_URL)

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

    override fun onPause() {
        super.onPause()
        // Save game state to a background save slot so it can be restored if the process
        // is killed while the app is in the background for a long time.
        WindowCloseHandler.saveOnBackground()
        GlobalBackgroundMusicManager.pauseMusic()
        setSoundEffectsAppInBackground(true)
    }

    override fun onResume() {
        super.onResume()
        setSoundEffectsAppInBackground(false)
        GlobalBackgroundMusicManager.resumeMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            // Release audio resources only when the Activity is truly finishing (not just recreating).
            GlobalBackgroundMusicManager.release()
        }
    }
}
