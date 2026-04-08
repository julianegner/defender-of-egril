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

@JsFun("(date) => date.getSeconds()")
external fun jsGetSeconds(date: JsAny): Int

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

actual fun formatTimestampISO(timestamp: Long): String {
    val date = jsCreateDate(timestamp.toDouble())
    
    // Format: "yyyy-MM-dd_HH-mm-ss"
    val year = jsGetFullYear(date)
    val month = jsGetMonth(date) + 1 // JS months are 0-indexed
    val day = jsGetDate(date)
    val hours = jsGetHours(date)
    val minutes = jsGetMinutes(date)
    val seconds = jsGetSeconds(date)
    
    return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}_" +
           "${hours.toString().padStart(2, '0')}-${minutes.toString().padStart(2, '0')}-${seconds.toString().padStart(2, '0')}"
}

actual fun getLocalHour(timestamp: Long): Int {
    val date = jsCreateDate(timestamp.toDouble())
    return jsGetHours(date)
}
