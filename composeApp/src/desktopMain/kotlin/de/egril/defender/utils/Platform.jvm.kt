package de.egril.defender.utils

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isAndroidTV: Boolean = false
}

actual fun getPlatform(): Platform = JVMPlatform()
