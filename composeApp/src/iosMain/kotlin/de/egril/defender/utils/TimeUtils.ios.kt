package de.egril.defender.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

@OptIn(ExperimentalForeignApi::class)
actual fun formatTimestamp(timestamp: Long): String {
    val date = NSDate(timestamp / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "MMM dd, yyyy HH:mm"
    formatter.locale = NSLocale.currentLocale
    return formatter.stringFromDate(date)
}

@OptIn(ExperimentalForeignApi::class)
actual fun formatTimestampISO(timestamp: Long): String {
    val date = NSDate(timestamp / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd_HH-mm-ss"
    formatter.locale = NSLocale.currentLocale
    return formatter.stringFromDate(date)
}
