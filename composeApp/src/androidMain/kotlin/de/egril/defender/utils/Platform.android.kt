package de.egril.defender.utils

import android.content.pm.PackageManager
import android.os.Build
import de.egril.defender.AndroidContextProvider

class AndroidPlatform : Platform {
    override val name: String = buildPlatformName()
    
    private fun buildPlatformName(): String {
        val baseInfo = "Android ${Build.VERSION.SDK_INT}"
        
        return try {
            val context = AndroidContextProvider.getContext()
            val isAndroidTV = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            
            if (isAndroidTV) {
                "$baseInfo (Android TV)"
            } else {
                baseInfo
            }
        } catch (e: Exception) {
            // If context is not available, return base info
            baseInfo
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
