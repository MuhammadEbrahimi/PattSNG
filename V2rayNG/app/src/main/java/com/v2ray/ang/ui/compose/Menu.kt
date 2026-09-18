package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dropdown menu rows.
 *
 * Same contract as before - a list, a label resource, a selection callback - but each row now
 * carries the app's visual language: a tinted icon tile, a medium-weight label, and a red accent
 * for destructive entries. Behaviour is unchanged.
 */
@Composable
fun <T> AppDropdownMenuItems(
    items: List<T>,
    labelRes: (T) -> Int,
    onSelected: (T) -> Unit,
    iconRes: (T) -> Int? = { null },
    isDestructive: (T) -> Boolean = { false }
) {
    val scheme = MaterialTheme.colorScheme
    items.forEach { item ->
        val destructive = isDestructive(item)
        val accent = if (destructive) colorPingRed else scheme.secondary
        val icon = iconRes(item)
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(labelRes(item)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (destructive) colorPingRed else scheme.onSurface
                )
            },
            leadingIcon = if (icon != null) {
                {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accent
                        )
                    }
                }
            } else null,
            contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
            onClick = { onSelected(item) },
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(0.dp))
    }
}
