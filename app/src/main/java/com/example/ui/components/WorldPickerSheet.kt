package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.WorldCatalog
import com.example.model.WorldCity
import com.example.model.WorldRegion
import com.example.model.formatDistanceArabic
import com.example.search.SearchResultKind
import com.example.search.SearchScopeFilter
import com.example.search.SmartSearchResult
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseOcean
import com.example.ui.theme.PulseTeal

private data class QuickSearchChip(
    val label: String,
    val query: String,
    val emoji: String
)

private val quickServiceChips = listOf(
    QuickSearchChip("صيدلية", "صيدلية", "💊"),
    QuickSearchChip("مطعم", "مطعم", "🍽️"),
    QuickSearchChip("مطار", "مطار", "✈️"),
    QuickSearchChip("مستشفى", "مستشفى", "🏥"),
    QuickSearchChip("مقهى", "مقهى", "☕"),
    QuickSearchChip("محطة وقود", "محطة وقود", "⛽"),
    QuickSearchChip("ميناء", "ميناء", "⚓")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldPickerSheet(
    activeCity: WorldCity?,
    isOverviewMode: Boolean,
    isMutating: Boolean,
    searchQuery: String,
    searchScope: SearchScopeFilter,
    smartResults: List<SmartSearchResult>,
    recentSearches: List<String>,
    isSearchingRemote: Boolean,
    remoteSearchError: String?,
    isResultFavorited: (SmartSearchResult) -> Boolean = { false },
    onToggleFavoriteResult: (SmartSearchResult) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSearchScopeChange: (SearchScopeFilter) -> Unit,
    onClearRecentSearches: () -> Unit,
    onDismiss: () -> Unit,
    onSelectWorldOverview: () -> Unit,
    onSelectArabOverview: () -> Unit,
    onSelectSmartResult: (SmartSearchResult) -> Unit,
    onDirectionsToSmartResult: (SmartSearchResult) -> Unit,
    onSelectCity: (WorldCity) -> Unit,
    onDirectionsToCity: (WorldCity) -> Unit,
    onPopulateWorldPins: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRegion by remember { mutableStateOf(WorldRegion.ALL) }

    val localDirectoryCities = remember(selectedRegion) {
        WorldCatalog.cities.filter { city ->
            selectedRegion == WorldRegion.ALL || city.region == selectedRegion
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // 1. Sleek Gradient Search & World Explorer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(colors = listOf(PulseTeal, PulseOcean))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TravelExplore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.world_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PulseTeal.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${WorldCatalog.cities.size}+",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTeal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.world_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close_cd)
                    )
                }
            }

            // 2. Super-Responsive Elevated Search Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = PulseOcean
                        )
                    },
                    trailingIcon = {
                        if (isSearchingRemote) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.2.dp,
                                color = PulseTeal
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.close_cd)
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_bar_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            smartResults.firstOrNull()?.let { onSelectSmartResult(it) }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = PulseTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("world_search_input")
                )

                AnimatedVisibility(visible = isSearchingRemote) {
                    LinearProgressIndicator(
                        color = PulseTeal,
                        trackColor = PulseTeal.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, start = 12.dp, end = 12.dp)
                            .clip(CircleShape)
                    )
                }
            }

            // 3. One-Tap Quick Service Search Chips (Pharmacies, Restaurants, Airports, Hospitals...)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = stringResource(R.string.search_quick_services_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickServiceChips, key = { it.label }) { chip ->
                        val active = searchQuery.trim() == chip.query
                        Surface(
                            onClick = { onSearchQueryChange(chip.query) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (active) PulseOcean else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                            border = BorderStroke(
                                1.dp,
                                if (active) PulseOcean else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(text = chip.emoji, fontSize = 14.sp)
                                Text(
                                    text = chip.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==================== MODE A: ACTIVE SMART SEARCH RESULTS ====================
            if (searchQuery.isNotBlank()) {
                // Scope Tabs: All / Places & Services / Cities / Airports & Ports / Community Pins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val scopeEntries = listOf(
                        SearchScopeFilter.ALL to R.string.search_scope_all,
                        SearchScopeFilter.PLACES to R.string.search_scope_places,
                        SearchScopeFilter.CITIES to R.string.search_scope_cities,
                        SearchScopeFilter.HUBS to R.string.search_scope_hubs,
                        SearchScopeFilter.COMMUNITY to R.string.search_scope_community
                    )
                    scopeEntries.forEach { (scope, labelRes) ->
                        val selected = searchScope == scope
                        Surface(
                            onClick = { onSearchScopeChange(scope) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) PulseTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("search_scope_${scope.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.search_results_count_fmt, smartResults.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseOcean,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSearchingRemote) {
                        Text(
                            text = stringResource(R.string.world_searching_global),
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTeal
                        )
                    }
                }

                if (!remoteSearchError.isNullOrBlank()) {
                    Text(
                        text = remoteSearchError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                if (smartResults.isEmpty() && !isSearchingRemote) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TravelExplore,
                            contentDescription = null,
                            tint = PulseOcean.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = stringResource(R.string.world_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp)
                    ) {
                        items(
                            items = smartResults,
                            key = { it.id }
                        ) { result ->
                            SmartSearchResultCard(
                                result = result,
                                distanceFormatted = result.distanceMeters?.let { formatDistanceArabic(context, it) },
                                isFavorited = isResultFavorited(result),
                                onToggleFavorite = { onToggleFavoriteResult(result) },
                                onSelect = { onSelectSmartResult(result) },
                                onDirections = { onDirectionsToSmartResult(result) }
                            )
                        }
                    }
                }
            } else {
                // ==================== MODE B: EMPTY QUERY EXPLORER (RECENT + QUICK WORLD + DIRECTORY) ====================
                if (recentSearches.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.search_recent_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onClearRecentSearches) {
                                Text(
                                    text = stringResource(R.string.search_clear_recent),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentSearches, key = { "recent_$it" }) { term ->
                                Surface(
                                    onClick = { onSearchQueryChange(term) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = term,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Jump Suggestions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.world_quick_picks_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = WorldCatalog.quickPickCities,
                            key = { "quick_${it.id}" }
                        ) { city ->
                            val isSelected = !isOverviewMode && activeCity?.id == city.id
                            Surface(
                                onClick = { onSelectCity(city) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) {
                                    PulseOcean
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PulseOcean else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.heightIn(min = 46.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = city.flagEmoji, fontSize = 16.sp)
                                    Text(
                                        text = city.cityName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Globe / Arab World / Populate World Action Tiles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorldQuickActionTile(
                        icon = Icons.Rounded.Language,
                        label = stringResource(R.string.world_overview_action),
                        accent = PulseOcean,
                        highlighted = isOverviewMode,
                        enabled = true,
                        onClick = onSelectWorldOverview,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("world_overview_button")
                    )
                    WorldQuickActionTile(
                        icon = Icons.Rounded.Explore,
                        label = stringResource(R.string.arab_world_overview_action),
                        accent = PulseTeal,
                        highlighted = false,
                        enabled = true,
                        onClick = onSelectArabOverview,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arab_world_overview_button")
                    )
                    WorldQuickActionTile(
                        icon = Icons.Rounded.AutoAwesome,
                        label = stringResource(R.string.populate_world_action),
                        accent = PulseCoral,
                        highlighted = false,
                        enabled = !isMutating,
                        onClick = onPopulateWorldPins,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("populate_world_pins_button")
                    )
                }

                // Continent / Region Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorldRegion.entries.forEach { region ->
                        val selected = selectedRegion == region
                        Surface(
                            onClick = { selectedRegion = region },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (selected) null else BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.heightIn(min = 46.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = region.emoji, fontSize = 14.sp)
                                Text(
                                    text = stringResource(region.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp)
                ) {
                    items(
                        items = localDirectoryCities,
                        key = { it.id }
                    ) { city ->
                        val isSelected = !isOverviewMode && activeCity?.id == city.id
                        val cat = city.sampleCategory
                        Card(
                            onClick = { onSelectCity(city) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("world_city_card_${city.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = city.flagEmoji, fontSize = 22.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = city.cityName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = city.countryName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = cat.color.copy(alpha = 0.14f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = cat.color,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = stringResource(cat.labelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cat.color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDirectionsToCity(city) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("world_city_directions_${city.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(PulseOcean.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.NearMe,
                                                contentDescription = stringResource(R.string.directions_short),
                                                tint = PulseOcean,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSearchResultCard(
    result: SmartSearchResult,
    distanceFormatted: String?,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit,
    onDirections: () -> Unit
) {
    val accent = when (result.kind) {
        SearchResultKind.COMMUNITY_PIN -> result.categoryHint?.color ?: PulseCoral
        SearchResultKind.AIRPORT -> Color(0xFF4F46E5)
        SearchResultKind.SEAPORT -> Color(0xFF0284C7)
        SearchResultKind.CITY_COUNTRY -> PulseOcean
        SearchResultKind.PLACE_POI -> result.categoryHint?.color ?: PulseTeal
    }
    val kindIcon: ImageVector? = when (result.kind) {
        SearchResultKind.COMMUNITY_PIN -> result.categoryHint?.icon ?: Icons.Rounded.Place
        SearchResultKind.AIRPORT -> Icons.Rounded.Flight
        SearchResultKind.SEAPORT -> Icons.Rounded.Anchor
        SearchResultKind.PLACE_POI -> result.categoryHint?.icon ?: Icons.Rounded.Place
        SearchResultKind.CITY_COUNTRY -> if (result.flagEmoji == "🌍") Icons.Rounded.LocationCity else null
    }

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_result_${result.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (kindIcon != null) {
                        Icon(
                            imageVector = kindIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(text = result.flagEmoji, fontSize = 22.sp)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (result.subtitle.isNotBlank()) {
                        Text(
                            text = result.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = result.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (!distanceFormatted.isNullOrBlank()) {
                            Text(
                                text = distanceFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("smart_result_favorite_${result.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(R.string.favorite_toggle_cd),
                        tint = if (isFavorited) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
                IconButton(
                    onClick = onDirections,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("smart_result_directions_${result.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PulseOcean),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NearMe,
                            contentDescription = stringResource(R.string.directions_short),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldQuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) accent else accent.copy(alpha = 0.12f)
        ),
        border = if (highlighted) null else BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier.heightIn(min = 50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) Color.White else accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) Color.White else accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
