package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.appTween
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.verticalScrollbar

/**
 * Main header.
 *
 * Every control and every callback is the same as before: drawer, search, import menu, overflow
 * menu, loading indicator. The Material app bar is gone - the header is now a two-line title block
 * with floating capsule buttons, and search expands into a full-width capsule field in place.
 */
@Composable
fun MainTopBar(
    isLoading: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit
) {
    var showImportMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val importMenuScrollState = rememberScrollState()
    val moreMenuScrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp - statusBarHeight - navBarHeight - 20.dp
    val scheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSearch) {
                CapsuleButton(
                    iconRes = R.drawable.ic_arrow_back_24dp,
                    description = stringResource(R.string.acc_back),
                    onClick = onSearchClose
                )
                Spacer(Modifier.width(10.dp))
                // Search is a capsule field inline in the header, not a replaced app bar.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.menu_item_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                CapsuleButton(
                    iconRes = R.drawable.ic_menu_24dp,
                    description = stringResource(R.string.acc_open_menu),
                    onClick = onMenuClick,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PATTNG",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.title_server),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CapsuleButton(
                        iconRes = R.drawable.ic_search_24dp,
                        description = stringResource(R.string.acc_search),
                        onClick = { onSearchToggle(true) }
                    )
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        CapsuleButton(
                            iconRes = R.drawable.ic_add_24dp,
                            description = stringResource(R.string.acc_add),
                            onClick = { showImportMenu = true },
                            accent = true
                        )
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false },
                            scrollState = importMenuScrollState,
                            containerColor = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .heightIn(max = maxMenuHeight)
                                .verticalScrollbar(importMenuScrollState)
                        ) {
                            ImportMenuContent(
                                onAction = { action ->
                                    showImportMenu = false
                                    onAction(action)
                                }
                            )
                        }
                    }
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        CapsuleButton(
                            iconRes = R.drawable.ic_more_vert_24dp,
                            description = stringResource(R.string.acc_more),
                            onClick = { showMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            scrollState = moreMenuScrollState,
                            containerColor = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .heightIn(max = maxMenuHeight)
                                .verticalScrollbar(moreMenuScrollState)
                        ) {
                            MoreMenuContent { action ->
                                showMenu = false
                                onMoreMenuAction(action)
                            }
                        }
                    }
                }
            }
        }

        // Loading is a hairline under the header rather than a spinner inside it.
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(appTween()),
            exit = fadeOut(appTween())
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = scheme.secondary,
                trackColor = scheme.surfaceContainerHigh
            )
        }
    }
}

/** Header button: a floating capsule instead of a bare icon. */
@Composable
private fun CapsuleButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    shape: Shape = CircleShape
) {
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val fill = if (accent) {
        Brush.linearGradient(listOf(scheme.secondary, scheme.tertiary))
    } else {
        Brush.linearGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerHigh))
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(shape)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(22.dp),
            tint = if (accent) scheme.onSecondary else scheme.onSurface
        )
    }
}
