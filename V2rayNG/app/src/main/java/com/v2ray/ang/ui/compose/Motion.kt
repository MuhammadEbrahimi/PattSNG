package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * PattNG motion vocabulary.
 *
 * Every animation in the UI layer pulls its duration and easing from here, so the whole app
 * accelerates and settles the same way. Nothing in this file touches app state, the core, or the
 * service layer - it only describes how pixels move.
 */
object MotionTokens {
    /** Feedback that must feel instant: presses, ripples, tiny colour shifts. */
    const val QUICK_MS = 120

    /** The default: selection changes, colour and size transitions. */
    const val STANDARD_MS = 240

    /** Entrances and layout-level changes that deserve to be noticed. */
    const val EMPHASIZED_MS = 420

    /** One breath of the "connected" pulse. */
    const val PULSE_MS = 2_200

    /** One full turn of the orbiting ring around the connect button. */
    const val ORBIT_MS = 5_000

    /** Stagger between consecutive list items on first paint. */
    const val STAGGER_MS = 28

    /** Longest stagger delay, so long lists never feel slow. */
    const val STAGGER_MAX_MS = 260

    /**
     * Fast out, slow in - motion leaves immediately and eases into place. This is what makes the
     * animations read as "minimal" rather than bouncy.
     */
    val easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** The app's standard tween. Use this instead of hand-written specs. */
fun <T> appTween(
    durationMillis: Int = MotionTokens.STANDARD_MS,
    delayMillis: Int = 0,
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = MotionTokens.easing,
)

/**
 * Scales a composable slightly while it is pressed.
 *
 * The scale is applied in the draw layer, so layout and hit targets never change - the row cannot
 * shift under the finger.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = appTween(MotionTokens.QUICK_MS),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A 0f..1f ramp that repeats while [active].
 *
 * When inactive it returns a constant and never starts a clock, so an idle screen animates nothing
 * and costs nothing.
 */
@Composable
fun rememberPulse(active: Boolean, durationMillis: Int = MotionTokens.PULSE_MS): Float {
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseProgress",
    )
    return progress
}

/**
 * A 0f..1f progress that runs once, shortly after the composable first appears.
 *
 * Used for entrance animations. [delayMillis] staggers items so a list assembles itself instead of
 * snapping in all at once.
 */
@Composable
fun rememberEntrance(key: Any?, delayMillis: Int = 0): Float {
    var appeared by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { appeared = true }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = appTween(MotionTokens.EMPHASIZED_MS, delayMillis),
        label = "entrance",
    )
    return progress
}

/** Stagger delay for the item at [index], capped so long lists stay snappy. */
fun staggerDelay(index: Int): Int =
    (index.coerceAtLeast(0) * MotionTokens.STAGGER_MS).coerceAtMost(MotionTokens.STAGGER_MAX_MS)
