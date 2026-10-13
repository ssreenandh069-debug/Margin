package com.Margin.app.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AttendanceMath {

    fun utcToLocalMidnight(utcDateMs: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcDateMs }
        val localCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    fun localToUtcMidnight(localMs: Long): Long {
        val localCal = Calendar.getInstance().apply { timeInMillis = localMs }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, localCal.get(Calendar.YEAR))
            set(Calendar.MONTH, localCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis
    }

    /**
     * Converts a time string (e.g. "09:00 AM", "1:30 PM", "14:00", "2:00 - 3:00 PM") into minutes
     * from midnight (0..1439) for 100% accurate chronological sorting.
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        val raw = timeStr.trim().uppercase(Locale.US)
        // If it's a range like "2:00 - 3:00 PM", extract the start time but inherit trailing AM/PM if missing
        val parts = raw.split(Regex("""[-—]|(?:\bTO\b)"""))
        var clean = parts.firstOrNull()?.trim() ?: raw
        if (!clean.contains("AM") && !clean.contains("PM")) {
            if (raw.endsWith("PM")) clean = "$clean PM"
            else if (raw.endsWith("AM")) clean = "$clean AM"
        }

        val formats = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm", "h a", "ha")
        for (fmt in formats) {
            try {
                val d = SimpleDateFormat(fmt, Locale.US).parse(clean)
                if (d != null) {
                    val cal = Calendar.getInstance().apply { time = d }
                    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }
            } catch (_: Exception) {}
        }
        val match = Regex("""(\d+)(?::(\d+))?\s*(AM|PM)?""").find(clean)
        if (match != null) {
            var h = match.groupValues[1].toIntOrNull() ?: 0
            val m = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3]
            if (ampm == "PM" && h < 12) h += 12
            if (ampm == "AM" && h == 12) h = 0
            return h * 60 + m
        }
        return 0
    }

    fun calculatePercentage(attended: Int, total: Int): Float {
        if (total <= 0) return 0f
        return (attended.toFloat() / total.toFloat()) * 100f
    }

    fun calculateSafeBunks(attended: Int, total: Int, targetPercentage: Float = 75f): Int {
        if (total <= 0) return 0
        if (targetPercentage <= 0f) return attended
        if (targetPercentage == 75f) {
            val margin = 4 * attended - 3 * total
            return if (margin <= 0) 0 else margin / 3
        }
        val target = targetPercentage / 100.0
        val safe = kotlin.math.floor((attended - (target * total)) / target).toInt()
        return maxOf(0, safe)
    }

    fun calculateClassesNeeded(attended: Int, total: Int, targetPercentage: Float = 75f): Int {
        if (total <= 0) return 0
        val currentPct = calculatePercentage(attended, total)
        if (currentPct >= targetPercentage) return 0
        if (targetPercentage == 75f) {
            return maxOf(0, 3 * total - 4 * attended)
        }
        val target = targetPercentage / 100.0
        val needed = kotlin.math.ceil((target * total - attended) / (1.0 - target)).toInt()
        return maxOf(0, needed)
    }

    fun formatRelativeDueDate(dueDateMs: Long): String {
        val now = System.currentTimeMillis()
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val dueCal = Calendar.getInstance().apply {
            timeInMillis = dueDateMs
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }

        val diffDays = ((dueCal.timeInMillis - todayCal.timeInMillis) / 86400000L).toInt()

        return when {
            diffDays < -1 -> "Overdue by ${-diffDays} days"
            diffDays == -1 -> "Overdue by 1 day"
            diffDays == 0 -> "Due today"
            diffDays == 1 -> "Due tomorrow"
            diffDays in 2..6 -> "Due in $diffDays days"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDateMs))
        }
    }
}
