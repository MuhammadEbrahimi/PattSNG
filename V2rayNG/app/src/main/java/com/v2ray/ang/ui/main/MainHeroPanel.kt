package com.v2ray.ang.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
 * This replaces the bottom bar. The two gestures it exposes are exactly the two the bottom bar had:
 * tapping the panel runs a connection test on the current server, tapping the dial starts or stops
 * the service. No new state, no new actions.
 */
@Composable
fun MainHeroPanel(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val panelInteraction = remember { MutableInteractionSource() }
    val entrance = rememberEntrance("hero")
    val pulse = rememberPulse(isRunning)
    val breath = breathOf(pulse)

    val accent by animateColorAsState(
        targetValue = if (isRunning) {
            colorFabActive
        } else {
            if (isDarkTheme) colorFabInactiveDark else colorFabInactiveLight
        },
        animationSpec = appTween(MotionTokens.EMPHASIZED_MS),
        label = "heroAccent"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, top = 6.dp, bottom = 10.dp)
            .graphicsLayer {
                alpha = entrance
                translationY = (1f - entrance) * 20.dp.toPx()
            }
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(
                    listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLowest)
                )
            )
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            .pressScale(panelInteraction, pressedScale = 0.99f)
            .clickable(
                interactionSource = panelInteraction,
                indication = null
            ) { onAction(MainAction.TestCurrentServer) }
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StateBadge(isRunning = isRunning, accent = accent, breath = breath)
                    Spacer(Modifier.height(14.dp))
                    // The status line the bottom bar used to show, unchanged in content.
                    Crossfade(
                        targetState = displayText,
                        animationSpec = appTween(MotionTokens.STANDARD_MS),
                        label = "heroStatus"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurface,
                            maxLines = 3
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                PowerDial(
                    isRunning = isRunning,
                    accent = accent,
                    pulse = pulse,
                    breath = breath,
                    onToggle = { onAction(MainAction.ToggleService) }
                )
            }
            Spacer(Modifier.height(16.dp))
            SignalBars(isRunning = isRunning, pulse = pulse, accent = accent)
        }
    }
}

/** Running/stopped capsule with a breathing dot. */
@Composable
private fun StateBadge(isRunning: Boolean, accent: Color, breath: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .graphicsLayer {
                    val scale = if (isRunning) 0.8f + 0.35f * breath else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(if (isRunning) colorPing else scheme.onSurfaceVariant)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

/** The start/stop control: a dial with a halo and an orbiting arc. */
@Composable
private fun PowerDial(
    isRunning: Boolean,
    accent: Color,
    pulse: Float,
    breath: Float,
    onToggle: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(118.dp)
            .pressScale(interaction, pressedScale = 0.93f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        // Halo: only drawn while connected, and it breathes with the pulse.
        if (isRunning) {
            Canvas(modifier = Modifier.size(118.dp)) {
                drawCircle(
                    color = accent.copy(alpha = 0.10f + 0.12f * breath),
                    radius = size.minDimension / 2f * (0.86f + 0.14f * breath)
                )
            }
        }
        // Static track plus the orbiting segment.
        Canvas(modifier = Modifier.size(104.dp)) {
            val stroke = 5.dp.toPx()
            drawCircle(
                color = scheme.onSurfaceVariant.copy(alpha = 0.18f),
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(width = stroke)
            )
            if (isRunning) {
                drawArc(
                    color = accent,
                    startAngle = -90f + pulse * 360f,
                    sweepAngle = 96f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accent, accent.copy(alpha = 0.72f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (isRunning) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp
                ),
                contentDescription = stringResource(
                    if (isRunning) R.string.acc_stop else R.string.acc_start
                ),
                modifier = Modifier.size(34.dp),
                tint = Color.White
            )
        }
    }
}

/** Decorative waveform along the bottom of the panel. */
@Composable
private fun SignalBars(isRunning: Boolean, pulse: Float, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val bars = 26
        val gap = size.width / (bars * 1.9f)
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val phase = (pulse + i / bars.toFloat()) % 1f
            val wave = breathOf(phase)
            val factor = if (isRunning) 0.28f + 0.72f * wave else 0.12f
            val barHeight = size.height * factor
            val left = i * (barWidth + gap)
            drawRoundRect(
                color = if (isRunning) {
                    accent.copy(alpha = 0.45f + 0.55f * wave)
                } else {
                    scheme.onSurfaceVariant.copy(alpha = 0.22f)
                },
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}
