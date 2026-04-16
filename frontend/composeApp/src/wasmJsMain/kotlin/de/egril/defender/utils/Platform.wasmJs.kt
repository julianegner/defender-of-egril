@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.utils

// External JS function to get browser user agent
@JsFun("() => { try { return navigator.userAgent; } catch (e) { return null; } }")
private external fun getBrowserUserAgent(): String?

/**
 * Extracts the browser name and full version from the User-Agent string.
 * Detection order: Edge → Chrome → Firefox → Safari → fallback to raw UA.
 */
@JsFun("""() => {
    try {
        var ua = navigator.userAgent;
        var edg = ua.match(/Edg\/([0-9.]+)/);
        if (edg) return "Edge/" + edg[1];
        var chrome = ua.match(/Chrome\/([0-9.]+)/);
        if (chrome) return "Chrome/" + chrome[1];
        var ff = ua.match(/Firefox\/([0-9.]+)/);
        if (ff) return "Firefox/" + ff[1];
        var safari = ua.match(/Version\/([0-9.]+).*Safari/);
        if (safari) return "Safari/" + safari[1];
        return ua;
    } catch (e) { return null; }
}""")
private external fun getBrowserInfo(): String?

class WasmPlatform: Platform {
    override val name: String = getBrowserUserAgent()?.let { "Web with Kotlin/Wasm $it" } ?: "Web with Kotlin/Wasm"
    override val isAndroidTV: Boolean = false
    override val isSteamDeckGamingMode: Boolean = false
    override val platformExtended: String = getBrowserInfo() ?: "Unknown"
}

actual fun getPlatform(): Platform = WasmPlatform()

// External JS function to get browser language
@JsFun("() => { try { return navigator.language.split('-')[0].toLowerCase(); } catch (e) { return null; } }")
private external fun getBrowserLanguage(): String?

actual fun getSystemLanguageCode(): String? {
    return getBrowserLanguage()
}

actual fun getCurrentUsername(): String = ""
