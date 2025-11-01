package com.defenderofegril.utils

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun formatTimestamp(timestamp: Long): String {
    val date = NSDate(timeIntervalSince1970 = timestamp / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "MMM dd, yyyy HH:mm"
    formatter.locale = NSLocale.currentLocale
    return formatter.stringFromDate(date)
}
