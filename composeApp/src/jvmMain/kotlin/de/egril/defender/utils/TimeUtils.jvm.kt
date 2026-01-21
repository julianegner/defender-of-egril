package de.egril.defender.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

actual fun formatTimestampISO(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

actual fun getLocalHour(timestamp: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.HOUR_OF_DAY)
}
