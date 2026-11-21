package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Today's local date as `yyyy-MM-dd`. Uses SimpleDateFormat rather than java.time because
 * minSdk is 24 and core library desugaring is not enabled.
 */
fun todayIsoDate(now: Date = Date()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

/** True only for a real calendar day in exact `yyyy-MM-dd` form (rejects 2024-13-40, 2024-2-1, etc.). */
fun isValidIsoDate(value: String): Boolean {
    if (!ISO_DATE.matches(value)) return false
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
    }.getOrNull() != null
}

private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
