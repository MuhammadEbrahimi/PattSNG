package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The motion vocabulary of the app: a single easing curve and three durations, so every animation
 * in the interface moves the same way. Nothing here touches app behaviour; it only describes how a
 * change is drawn over time.
 */
object MotionTokens {
    /** A press, a tint, a colour swap: short enough to feel like a direct response. */
    const val QUICK_MS = 120

    /** The default for anything that moves or resizes. */
    const val STANDARD_MS = 220

    /** Entrances and exits, where the curve itself should be noticeable. */
    const val EMPHASIZED_MS = 420

    /** One full breath of the running indicator. */
    const val PULSE_MS = 2_000

    /** Decelerating: fast off the mark, settling softly, never bouncing. */
    val easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

fun <T> appTween(durationMillis: Int = MotionTokens.STANDARD_MS, delayMillis: Int = 0) =
    tween<T>(durationMillis = durationMillis, delayMillis = delayMillis, easing = MotionTokens.easing)

/**
 * Scales the element down slightly while it is held. The touch target and the layout are left
 * alone: only the drawing is scaled, so nothing shifts around it.
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
        label = "PressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A 0..1 ramp that restarts every [MotionTokens.PULSE_MS], or a flat 0 while [active] is false so
 * an idle screen animates nothing at all. Drawn as a ring that grows and fades out.
 */
@Composable
fun rememberPulse(active: Boolean): Float {
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "Pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MotionTokens.PULSE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "PulseProgress",
    )
    return progress
}
