package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
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
import com.v2ray.ang.ui.compose.MotionTokens
import com.v2ray.ang.ui.compose.appTween
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.verticalScrollbar

/**
 * Main screen header: two-line title, capsule buttons, inline capsule search and a hairline
 * progress bar. The dropdown menus use the app's rounded, bordered container instead of the
 * stock Material sheet.
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapsuleButton(
                iconRes = if (showSearch) R.drawable.ic_arrow_back_24dp else R.drawable.ic_menu_24dp,
                description = stringResource(if (showSearch) R.string.acc_back else R.string.acc_open_menu),
                onClick = if (showSearch) onSearchClose else onMenuClick,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.width(12.dp))
            if (showSearch) {
                SearchCapsule(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PATTNG",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.title_server),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }
                CapsuleButton(
                    iconRes = R.drawable.ic_search_24dp,
                    description = stringResource(R.string.acc_search),
                    onClick = { onSearchToggle(true) }
                )
                Spacer(Modifier.width(6.dp))
            }
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                CapsuleButton(
                    iconRes = R.drawable.ic_add_24dp,
                    description = stringResource(R.string.acc_add),
                    onClick = { showImportMenu = true },
                    accent = true
                )
                AppMenuContainer(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false },
                    maxHeight = maxMenuHeight,
                    scrollModifier = Modifier.verticalScrollbar(importMenuScrollState),
                    scrollState = importMenuScrollState
                ) {
                    ImportMenuContent(
                        onAction = { action ->
                            showImportMenu = false
                            onAction(action)
                        }
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                CapsuleButton(
                    iconRes = R.drawable.ic_more_vert_24dp,
                    description = stringResource(R.string.acc_more),
                    onClick = { showMenu = true }
                )
                AppMenuContainer(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    maxHeight = maxMenuHeight,
                    scrollModifier = Modifier.verticalScrollbar(moreMenuScrollState),
                    scrollState = moreMenuScrollState
                ) {
                    MoreMenuContent { action ->
                        showMenu = false
                        onMoreMenuAction(action)
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(appTween(MotionTokens.QUICK_MS)),
            exit = fadeOut(appTween(MotionTokens.QUICK_MS))
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = scheme.secondary,
                trackColor = scheme.surfaceContainerHigh
            )
        }
    }
}

@Composable
private fun AppMenuContainer(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    maxHeight: androidx.compose.ui.unit.Dp,
    scrollModifier: Modifier,
    scrollState: androidx.compose.foundation.ScrollState,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        scrollState = scrollState,
        containerColor = scheme.surfaceContainerHigh,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.22f)),
        modifier = Modifier
            .heightIn(max = maxHeight)
            .then(scrollModifier)
    ) {
        content()
    }
}

@Composable
private fun CapsuleButton(
    iconRes: Int,
    description: String?,
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
            .clickable(interactionSource = interaction, indication = null) { onClick() },
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

@Composable
private fun SearchCapsule(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.secondary.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_24dp),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = scheme.secondary
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.secondary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.menu_item_search),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
