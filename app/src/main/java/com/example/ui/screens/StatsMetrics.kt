package com.example.ui.screens

/** Progress fraction for [SongFrequencyBar] track fill (6.3). */
internal fun songFrequencyProgress(playCount: Int, maxCount: Int): Float {
    if (maxCount <= 0) return 0.05f
    return (playCount.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1f)
}
