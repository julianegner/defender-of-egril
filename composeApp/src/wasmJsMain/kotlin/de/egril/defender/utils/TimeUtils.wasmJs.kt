@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.utils

@JsFun("() => Date.now()")
external fun jsDateNow(): Double

@JsFun("(timestamp) => new Date(timestamp)")
external fun jsCreateDate(timestamp: Double): JsAny

@JsFun("(date) => date.getFullYear()")
external fun jsGetFullYear(date: JsAny): Int

@JsFun("(date) => date.getMonth()")
external fun jsGetMonth(date: JsAny): Int

@JsFun("(date) => date.getDate()")
external fun jsGetDate(date: JsAny): Int

@JsFun("(date) => date.getHours()")
external fun jsGetHours(date: JsAny): Int

@JsFun("(date) => date.getMinutes()")
external fun jsGetMinutes(date: JsAny): Int

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

actual fun currentTimeMillis(): Long {
    return jsDateNow().toLong()
}

actual fun formatTimestamp(timestamp: Long): String {
    val date = jsCreateDate(timestamp.toDouble())
    
    // Format: "MMM dd, yyyy HH:mm"
    val year = jsGetFullYear(date)
    val monthIndex = jsGetMonth(date)
    val day = jsGetDate(date)
    val hours = jsGetHours(date)
    val minutes = jsGetMinutes(date)
    
    val monthStr = MONTH_NAMES[monthIndex]
    
    return "$monthStr $day, $year ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
