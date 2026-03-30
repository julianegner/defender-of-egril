package de.egril.defender.ui.infopage

import de.egril.defender.AndroidContextProvider

/** Returns true when the app was installed via the Google Play Store. */
internal fun isInstalledFromPlayStore(): Boolean {
    return try {
        val context = AndroidContextProvider.getContext()
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        installer == "com.android.vending"
    } catch (_: Exception) {
        false
    }
}
