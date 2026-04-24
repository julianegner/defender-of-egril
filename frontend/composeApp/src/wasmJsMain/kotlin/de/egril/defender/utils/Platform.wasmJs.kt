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

/**
 * Extracts the operating system name from the browser User-Agent string.
 * Detection order: Android → iOS → Windows → macOS → ChromeOS → Linux → null.
 */
@JsFun("""() => {
    try {
        var ua = navigator.userAgent;
        var android = ua.match(/Android ([0-9.]+)/);
        if (android) return "Android " + android[1];
        var ios = ua.match(/(?:iPhone|iPad|iPod)[^)]*?OS ([0-9_]+)/);
        if (ios) return "iOS " + ios[1].replace(/_/g, ".");
        var win = ua.match(/Windows NT ([0-9.]+)/);
        if (win) {
            var v = win[1];
            if (v === "10.0") return "Windows 10/11";
            if (v === "6.3") return "Windows 8.1";
            if (v === "6.2") return "Windows 8";
            if (v === "6.1") return "Windows 7";
            return "Windows NT " + v;
        }
        var mac = ua.match(/Mac OS X ([0-9_.]+)/);
        if (mac) return "macOS " + mac[1].replace(/_/g, ".");
        if (ua.indexOf("CrOS") >= 0) return "ChromeOS";
        if (ua.indexOf("Linux") >= 0) return "Linux";
        return null;
    } catch (e) { return null; }
}""")
private external fun getBrowserOsName(): String?

class WasmPlatform: Platform {
    override val name: String = getBrowserUserAgent()?.let { "Web with Kotlin/Wasm $it" } ?: "Web with Kotlin/Wasm"
    override val isAndroidTV: Boolean = false
    override val isSteamDeckGamingMode: Boolean = false
    override val platformExtended: String = getBrowserInfo() ?: "Unknown"
    override val osName: String? = getBrowserOsName()
}

actual fun getPlatform(): Platform = WasmPlatform()

// External JS function to get browser language
@JsFun("() => { try { return navigator.language.split('-')[0].toLowerCase(); } catch (e) { return null; } }")
private external fun getBrowserLanguage(): String?

actual fun getSystemLanguageCode(): String? {
    return getBrowserLanguage()
}

actual fun getCurrentUsername(): String = ""
