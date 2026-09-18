package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.ui.compose.MotionTokens
import com.v2ray.ang.ui.compose.appTween
import com.v2ray.ang.ui.compose.pressScale
import kotlinx.coroutines.flow.StateFlow

/**
 * Subscription group chips.
 *
 * Behaviour is untouched: same groups, same indices, same click callback. Visually the tab row is
 * gone - groups are now standalone capsules that scroll horizontally, the active one filled with
 * the brand gradient and carrying its server count as a badge.
 */
@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = selectedTabIndex.coerceIn(0, groups.lastIndex)
    val listState = rememberLazyListState()

    // Keep the active chip on screen when the pager is swiped.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in groups.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        groups.forEachIndexed { index, group ->
            item(key = group.id.ifEmpty { "group-$index" }) {
                val serverFlow = remember(group.id, mainViewModel) {
                    mainViewModel.serversForGroup(group.id)
                }
                GroupChip(
                    group = group,
                    selected = index == selectedIndex,
                    serverFlow = serverFlow,
                    onClick = { onTabClick(index) }
                )
            }
        }
    }
}

@Composable
private fun GroupChip(
    group: GroupMapItem,
    selected: Boolean,
    serverFlow: StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val servers by serverFlow.collectAsStateWithLifecycle()
    val count = servers.size

    val interaction = remember { MutableInteractionSource() }
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = appTween(MotionTokens.STANDARD_MS),
        label = "chipEmphasis"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = appTween(),
        label = "chipLabel"
    )
    val idleContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val activeStart = MaterialTheme.colorScheme.secondary
    val activeEnd = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .graphicsLayer {
                val scale = 1f + 0.04f * emphasis
                scaleX = scale
                scaleY = scale
            }
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(idleContainer)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        activeStart.copy(alpha = emphasis),
                        activeEnd.copy(alpha = emphasis * 0.9f)
                    )
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.remarks,
            color = labelColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        if (group.id.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = labelColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}
