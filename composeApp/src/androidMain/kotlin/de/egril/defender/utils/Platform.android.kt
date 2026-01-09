package de.egril.defender.utils

import android.content.pm.PackageManager
import android.os.Build
import de.egril.defender.AndroidContextProvider

class AndroidPlatform : Platform {
    override val name: String = buildPlatformName()
    override val isAndroidTV: Boolean = checkIsAndroidTV()
    
    private fun buildPlatformName(): String {
        val baseInfo = "Android ${Build.VERSION.SDK_INT}"
        
        return try {
            val context = AndroidContextProvider.getContext()
            val isTV = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            
            if (isTV) {
                "$baseInfo (Android TV)"
            } else {
                baseInfo
            }
        } catch (e: Exception) {
            // If context is not available, return base info
            baseInfo
        }
    }
    
    private fun checkIsAndroidTV(): Boolean {
        return try {
            val context = AndroidContextProvider.getContext()
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        } catch (e: Exception) {
            // If context is not available, assume not TV
            false
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
