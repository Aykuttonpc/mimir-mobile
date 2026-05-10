package com.aykutcincik.mimir.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object PresenceFormat {

    /** "şimdi", "5 dk önce", "2 saat önce", "Dün 14:32", "12 Mar 09:21" */
    fun lastSeenLabel(lastSeenIso: String?): String {
        if (lastSeenIso.isNullOrBlank()) return "uzun zaman önce"
        val seen = runCatching { Instant.parse(lastSeenIso) }.getOrNull() ?: return "uzun zaman önce"
        val now = Instant.now()
        val diffMin = ChronoUnit.MINUTES.between(seen, now)

        return when {
            diffMin < 1 -> "şimdi"
            diffMin < 60 -> "$diffMin dk önce"
            diffMin < 60 * 24 -> "${diffMin / 60} saat önce"
            else -> {
                val zone = ZoneId.systemDefault()
                val ldt = LocalDateTime.ofInstant(seen, zone)
                val today = LocalDateTime.now(zone)
                val daysDiff = ChronoUnit.DAYS.between(ldt.toLocalDate(), today.toLocalDate())
                when (daysDiff) {
                    1L -> "Dün ${ldt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    in 2..6 -> ldt.format(DateTimeFormatter.ofPattern("EEE HH:mm"))
                    else -> ldt.format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
                }
            }
        }
    }

    /** "çevrimiçi" veya "son görülme: X dk önce" */
    fun statusLabel(isOnline: Boolean, lastSeenIso: String?): String {
        return if (isOnline) "çevrimiçi" else "son görülme: ${lastSeenLabel(lastSeenIso)}"
    }
}
