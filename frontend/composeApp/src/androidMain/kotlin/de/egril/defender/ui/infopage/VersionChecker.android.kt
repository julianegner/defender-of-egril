package de.egril.defender.ui.infopage

actual fun platformAssetExtensions(): List<String>? {
    if (isInstalledFromPlayStore()) return null
    return listOf(".apk")
}
