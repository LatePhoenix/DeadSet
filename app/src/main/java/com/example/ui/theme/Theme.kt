package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private const val THEME_PREFS_NAME = "deadset_theme_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_DYNAMIC_COLOR = "use_dynamic_color"

/**
 * Controller exposed via [LocalDeadSetThemeController] allowing any screen or component
 * to inspect or toggle between the 'Sunshine Daydream' Light theme, the
 * 'Steal Your Face' inspired Dark mode, and Material 3 Dynamic Color schemes.
 */
@Stable
data class DeadSetThemeController(
    val themeMode: DeadThemeMode = DeadThemeMode.STEAL_YOUR_FACE_DARK,
    val isDarkTheme: Boolean = true,
    val useDynamicColor: Boolean = true,
    val isDynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val onToggleTheme: () -> Unit = {},
    val onSetThemeMode: (DeadThemeMode) -> Unit = {},
    val onSetDynamicColor: (Boolean) -> Unit = {}
)

val LocalDeadSetThemeController = staticCompositionLocalOf {
    DeadSetThemeController()
}

/**
 * Signature 'Steal Your Face' Dark Mode Material 3 ColorScheme:
 * Deep midnight canvas, 13-point lightning gold primary, cobalt/cyan secondary, and crimson tertiary.
 */
val StealYourFaceDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = StealieMidnightDarkRaw,
    primaryContainer = StealieCardElevatedDarkRaw,
    onPrimaryContainer = Color(0xFFFBBF24),
    secondary = Color(0xFF38BDF8),
    onSecondary = StealieMidnightDarkRaw,
    secondaryContainer = StealieCardDarkRaw,
    onSecondaryContainer = Color(0xFF38BDF8),
    tertiary = Color(0xFFEF4444),
    onTertiary = TextPrimaryDarkRaw,
    tertiaryContainer = Color(0xFF450A0A),
    onTertiaryContainer = Color(0xFFFECACA),
    background = StealieMidnightDarkRaw,
    onBackground = TextPrimaryDarkRaw,
    surface = StealieSurfaceDarkRaw,
    onSurface = TextPrimaryDarkRaw,
    surfaceVariant = StealieCardDarkRaw,
    onSurfaceVariant = TextSecondaryDarkRaw,
    outline = BorderSubtleDarkRaw,
    outlineVariant = BorderSubtleDarkRaw
)

/**
 * 'Sunshine Daydream' Light Mode Material 3 ColorScheme:
 * Vintage Fillmore concert poster parchment, warm golden-amber primary, cobalt secondary, and crimson tertiary.
 */
val SunshineDaydreamLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFDC2626),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEE2E2),
    onTertiaryContainer = Color(0xFF7F1D1D),
    background = StealieParchmentLightRaw,
    onBackground = TextPrimaryLightRaw,
    surface = StealieSurfaceLightRaw,
    onSurface = TextPrimaryLightRaw,
    surfaceVariant = StealieCardLightRaw,
    onSurfaceVariant = TextSecondaryLightRaw,
    outline = BorderSubtleLightRaw,
    outlineVariant = BorderSubtleLightRaw
)

/**
 * Resolves the active Material 3 [ColorScheme], blending Android 12+ Material 3
 * dynamic color schemes ([dynamicDarkColorScheme] / [dynamicLightColorScheme]) with
 * the 'Steal Your Face' Dark or 'Sunshine Daydream' Light palette when [useDynamicColor] is enabled.
 */
fun resolveDeadSetColorScheme(
    context: Context,
    isDarkTheme: Boolean,
    useDynamicColor: Boolean
): ColorScheme {
    val baseScheme = if (isDarkTheme) StealYourFaceDarkColorScheme else SunshineDaydreamLightColorScheme
    if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val systemDynamic = if (isDarkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        // Harmonize system Material 3 dynamic colors with the Steal Your Face lightning/crimson/cobalt identity
        return systemDynamic.copy(
            primary = lerp(systemDynamic.primary, baseScheme.primary, 0.45f),
            secondary = lerp(systemDynamic.secondary, baseScheme.secondary, 0.45f),
            tertiary = lerp(systemDynamic.tertiary, baseScheme.tertiary, 0.55f),
            background = lerp(systemDynamic.background, baseScheme.background, 0.65f),
            surface = lerp(systemDynamic.surface, baseScheme.surface, 0.65f),
            surfaceVariant = lerp(systemDynamic.surfaceVariant, baseScheme.surfaceVariant, 0.6f)
        )
    }
    return baseScheme
}

/**
 * Global Theme Provider for DeadSet.
 * Manages theme mode persistence, Material 3 Dynamic Color harmonization, reactive token updates,
 * and exposes [LocalDeadSetThemeController] to the entire Compose hierarchy.
 */
@Composable
fun DeadSetThemeProvider(
    initialThemeMode: DeadThemeMode? = null,
    initialDynamicColor: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val prefs = remember(context) {
        context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
    }

    var themeMode by remember {
        val savedModeName = prefs.getString(KEY_THEME_MODE, null)
        val startMode = initialThemeMode
            ?: savedModeName?.let { runCatching { DeadThemeMode.valueOf(it) }.getOrNull() }
            ?: DeadSetThemeState.themeMode
        mutableStateOf(startMode)
    }

    var useDynamicColor by remember {
        val startDynamic = initialDynamicColor
            ?: if (prefs.contains(KEY_DYNAMIC_COLOR)) prefs.getBoolean(KEY_DYNAMIC_COLOR, true) else DeadSetThemeState.useDynamicColor
        mutableStateOf(startDynamic)
    }

    val resolvedDarkTheme = when (themeMode) {
        DeadThemeMode.STEAL_YOUR_FACE_DARK -> true
        DeadThemeMode.SUNSHINE_LIGHT -> false
        DeadThemeMode.SYSTEM -> systemInDark
    }

    // Keep global snapshot state synchronized immediately so semantic tokens in Color.kt recompose
    DeadSetThemeState.themeMode = themeMode
    DeadSetThemeState.isDarkTheme = resolvedDarkTheme
    DeadSetThemeState.useDynamicColor = useDynamicColor

    val controller = remember(themeMode, resolvedDarkTheme, useDynamicColor) {
        DeadSetThemeController(
            themeMode = themeMode,
            isDarkTheme = resolvedDarkTheme,
            useDynamicColor = useDynamicColor,
            isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onToggleTheme = {
                val nextMode = if (resolvedDarkTheme) {
                    DeadThemeMode.SUNSHINE_LIGHT
                } else {
                    DeadThemeMode.STEAL_YOUR_FACE_DARK
                }
                themeMode = nextMode
                DeadSetThemeState.applyThemeMode(nextMode, systemInDark)
                prefs.edit().putString(KEY_THEME_MODE, nextMode.name).apply()
            },
            onSetThemeMode = { newMode ->
                themeMode = newMode
                DeadSetThemeState.applyThemeMode(newMode, systemInDark)
                prefs.edit().putString(KEY_THEME_MODE, newMode.name).apply()
            },
            onSetDynamicColor = { enabled ->
                useDynamicColor = enabled
                DeadSetThemeState.useDynamicColor = enabled
                prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
            }
        )
    }

    val colorScheme = remember(context, resolvedDarkTheme, useDynamicColor) {
        resolveDeadSetColorScheme(
            context = context,
            isDarkTheme = resolvedDarkTheme,
            useDynamicColor = useDynamicColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = StealieMidnight.toArgb()
                window.navigationBarColor = StealieMidnight.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !resolvedDarkTheme
                insetsController.isAppearanceLightNavigationBars = !resolvedDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDeadSetThemeController provides controller) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    DeadSetThemeProvider(content = content)
}

/**
 * Compact header button that toggles between the 'Steal Your Face' Dark Mode and 'Sunshine Daydream' Light Theme.
 */
@Composable
fun StealieQuickThemeToggleButton(
    modifier: Modifier = Modifier
) {
    val themeController = LocalDeadSetThemeController.current
    val isDark = themeController.isDarkTheme

    Surface(
        onClick = themeController.onToggleTheme,
        color = if (isDark) StealieCardElevated else StealieCardElevatedLightRaw,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) StealieGold.copy(alpha = 0.45f) else StealieCrimson.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .heightIn(min = 38.dp)
            .testTag("global_theme_toggle_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Filled.Bolt else Icons.Filled.WbSunny,
                contentDescription = if (isDark) {
                    "Switch to Sunshine Daydream Light Theme"
                } else {
                    "Switch to Steal Your Face Dark Mode"
                },
                tint = if (isDark) StealieGold else StealieCrimson,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isDark) "Stealie Dark" else "Light Mode",
                color = TextPrimaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Full interactive control card for switching between Light Theme, 'Steal Your Face' Dark Mode,
 * System Auto, and Material 3 Dynamic Color schemes.
 */
@Composable
fun StealieThemeControlCard(
    modifier: Modifier = Modifier
) {
    val themeController = LocalDeadSetThemeController.current
    val colorScheme = MaterialTheme.colorScheme

    Card(
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_provider_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(StealieCrimson, StealieBlue)
                                )
                            )
                            .border(1.5.dp, StealieGold, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (themeController.isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = "Theme Mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "STEAL YOUR FACE THEME & M3 PALETTE",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = themeController.themeMode.title,
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                FilledTonalButton(
                    onClick = themeController.onToggleTheme,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StealieGold.copy(alpha = 0.16f),
                        contentColor = StealieGold
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("profile_quick_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (themeController.isDarkTheme) Icons.Filled.WbSunny else Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (themeController.isDarkTheme) "Try Light" else "Stealie Dark",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = themeController.themeMode.subtitle,
                color = TextSecondaryDark,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // 3-Mode Selector Row (Steal Your Face Dark / Sunshine Light / System)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    Triple(DeadThemeMode.STEAL_YOUR_FACE_DARK, "⚡ Stealie Dark", "theme_mode_dark"),
                    Triple(DeadThemeMode.SUNSHINE_LIGHT, "☀️ Sunshine Light", "theme_mode_light"),
                    Triple(DeadThemeMode.SYSTEM, "🔄 System", "theme_mode_system")
                )

                for ((mode, label, tag) in modes) {
                    val selected = themeController.themeMode == mode
                    Surface(
                        onClick = { themeController.onSetThemeMode(mode) },
                        color = if (selected) StealieGold.copy(alpha = 0.2f) else StealieMidnight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) StealieGold else BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(tag)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) StealieGold else TextSecondaryDark,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Material 3 Dynamic Color Toggle + Live Swatches
            Surface(
                color = StealieMidnight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = StealieCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Material 3 Dynamic Color Scheme",
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (themeController.isDynamicColorSupported) {
                                    "Harmonizes Android 12+ wallpaper dynamic colors with Steal Your Face crimson, cobalt & lightning gold"
                                } else {
                                    "Uses signature Steal Your Face M3 tonal color scheme on this Android version"
                                },
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = themeController.useDynamicColor,
                            onCheckedChange = themeController.onSetDynamicColor,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StealieMidnight,
                                checkedTrackColor = StealieGold,
                                uncheckedThumbColor = TextSecondaryDark,
                                uncheckedTrackColor = StealieCardDark
                            ),
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    // Active M3 ColorScheme Swatch Preview Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val swatches = listOf(
                            "Primary" to colorScheme.primary,
                            "Secondary" to colorScheme.secondary,
                            "Tertiary" to colorScheme.tertiary,
                            "Surface" to colorScheme.surfaceVariant
                        )
                        for ((name, swatchColor) in swatches) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StealieCardDark)
                                    .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = name,
                                    color = TextSecondaryDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
