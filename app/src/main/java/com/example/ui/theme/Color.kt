package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Supported theme modes for the DeadSet Global Theme Provider.
 */
enum class DeadThemeMode(val title: String, val subtitle: String) {
    STEAL_YOUR_FACE_DARK(
        title = "Steal Your Face Dark",
        subtitle = "Deep midnight skull canvas with crimson, cobalt & lightning gold"
    ),
    SUNSHINE_LIGHT(
        title = "Sunshine Daydream Light",
        subtitle = "Warm Fillmore poster parchment with high-contrast archival ink"
    ),
    SYSTEM(
        title = "System Auto",
        subtitle = "Automatically follows your Android device dark/light appearance"
    )
}

/**
 * Snapshot-reactive state holder backing the global theme provider so that
 * both MaterialTheme.colorScheme and DeadSet semantic tokens update live across all screens.
 */
object DeadSetThemeState {
    var themeMode: DeadThemeMode by mutableStateOf(DeadThemeMode.STEAL_YOUR_FACE_DARK)
    var isDarkTheme: Boolean by mutableStateOf(true)
    var useDynamicColor: Boolean by mutableStateOf(true)

    fun toggleDarkLight() {
        val nextDark = !isDarkTheme
        isDarkTheme = nextDark
        themeMode = if (nextDark) DeadThemeMode.STEAL_YOUR_FACE_DARK else DeadThemeMode.SUNSHINE_LIGHT
    }

    fun applyThemeMode(mode: DeadThemeMode, systemInDark: Boolean = true) {
        themeMode = mode
        isDarkTheme = when (mode) {
            DeadThemeMode.STEAL_YOUR_FACE_DARK -> true
            DeadThemeMode.SUNSHINE_LIGHT -> false
            DeadThemeMode.SYSTEM -> systemInDark
        }
    }
}

// Raw 'Steal Your Face' Dark Mode Palette Constants
val StealieMidnightDarkRaw = Color(0xFF0A0F1D)
val StealieSurfaceDarkRaw = Color(0xFF111827)
val StealieCardDarkRaw = Color(0xFF1E293B)
val StealieCardElevatedDarkRaw = Color(0xFF26334D)
val TextPrimaryDarkRaw = Color(0xFFF8FAFC)
val TextSecondaryDarkRaw = Color(0xFF94A3B8)
val TextTertiaryDarkRaw = Color(0xFF64748B)
val BorderSubtleDarkRaw = Color(0xFF334155)

// Raw 'Sunshine Daydream' Light Mode Palette Constants
val StealieParchmentLightRaw = Color(0xFFFBF8F1)
val StealieSurfaceLightRaw = Color(0xFFF2ECE1)
val StealieCardLightRaw = Color(0xFFFFFFFF)
val StealieCardElevatedLightRaw = Color(0xFFE8DFCE)
val TextPrimaryLightRaw = Color(0xFF0F172A)
val TextSecondaryLightRaw = Color(0xFF334155)
val TextTertiaryLightRaw = Color(0xFF64748B)
val BorderSubtleLightRaw = Color(0xFFCBD5E1)

// Reactive Semantic Surface & Text Colors (automatically adapt to Light vs 'Steal Your Face' Dark Mode)
val StealieMidnight: Color
    get() = if (DeadSetThemeState.isDarkTheme) StealieMidnightDarkRaw else StealieParchmentLightRaw

val StealieSurfaceDark: Color
    get() = if (DeadSetThemeState.isDarkTheme) StealieSurfaceDarkRaw else StealieSurfaceLightRaw

val StealieCardDark: Color
    get() = if (DeadSetThemeState.isDarkTheme) StealieCardDarkRaw else StealieCardLightRaw

val StealieCardElevated: Color
    get() = if (DeadSetThemeState.isDarkTheme) StealieCardElevatedDarkRaw else StealieCardElevatedLightRaw

val TextPrimaryDark: Color
    get() = if (DeadSetThemeState.isDarkTheme) TextPrimaryDarkRaw else TextPrimaryLightRaw

val TextSecondaryDark: Color
    get() = if (DeadSetThemeState.isDarkTheme) TextSecondaryDarkRaw else TextSecondaryLightRaw

val TextTertiaryDark: Color
    get() = if (DeadSetThemeState.isDarkTheme) TextTertiaryDarkRaw else TextTertiaryLightRaw

val BorderSubtle: Color
    get() = if (DeadSetThemeState.isDarkTheme) BorderSubtleDarkRaw else BorderSubtleLightRaw

// Iconic Steal Your Face Accents (with high-contrast adaptation in Light Mode)
val StealieCrimson = Color(0xFFDC2626)
val StealieCrimsonLight: Color
    get() = if (DeadSetThemeState.isDarkTheme) Color(0xFFEF4444) else Color(0xFFB91C1C)

val StealieBlue = Color(0xFF2563EB)
val StealieCyan: Color
    get() = if (DeadSetThemeState.isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)

val StealieAmber = Color(0xFFF59E0B)
val StealieGold: Color
    get() = if (DeadSetThemeState.isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)

val StealiePurple = Color(0xFF8B5CF6)
val StealieGreen = Color(0xFF10B981)
val StealieRose = Color(0xFFF43F5E)

// Era Theme Colors for visual distinction
val EraPrimalColor = Color(0xFFEC4899)      // 60s Primal Psych Pink
val EraEurope72Color = Color(0xFF3B82F6)    // Early 70s Europe Blue
val EraWallOfSoundColor = Color(0xFF10B981) // Wall of Sound Green
val EraSpring77Color = Color(0xFFF59E0B)    // Spring 77 Golden Era
val EraBrent80sColor = Color(0xFF8B5CF6)    // 80s Brent Era Purple
val Era90sColor = Color(0xFF06B6D4)         // 90s Vince & Bruce Cyan
