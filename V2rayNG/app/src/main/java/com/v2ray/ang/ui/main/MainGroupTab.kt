package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
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
 * Subscription groups as a rail of capsules.
 *
 * Behaviour is untouched: same groups, same indices, same click callback. The tab row and its
 * underline are gone; each group is now a standalone chip that fills with the brand gradient when
 * it is the active one, and the rail scrolls itself to keep that chip in view.
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

    // Follow the pager: swiping to a group brings its chip into view.
    LaunchedEffect(selectedIndex, groups.size) {
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
        itemsIndexed(items = groups, key = { _, group -> group.id }) { index, group ->
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

@Composable
private fun GroupChip(
    group: GroupMapItem,
    selected: Boolean,
    serverFlow: StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val servers by serverFlow.collectAsStateWithLifecycle()
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme

    val labelColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondary else scheme.onSurfaceVariant,
        animationSpec = appTween(),
        label = "chipLabel"
    )
    val idleColor by animateColorAsState(
        targetValue = if (selected) {
            scheme.secondary
        } else {
            scheme.surfaceContainerHigh
        },
        animationSpec = appTween(),
        label = "chipFill"
    )
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = appTween(MotionTokens.STANDARD_MS),
        label = "chipEmphasis"
    )

    // Active chip carries a cyan to blue wash; inactive ones stay a flat neutral capsule.
    val fill = if (selected) {
        Brush.horizontalGradient(listOf(scheme.secondary, scheme.tertiary))
    } else {
        Brush.horizontalGradient(listOf(idleColor, idleColor))
    }

    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .graphicsLayer {
                val scale = 1f + 0.03f * emphasis
                scaleX = scale
                scaleY = scale
            }
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(fill)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.remarks,
            color = labelColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        // Server count lives inside the chip instead of inside the label text.
        if (group.id.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            scheme.onSecondary.copy(alpha = 0.18f)
                        } else {
                            scheme.onSurfaceVariant.copy(alpha = 0.14f)
                        }
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = servers.size.toString(),
                    color = labelColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
