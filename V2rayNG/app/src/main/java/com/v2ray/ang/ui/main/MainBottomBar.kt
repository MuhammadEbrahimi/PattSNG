package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.MotionTokens
import com.v2ray.ang.ui.compose.appTween
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.colorFabInactiveDark
import com.v2ray.ang.ui.compose.colorFabInactiveLight
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.rememberPulse

@Composable
fun MainBottomBar(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onAction: (MainAction) -> Unit
) {
    val fabColor by animateColorAsState(
        targetValue = if (isRunning) colorFabActive
        else if (isDarkTheme) colorFabInactiveDark
        else colorFabInactiveLight,
        animationSpec = appTween(MotionTokens.STANDARD_MS),
        label = "FabColor"
    )
    val fabElevation by animateDpAsState(
        targetValue = if (isRunning) 10.dp else 6.dp,
        animationSpec = appTween(MotionTokens.STANDARD_MS),
        label = "FabElevation"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = { onAction(MainAction.TestCurrentServer) })
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            AppDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(isRunning = isRunning)
                    Spacer(modifier = Modifier.width(10.dp))
                    // The status is one line that keeps being replaced; sliding the old line out
                    // and the new one in makes the change readable instead of a silent swap.
                    AnimatedContent(
                        targetState = displayText,
                        transitionSpec = {
                            (slideInVertically(appTween(MotionTokens.STANDARD_MS)) { it / 2 } +
                                fadeIn(appTween(MotionTokens.STANDARD_MS))) togetherWith
                                (slideOutVertically(appTween(MotionTokens.STANDARD_MS)) { -it / 2 } +
                                    fadeOut(appTween(MotionTokens.QUICK_MS)))
                        },
                        label = "StatusText"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.semantics {
                                contentDescription = text
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp)
                .offset(y = (-28).dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            RunningHalo(isRunning = isRunning, color = fabColor)
            FloatingActionButton(
                onClick = { onAction(MainAction.ToggleService) },
                modifier = Modifier.pressScale(interactionSource, pressedScale = 0.92f),
                interactionSource = interactionSource,
                containerColor = fabColor,
                shape = CircleShape,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = fabElevation
                )
            ) {
                // The two states are one control, so the icons cross-fade in place rather than
                // the whole button being rebuilt.
                AnimatedContent(
                    targetState = isRunning,
                    transitionSpec = {
                        fadeIn(appTween(MotionTokens.QUICK_MS)) togetherWith
                            fadeOut(appTween(MotionTokens.QUICK_MS))
                    },
                    label = "FabIcon"
                ) { running ->
                    Icon(
                        painter = if (running) painterResource(R.drawable.ic_stop_24dp)
                        else painterResource(R.drawable.ic_play_24dp),
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

/** A dot that fills in and takes the accent colour while the tunnel is up. */
@Composable
private fun StatusDot(isRunning: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isRunning) colorFabActive else MaterialTheme.colorScheme.outline,
        animationSpec = appTween(MotionTokens.STANDARD_MS),
        label = "StatusDotColor"
    )
    val size by animateDpAsState(
        targetValue = if (isRunning) 10.dp else 8.dp,
        animationSpec = appTween(MotionTokens.STANDARD_MS),
        label = "StatusDotSize"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * A ring that grows out of the button and fades, once every pulse, only while the tunnel is up.
 * Idle, it is not composed at all, so a stopped app animates nothing.
 */
@Composable
private fun RunningHalo(isRunning: Boolean, color: Color) {
    val progress = rememberPulse(isRunning)
    if (!isRunning) return
    val diameter = 56.dp + (28.dp * progress)
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f * (1f - progress)))
    )
}
