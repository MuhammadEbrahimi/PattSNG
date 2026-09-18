package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * PattNG visual identity.
 *
 * Only colour, shape and type live here. No behaviour, no state, no navigation - swapping this
 * file changes how the app looks and nothing about how it works.
 */

private val BrandAmber = Color(0xFFFF7A18) // signature accent
private val BrandTeal = Color(0xFF00C08B) // healthy / connected
private val BrandViolet = Color(0xFF7C5CFF) // secondary emphasis

private val LightColor = lightColorScheme(
    primary = Color(0xFF111114),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E6EC),
    onPrimaryContainer = Color(0xFF111114),
    secondary = BrandAmber,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE6D2),
    onSecondaryContainer = Color(0xFF3A1600),
    tertiary = BrandViolet,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6DEFF),
    onTertiaryContainer = Color(0xFF1B0F52),
    error = Color(0xFFC4162B),
    errorContainer = Color(0xFFFFDAD9),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410006),
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF14141A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14141A),
    surfaceVariant = Color(0xFFE9E9F1),
    onSurfaceVariant = Color(0xFF4A4A56),
    outline = Color(0xFF7B7B87),
    outlineVariant = Color(0xFFD3D3DE),
    inverseSurface = Color(0xFF26262C),
    inverseOnSurface = Color(0xFFF3F3F7),
    inversePrimary = Color(0xFFC7C7D1),
    scrim = Color(0xFF000000),
    surfaceTint = BrandAmber,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F6FA),
    surfaceContainer = Color(0xFFF0F0F6),
    surfaceContainerHigh = Color(0xFFE9E9F1),
    surfaceContainerHighest = Color(0xFFE2E2EC),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFE8E8F2),
    onPrimary = Color(0xFF17171D),
    primaryContainer = Color(0xFF34343E),
    onPrimaryContainer = Color(0xFFE8E8F2),
    secondary = BrandAmber,
    onSecondary = Color(0xFF3A1600),
    secondaryContainer = Color(0xFF5E2800),
    onSecondaryContainer = Color(0xFFFFE6D2),
    tertiary = Color(0xFFB9A6FF),
    onTertiary = Color(0xFF23145F),
    tertiaryContainer = Color(0xFF3B2A84),
    onTertiaryContainer = Color(0xFFE6DEFF),
    error = Color(0xFFFFB3AE),
    errorContainer = Color(0xFF8E0010),
    onError = Color(0xFF5C0008),
    onErrorContainer = Color(0xFFFFDAD9),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFE7E7EF),
    surface = Color(0xFF0B0B0F),
    onSurface = Color(0xFFE7E7EF),
    surfaceVariant = Color(0xFF3A3A46),
    onSurfaceVariant = Color(0xFFC6C6D2),
    outline = Color(0xFF8A8A97),
    outlineVariant = Color(0xFF3A3A46),
    inverseSurface = Color(0xFFE7E7EF),
    inverseOnSurface = Color(0xFF0B0B0F),
    inversePrimary = Color(0xFF17171D),
    scrim = Color(0xFF000000),
    surfaceTint = BrandAmber,
    surfaceContainerLowest = Color(0xFF07070A),
    surfaceContainerLow = Color(0xFF111117),
    surfaceContainer = Color(0xFF16161D),
    surfaceContainerHigh = Color(0xFF1E1E26),
    surfaceContainerHighest = Color(0xFF272730),
)

// Semantic Colors
val colorPing = BrandTeal
val colorPingRed = Color(0xFFFF3B6B)
val colorConfigType = BrandAmber
val colorFabActive = BrandAmber
val colorFabInactiveLight = Color(0xFF9A9AA6)
val colorFabInactiveDark = Color(0xFF5A5A66)
val dividerColorLight = Color(0xFFD3D3DE)
val dividerColorDark = Color(0xFF32323C)

// Toast Colors 70%
val toastNormalBgLight = Color(0xB31E1E26)
val toastNormalBgDark = Color(0xB3323240)
val toastSuccessBg = Color(0xB3128A5E)
val toastErrorBg = Color(0xB3C4162B)
val toastInfoBg = Color(0xB34A32C8)
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
 * Larger, softer corners than Material defaults. Cards, sheets, dialogs and menus inherit these,
 * which is most of what gives the app its shape language.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Tighter tracking and a clearer weight hierarchy, layered on the Material scale so any style not
 * listed here keeps its default.
 */
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
        labelLarge = base.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
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
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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
