package de.egril.defender.utils

import android.content.pm.PackageManager
import android.os.Build
import de.egril.defender.AndroidContextProvider
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = buildPlatformName()
    override val isAndroidTV: Boolean = checkIsAndroidTV()
    override val platformExtended: String = buildPlatformExtended()
    
    private fun checkIsAndroidTV(): Boolean {
        return try {
            val context = AndroidContextProvider.getContext()
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        } catch (e: Exception) {
            // If context is not available, assume not TV
            false
        }
    }
    
    private fun buildPlatformName(): String {
        val baseInfo = "Android ${Build.VERSION.SDK_INT}"
        
        return try {
            if (checkIsAndroidTV()) {
                "$baseInfo (Android TV)"
            } else {
                baseInfo
            }
        } catch (e: Exception) {
            // If context is not available, return base info
            baseInfo
        }
    }

    private fun buildPlatformExtended(): String {
        val version = "Android ${Build.VERSION.RELEASE}"
        return try {
            if (checkIsAndroidTV()) "$version (Android TV)" else version
        } catch (e: Exception) {
            version
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getSystemLanguageCode(): String? {
    return try {
        Locale.getDefault().language.lowercase()
    } catch (e: Exception) {
        null
    }
}

actual fun getCurrentUsername(): String = ""
