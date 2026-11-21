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
