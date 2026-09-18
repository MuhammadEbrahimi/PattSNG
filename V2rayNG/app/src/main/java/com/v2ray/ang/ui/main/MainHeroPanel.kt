package com.v2ray.ang.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.MotionTokens
import com.v2ray.ang.ui.compose.appTween
import com.v2ray.ang.ui.compose.breathOf
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.colorFabInactiveDark
import com.v2ray.ang.ui.compose.colorFabInactiveLight
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.rememberEntrance
import com.v2ray.ang.ui.compose.rememberPulse

/**
 * The connection hero.
 *
 * This replaces the old bottom connection bar as the primary surface of the main screen, but the
 * contract is byte-for-byte the same: tapping the panel tests the current server, tapping the power
 * dial toggles the service. Everything else here is paint - a gradient slab, a segmented power
 * dial, an orbiting arc and a breathing halo that only run while the tunnel is up.
 */
@Composable
fun MainHeroPanel(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val panelInteraction = remember { MutableInteractionSource() }
    val dialInteraction = remember { MutableInteractionSource() }

    val accentTarget = if (isRunning) {
        colorFabActive
    } else if (isDarkTheme) {
        colorFabInactiveDark
    } else {
        colorFabInactiveLight
    }
    val accent by animateColorAsState(accentTarget, appTween(), label = "heroAccent")
    val pulse = rememberPulse(isRunning)
    val breath = if (isRunning) breathOf(pulse) else 0f
    val entrance = rememberEntrance("hero-panel")
    val liveness by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = appTween(MotionTokens.EMPHASIZED_MS),
        label = "heroLiveness"
    )

    val slabTop = MaterialTheme.colorScheme.surfaceContainerHigh
    val slabBottom = MaterialTheme.colorScheme.surfaceContainerLowest

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .graphicsLayer {
                alpha = entrance
                translationY = (1f - entrance) * 18.dp.toPx()
            }
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.verticalGradient(listOf(slabTop, slabBottom)))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.16f * liveness + 0.05f),
                        Color.Transparent,
                        accent.copy(alpha = 0.10f * liveness)
                    )
                )
            )
            .pressScale(panelInteraction, pressedScale = 0.994f)
            .clickable(
                interactionSource = panelInteraction,
                indication = null,
                onClick = { onAction(MainAction.TestCurrentServer) }
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.width(0.dp).weight(1f)) {
                StateBadge(isRunning = isRunning, accent = accent, breath = breath)
                Spacer(Modifier.height(12.dp))
                // The status line stays exactly the text the view model formats.
                Crossfade(
                    targetState = displayText,
                    animationSpec = appTween(MotionTokens.QUICK_MS),
                    label = "heroStatus"
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { contentDescription = text }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            PowerDial(
                isRunning = isRunning,
                accent = accent,
                pulse = pulse,
                breath = breath,
                interactionSource = dialInteraction,
                onClick = { onAction(MainAction.ToggleService) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SignalBars(accent = accent, liveness = liveness, pulse = pulse)
    }
}

/** Small uppercase capsule: the one-glance answer to "am I protected?". */
@Composable
private fun StateBadge(isRunning: Boolean, accent: Color, breath: Float) {
    val container by animateColorAsState(
        targetValue = if (isRunning) {
            accent.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = appTween(),
        label = "badgeBg"
    )
    val dot by animateColorAsState(
        targetValue = if (isRunning) colorPing else MaterialTheme.colorScheme.outline,
        animationSpec = appTween(),
        label = "badgeDot"
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp + 3.dp * breath)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = if (isRunning) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * The power dial: a large tappable disc wrapped in a static track, an orbiting arc and a halo.
 * It is the only element that toggles the service, exactly like the old FAB did.
 */
@Composable
private fun PowerDial(
    isRunning: Boolean,
    accent: Color,
    pulse: Float,
    breath: Float,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(118.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRunning) {
            Canvas(modifier = Modifier.size(118.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val minRadius = size.minDimension * 0.32f
                val maxRadius = size.minDimension * 0.5f
                drawCircle(
                    color = accent.copy(alpha = 0.20f * (1f - pulse)),
                    radius = minRadius + (maxRadius - minRadius) * pulse,
                    center = center
                )
            }
        }

        Canvas(modifier = Modifier.size(104.dp)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            drawCircle(
                color = accent.copy(alpha = 0.20f),
                radius = size.minDimension / 2f - inset,
                style = Stroke(width = stroke)
            )
            if (isRunning) {
                drawArc(
                    color = accent,
                    startAngle = pulse * 360f - 90f,
                    sweepAngle = 96f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(78.dp + 2.dp * breath)
                .pressScale(interactionSource, pressedScale = 0.90f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.78f))
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isRunning,
                animationSpec = appTween(MotionTokens.QUICK_MS),
                label = "dialIcon"
            ) { running ->
                Icon(
                    painter = painterResource(
                        if (running) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp
                    ),
                    contentDescription = stringResource(
                        if (running) R.string.acc_stop else R.string.acc_start
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/** A row of bars that rise while the tunnel is up - a wordless "traffic is flowing" readout. */
@Composable
private fun SignalBars(accent: Color, liveness: Float, pulse: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val bars = 26
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(1f)
        val radius = barWidth / 2f
        for (i in 0 until bars) {
            val phase = (pulse + i / bars.toFloat()) % 1f
            val wave = 0.35f + 0.65f * breathOf(phase)
            val idleHeight = size.height * 0.22f
            val height = idleHeight + (size.height - idleHeight) * wave * liveness
            val left = i * (barWidth + gap)
            drawRoundRect(
                color = accent.copy(alpha = 0.22f + 0.5f * liveness * wave),
                topLeft = Offset(left, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
}
