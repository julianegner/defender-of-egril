package de.egril.defender.utils

interface Platform {
    val name: String
    val isAndroidTV: Boolean
}

expect fun getPlatform(): Platform

val isPlatformWasm = ("Web with Kotlin/Wasm" == getPlatform().name)
val isPlatformAndroid = getPlatform().name.startsWith("Android")
val isPlatformIos = getPlatform().name.startsWith("iOS")
val isPlatformDesktop = getPlatform().name.startsWith("Java")
val isPlatformMobile = isPlatformAndroid || isPlatformIos
