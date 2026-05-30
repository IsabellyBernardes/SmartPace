package com.example.smartpace.utils

import com.example.smartpace.model.Run
import java.util.Calendar

fun isToday(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
    return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
}

fun isThisWeek(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
    return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
}

fun isLastWeek(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val lastWeekNum = if (now.get(Calendar.WEEK_OF_YEAR) == 1) 52 else now.get(Calendar.WEEK_OF_YEAR) - 1
    val lastWeekYear = if (now.get(Calendar.WEEK_OF_YEAR) == 1) now.get(Calendar.YEAR) - 1 else now.get(Calendar.YEAR)
    val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
    return cal.get(Calendar.YEAR) == lastWeekYear &&
            cal.get(Calendar.WEEK_OF_YEAR) == lastWeekNum
}

fun parseDurationToSeconds(duration: String): Int {
    return try {
        val parts = duration.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) { 0 }
}

fun formatTotalTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h${minutes}m" else "${minutes}m"
}

fun paceToSeconds(pace: String): Int {
    return try {
        val parts = pace.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) { Int.MAX_VALUE }
}

fun hasSevenConsecutiveDays(runs: List<Run>): Boolean {
    if (runs.size < 7) return false
    val days = runs.map {
        val cal = Calendar.getInstance().also { c -> c.timeInMillis = it.timestamp }
        cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR)
    }.toSortedSet().toList()
    var consecutive = 1
    for (i in 1 until days.size) {
        consecutive = if (days[i] - days[i - 1] == 1) consecutive + 1 else 1
        if (consecutive >= 7) return true
    }
    return false
}
