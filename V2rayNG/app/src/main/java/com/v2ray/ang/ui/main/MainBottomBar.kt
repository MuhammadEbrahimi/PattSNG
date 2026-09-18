package com.v2ray.ang.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.MotionTokens
import com.v2ray.ang.ui.compose.appTween
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.colorFabInactiveDark
import com.v2ray.ang.ui.compose.colorFabInactiveLight
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.rememberPulse

/**
 * The connection bar.
 *
 * Same contract as before - tapping the bar tests the current server, tapping the button toggles
 * the service. Everything added here is presentation: a live status pill, an orbiting ring and a
 * breathing halo that only exist while the tunnel is up.
 */
@Composable
fun MainBottomBar(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onAction: (MainAction) -> Unit
) {
    val barInteraction = remember { MutableInteractionSource() }
    val fabInteraction = remember { MutableInteractionSource() }

    val accent = if (isRunning) colorFabActive else if (isDarkTheme) colorFabInactiveDark else colorFabInactiveLight
    val fabColor by animateColorAsState(accent, appTween(), label = "fabColor")
    val fabElevation by animateDpAsState(if (isRunning) 12.dp else 6.dp, appTween(), label = "fabElevation")

    // Morph between a circle (running) and a squircle (idle). Subtle, but it makes the button feel
    // like it changed state rather than just changed colour.
    val fabCorner by animateDpAsState(if (isRunning) 28.dp else 18.dp, appTween(MotionTokens.EMPHASIZED_MS), label = "fabCorner")

    val pulse = rememberPulse(isRunning)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = barInteraction,
                    indication = null,
                    onClick = { onAction(MainAction.TestCurrentServer) }
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // A hairline that lights up with the accent colour instead of a flat grey divider.
            ActiveHairline(accent = fabColor, isRunning = isRunning)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(start = 16.dp, end = 104.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                StatusPill(isRunning = isRunning, accent = fabColor, pulse = pulse)
                Spacer(Modifier.width(12.dp))
                // Cross-fade the status line so traffic counters tick over softly instead of
                // flickering between frames.
                Crossfade(
                    targetState = displayText,
                    animationSpec = appTween(MotionTokens.QUICK_MS),
                    label = "status"
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { contentDescription = text }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp)
                .offset(y = (-30).dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Two decorations, drawn only while running: an expanding halo (one breath per cycle)
            // and a ring whose gap orbits the button. Both are pure Canvas, no layout cost.
            if (isRunning) {
                ConnectionHalo(progress = pulse, color = fabColor)
                OrbitRing(progress = pulse, color = fabColor)
            }

            FloatingActionButton(
                onClick = { onAction(MainAction.ToggleService) },
                shape = RoundedCornerShape(fabCorner),
                containerColor = fabColor,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = fabElevation),
                interactionSource = fabInteraction,
                modifier = Modifier.pressScale(fabInteraction, pressedScale = 0.92f)
            ) {
                // Only the glyph swaps, so the button body never re-renders from scratch.
                Crossfade(
                    targetState = isRunning,
                    animationSpec = appTween(MotionTokens.QUICK_MS),
                    label = "fabIcon"
                ) { running ->
                    Icon(
                        painter = painterResource(
                            if (running) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp
                        ),
                        contentDescription = stringResource(
                            if (running) R.string.acc_stop else R.string.acc_start
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** Top edge of the bar: grey when idle, a soft accent gradient when the tunnel is up. */
@Composable
private fun ActiveHairline(accent: Color, isRunning: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = appTween(),
        label = "hairline"
    )
    val idle = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        idle,
                        accent.copy(alpha = 0.15f + 0.65f * alpha),
                        idle
                    )
                )
            )
    )
}

/** Small capsule showing connected / idle, with a dot that breathes while connected. */
@Composable
private fun StatusPill(isRunning: Boolean, accent: Color, pulse: Float) {
    val container by animateColorAsState(
        targetValue = if (isRunning) accent.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        animationSpec = appTween(),
        label = "pillColor"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isRunning) colorPing else MaterialTheme.colorScheme.outline,
        animationSpec = appTween(),
        label = "dotColor"
    )
    // 0 -> 1 -> 0 over one cycle, so the dot swells and settles instead of blinking.
    val breath = if (isRunning) 1f - kotlin.math.abs(pulse - 0.5f) * 2f else 0f

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp + 2.dp * breath)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isRunning) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** One expanding, fading ring per pulse cycle - the visual heartbeat of an active tunnel. */
@Composable
private fun ConnectionHalo(progress: Float, color: Color) {
    Canvas(modifier = Modifier.size(104.dp)) {
        val minRadius = size.minDimension * 0.28f
        val maxRadius = size.minDimension * 0.5f
        val radius = minRadius + (maxRadius - minRadius) * progress
        drawCircle(
            color = color.copy(alpha = 0.22f * (1f - progress)),
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

/** A thin ring with a rotating gap: reads as "traffic is flowing" without any text. */
@Composable
private fun OrbitRing(progress: Float, color: Color) {
    Canvas(modifier = Modifier.size(70.dp)) {
        val stroke = 2.5.dp.toPx()
        val inset = stroke / 2f
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = size.minDimension / 2f - inset,
            style = Stroke(width = stroke)
        )
        drawArc(
            color = color,
            startAngle = progress * 360f - 90f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke
            ),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
