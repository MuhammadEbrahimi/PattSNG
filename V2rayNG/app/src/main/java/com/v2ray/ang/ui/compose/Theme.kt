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
// Brand palette: iced cyan on deep blue
// ---------------------------------------------------------------------------
// Cyan is the action colour, deep blue carries structure, sky blue marks metadata.
// These are pinned even when Dynamic Color is on, so the app always looks like itself.
private val IceCyan = Color(0xFF22D3EE)
private val DeepBlue = Color(0xFF2563EB)
private val SkyBlue = Color(0xFF60A5FA)

private val LightColor = lightColorScheme(
    primary = Color(0xFF0B3A63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E9FA),
    onPrimaryContainer = Color(0xFF032037),
    secondary = IceCyan,
    onSecondary = Color(0xFF00252E),
    secondaryContainer = Color(0xFFCBF3FB),
    onSecondaryContainer = Color(0xFF002630),
    tertiary = DeepBlue,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E3FF),
    onTertiaryContainer = Color(0xFF00184A),
    error = Color(0xFFD01A4B),
    errorContainer = Color(0xFFFFDAE2),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF3F0013),
    background = Color(0xFFF1F5FA),
    onBackground = Color(0xFF0C1622),
    surface = Color(0xFFF1F5FA),
    onSurface = Color(0xFF0C1622),
    surfaceVariant = Color(0xFFE0E8F2),
    onSurfaceVariant = Color(0xFF4C5A6B),
    outline = Color(0xFF7D8B9C),
    outlineVariant = Color(0xFFCBD6E3),
    inverseSurface = Color(0xFF16212E),
    inverseOnSurface = Color(0xFFEEF3F9),
    inversePrimary = Color(0xFF9DC7EC),
    scrim = Color(0xFF000000),
    surfaceTint = DeepBlue,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFEDF2F8),
    surfaceContainerHigh = Color(0xFFE5ECF4),
    surfaceContainerHighest = Color(0xFFDDE6F0),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFBFE6F5),
    onPrimary = Color(0xFF07121F),
    primaryContainer = Color(0xFF16304A),
    onPrimaryContainer = Color(0xFFD6EEF9),
    secondary = IceCyan,
    onSecondary = Color(0xFF00222B),
    secondaryContainer = Color(0xFF07465A),
    onSecondaryContainer = Color(0xFFCBF3FB),
    tertiary = SkyBlue,
    onTertiary = Color(0xFF041B3D),
    tertiaryContainer = Color(0xFF123A79),
    onTertiaryContainer = Color(0xFFD8E3FF),
    error = Color(0xFFFF6E8A),
    errorContainer = Color(0xFF6E0022),
    onError = Color(0xFF39000F),
    onErrorContainer = Color(0xFFFFDAE2),
    // Deep navy canvas so the cyan actually reads as ice.
    background = Color(0xFF050A14),
    onBackground = Color(0xFFE4EEF7),
    surface = Color(0xFF050A14),
    onSurface = Color(0xFFE4EEF7),
    surfaceVariant = Color(0xFF223041),
    onSurfaceVariant = Color(0xFFA9BACC),
    outline = Color(0xFF6C7E92),
    outlineVariant = Color(0xFF243341),
    inverseSurface = Color(0xFFE4EEF7),
    inverseOnSurface = Color(0xFF07121F),
    inversePrimary = Color(0xFF07121F),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF03070E),
    surfaceContainerLow = Color(0xFF0A1220),
    surfaceContainer = Color(0xFF0E1827),
    surfaceContainerHigh = Color(0xFF142132),
    surfaceContainerHighest = Color(0xFF1B2B3E),
    surfaceTint = SkyBlue,
)

// Semantic Colors
val colorPing = Color(0xFF2DD4BF)
val colorPingRed = Color(0xFFFB7185)
val colorConfigType = SkyBlue
val colorFabActive = IceCyan
val colorFabInactiveLight = Color(0xFF93A3B5)
val colorFabInactiveDark = Color(0xFF44566B)
val dividerColorLight = Color(0xFFD5DFEA)
val dividerColorDark = Color(0xFF223041)

// Toast Colors
val toastNormalBgLight = Color(0xE6142132)
val toastNormalBgDark = Color(0xE61B2B3E)
val toastSuccessBg = Color(0xE6118C77)
val toastErrorBg = Color(0xE6C21C45)
val toastInfoBg = Color(0xE61D4ED8)
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

/** Dynamic Color may personalise neutrals, but brand accents stay pinned. */
private fun ColorScheme.withBrandAccents(): ColorScheme = copy(
    secondary = IceCyan,
    onSecondary = Color(0xFF00222B),
    tertiary = SkyBlue,
    onTertiary = Color(0xFF041B3D),
    surfaceTint = SkyBlue,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

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
