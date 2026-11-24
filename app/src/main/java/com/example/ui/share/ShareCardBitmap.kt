package com.example.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.ceil

/** Renders share-card text lines to a bitmap (no Compose, no invented facts). */
fun renderShareCardBitmap(
    lines: List<String>,
    widthPx: Int = 1080,
    backgroundColor: Int = 0xFF0B1A24.toInt(),
    titleColor: Int = 0xFFE8B923.toInt(),
    bodyColor: Int = 0xFFE8EEF2.toInt(),
    creditColor: Int = 0xFF8A9AA8.toInt()
): Bitmap {
    val pad = 64f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = titleColor
        textSize = 56f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyColor
        textSize = 40f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = creditColor
        textSize = 28f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
    }
    val lineGap = 18f
    var height = pad * 2
    for ((i, line) in lines.withIndex()) {
        val paint = when {
            i == 0 -> titlePaint
            line == SETLIST_CREDIT -> creditPaint
            else -> bodyPaint
        }
        height += if (line.isEmpty()) paint.textSize * 0.4f else paint.textSize + lineGap
    }
    val bitmap = Bitmap.createBitmap(widthPx, ceil(height).toInt().coerceAtLeast(400), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)
    var y = pad + titlePaint.textSize
    for ((i, line) in lines.withIndex()) {
        val paint = when {
            i == 0 -> titlePaint
            line == SETLIST_CREDIT -> creditPaint
            else -> bodyPaint
        }
        if (line.isEmpty()) {
            y += paint.textSize * 0.4f
        } else {
            canvas.drawText(line, pad, y, paint)
            y += paint.textSize + lineGap
        }
    }
    return bitmap
}
