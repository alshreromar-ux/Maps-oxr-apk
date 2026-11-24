package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PinCategory
import com.example.model.QuickFilter
import com.example.ui.theme.PulseCoral

private val HomeWorkChipAccent = Color(0xFF0F9D8A)

@Composable
fun CategoryFilterBar(
    selectedFilter: QuickFilter,
    areSymbolsHidden: Boolean,
    savedPlacesCount: Int = 0,
    onSelectFilter: (QuickFilter) -> Unit,
    onToggleHideSymbols: () -> Unit,
    onOpenHomeWorkSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filters = buildList {
        add(QuickFilter.All)
        add(QuickFilter.Nearest)
        PinCategory.entries.forEach { add(QuickFilter.Category(it)) }
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "home_work_places_button") {
            Surface(
                onClick = onOpenHomeWorkSheet,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = HomeWorkChipAccent,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(
                    width = 1.2.dp,
                    color = HomeWorkChipAccent.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("open_home_work_button")
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(HomeWorkChipAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = stringResource(R.string.home_work_fab_cd),
                            tint = HomeWorkChipAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.home_work_fab_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (savedPlacesCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = HomeWorkChipAccent
                        ) {
                            Text(
                                text = "$savedPlacesCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        item(key = "toggle_hide_symbols") {
            val containerColor by animateColorAsState(
                targetValue = if (areSymbolsHidden) PulseCoral else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                animationSpec = tween(220),
                label = "hideSymbolsContainer"
            )
            val contentColor by animateColorAsState(
                targetValue = if (areSymbolsHidden) Color.White else PulseCoral,
                animationSpec = tween(220),
                label = "hideSymbolsContent"
            )

            Surface(
                onClick = onToggleHideSymbols,
                shape = RoundedCornerShape(24.dp),
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = if (areSymbolsHidden) 4.dp else 1.dp,
                shadowElevation = if (areSymbolsHidden) 8.dp else 3.dp,
                border = if (areSymbolsHidden) null else BorderStroke(
                    width = 1.dp,
                    color = PulseCoral.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("toggle_hide_symbols_chip")
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (areSymbolsHidden) Color.White.copy(alpha = 0.22f)
                                else PulseCoral.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (areSymbolsHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = if (areSymbolsHidden) Color.White else PulseCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (areSymbolsHidden) R.string.show_all_symbols else R.string.hide_all_symbols
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        items(
            items = filters,
            key = { filter ->
                when (filter) {
                    QuickFilter.All -> "all"
                    QuickFilter.Nearest -> "nearest"
                    is QuickFilter.Category -> filter.category.name
                }
            }
        ) { filter ->
            val isSelected = selectedFilter == filter
            val accent = filter.accentColor(MaterialTheme.colorScheme.primary)
            val tagKey = when (filter) {
                QuickFilter.All -> "all"
                QuickFilter.Nearest -> "nearest"
                is QuickFilter.Category -> filter.category.name.lowercase()
            }

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) accent else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                animationSpec = tween(220),
                label = "chipContainer"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(220),
                label = "chipContent"
            )

            Surface(
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(24.dp),
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = if (isSelected) 4.dp else 1.dp,
                shadowElevation = if (isSelected) 8.dp else 3.dp,
                border = if (isSelected) null else BorderStroke(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.24f)
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("filter_chip_$tagKey")
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                else accent.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = filter.icon(),
                            contentDescription = null,
                            tint = if (isSelected) Color.White else accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(filter.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
