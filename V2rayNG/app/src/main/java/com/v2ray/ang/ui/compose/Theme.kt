package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---------------------------------------------------------------------------
// Brand palette
// ---------------------------------------------------------------------------
// Amber is the action colour, Teal means "healthy", Violet is the cool accent used by
// containers and highlights. These three are never overridden - not even by Dynamic Color -
// so the app always looks like itself.
private val BrandAmber = Color(0xFFFF7A18)
private val BrandTeal = Color(0xFF00C08B)
private val BrandViolet = Color(0xFF7C5CFF)

private val LightColor = lightColorScheme(
    primary = Color(0xFF12121A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9E7F5),
    onPrimaryContainer = Color(0xFF12121A),
    secondary = BrandAmber,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE6D2),
    onSecondaryContainer = Color(0xFF3A1600),
    tertiary = BrandTeal,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFF3E2),
    onTertiaryContainer = Color(0xFF00241B),
    error = Color(0xFFD5003C),
    errorContainer = Color(0xFFFFDAE2),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF3F0013),
    background = Color(0xFFF6F5FA),
    onBackground = Color(0xFF12121A),
    surface = Color(0xFFF6F5FA),
    onSurface = Color(0xFF12121A),
    surfaceVariant = Color(0xFFE7E5F0),
    onSurfaceVariant = Color(0xFF55525F),
    outline = Color(0xFF8B8896),
    outlineVariant = Color(0xFFD5D2DF),
    inverseSurface = Color(0xFF1B1A22),
    inverseOnSurface = Color(0xFFF2F0F7),
    inversePrimary = Color(0xFFCFCBDC),
    scrim = Color(0xFF000000),
    surfaceTint = BrandViolet,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF9FD),
    surfaceContainer = Color(0xFFF1EFF7),
    surfaceContainerHigh = Color(0xFFEAE8F2),
    surfaceContainerHighest = Color(0xFFE3E0ED),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFEDEBF5),
    onPrimary = Color(0xFF15141B),
    primaryContainer = Color(0xFF2A2833),
    onPrimaryContainer = Color(0xFFEDEBF5),
    secondary = BrandAmber,
    onSecondary = Color(0xFF2A0F00),
    secondaryContainer = Color(0xFF5A2600),
    onSecondaryContainer = Color(0xFFFFE6D2),
    tertiary = BrandTeal,
    onTertiary = Color(0xFF00281E),
    tertiaryContainer = Color(0xFF00503C),
    onTertiaryContainer = Color(0xFFBFF3E2),
    error = Color(0xFFFF6E8A),
    errorContainer = Color(0xFF7A0025),
    onError = Color(0xFF3F0013),
    onErrorContainer = Color(0xFFFFDAE2),
    // Deep, almost-black canvas so the neon accents actually glow.
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFEDEBF5),
    surface = Color(0xFF0B0B0F),
    onSurface = Color(0xFFEDEBF5),
    surfaceVariant = Color(0xFF2B2A35),
    onSurfaceVariant = Color(0xFFB7B3C4),
    outline = Color(0xFF7A7788),
    outlineVariant = Color(0xFF32313D),
    inverseSurface = Color(0xFFEDEBF5),
    inverseOnSurface = Color(0xFF15141B),
    inversePrimary = Color(0xFF15141B),
    scrim = Color(0xFF000000),
    surfaceTint = BrandViolet,
    surfaceContainerLowest = Color(0xFF07070A),
    surfaceContainerLow = Color(0xFF101017),
    surfaceContainer = Color(0xFF15151E),
    surfaceContainerHigh = Color(0xFF1C1C27),
    surfaceContainerHighest = Color(0xFF242431),
)

// Semantic Colors
val colorPing = BrandTeal
val colorPingRed = Color(0xFFFF2D6F)
val colorConfigType = BrandViolet
val colorFabActive = BrandAmber
val colorFabInactiveLight = Color(0xFF9B98A8)
val colorFabInactiveDark = Color(0xFF5B5868)
val dividerColorLight = Color(0xFFDFDCE9)
val dividerColorDark = Color(0xFF2B2A35)

// Toast Colors 70%
val toastNormalBgLight = Color(0xE61B1A22)
val toastNormalBgDark = Color(0xE6262531)
val toastSuccessBg = Color(0xE600A173)
val toastErrorBg = Color(0xE6D5003C)
val toastInfoBg = Color(0xE65B3FD6)
val toastIconCircleBg = Color(0x33FFFFFF)
val toastTextColor = Color.White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

/**
 * Dynamic Color still personalises the neutrals from the wallpaper, but the brand accents are
 * pinned. Without this the app looked like a stock Material sample on Android 12+.
 */
private fun ColorScheme.withBrandAccents(): ColorScheme = copy(
    secondary = BrandAmber,
    onSecondary = Color.White,
    tertiary = BrandTeal,
    onTertiary = Color.White,
    surfaceTint = BrandViolet,
)

/**
 * Very large, soft corners. Cards, sheets, dialogs and menus all inherit these, which is most of
 * what makes the app read as a different product at a glance.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Tighter tracking and a heavier hierarchy on top of the Material scale. */
private val AppTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        titleMedium = base.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        bodyLarge = base.bodyLarge.copy(letterSpacing = (-0.1).sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = (-0.1).sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        ),
    )
}

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context).withBrandAccents()
            } else {
                dynamicLightColorScheme(context).withBrandAccents()
            }
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
