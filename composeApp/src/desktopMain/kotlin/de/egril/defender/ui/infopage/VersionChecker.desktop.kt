package de.egril.defender.ui.infopage

actual fun platformAssetExtensions(): List<String>? {
    val os = System.getProperty("os.name")?.lowercase() ?: return null
    return when {
        "windows" in os -> listOf(".exe", ".msi")
        "mac" in os -> listOf(".dmg")
        "linux" in os -> listOf(".deb")
        else -> null
    }
}
