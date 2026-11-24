package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LatLngPoint
import com.example.model.SavedPlace
import com.example.model.SavedPlaceType
import com.example.model.calculateDistanceMeters
import com.example.model.formatDistanceArabic
import com.example.search.SmartSearchResult
import com.example.ui.theme.PulseOcean
import com.example.ui.theme.PulseTeal
import kotlinx.coroutines.delay

private val HomeAccent = Color(0xFF0F9D8A)
private val WorkAccent = Color(0xFF3B82F6)
private val FavoriteGold = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesSheet(
    homePlace: SavedPlace?,
    workPlace: SavedPlace?,
    favoritePlaces: List<SavedPlace>,
    userLocation: LatLngPoint?,
    currentMapCenter: LatLngPoint,
    defaultCityName: String,
    isMutating: Boolean,
    initialEditingSlot: SavedPlaceType? = null,
    onSearchAddress: suspend (String) -> List<SmartSearchResult>,
    onSaveHomeOrWork: (
        type: SavedPlaceType,
        title: String,
        subtitle: String,
        lat: Double,
        lng: Double,
        cityName: String
    ) -> Unit,
    onStartPickOnMap: (SavedPlaceType) -> Unit,
    onFocusPlaceOnMap: (SavedPlace) -> Unit,
    onDirectionsToPlace: (SavedPlace) -> Unit,
    onRemoveSavedPlace: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingSlot by remember(initialEditingSlot) { mutableStateOf(initialEditingSlot) }
    var customTitle by remember(editingSlot) {
        mutableStateOf(
            when (editingSlot) {
                SavedPlaceType.HOME -> homePlace?.title ?: "بيتي"
                SavedPlaceType.WORK -> workPlace?.title ?: "عملي"
                else -> ""
            }
        )
    }
    var addressQuery by remember(editingSlot) { mutableStateOf("") }
    var searchResults by remember(editingSlot) { mutableStateOf<List<SmartSearchResult>>(emptyList()) }
    var isSearchingAddress by remember(editingSlot) { mutableStateOf(false) }

    LaunchedEffect(addressQuery, editingSlot) {
        val trimmed = addressQuery.trim()
        if (editingSlot == null || trimmed.length < 2) {
            searchResults = emptyList()
            isSearchingAddress = false
            return@LaunchedEffect
        }
        isSearchingAddress = true
        delay(220L)
        searchResults = onSearchAddress(trimmed)
        isSearchingAddress = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp).let {
                PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp)
            },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                    Brush.linearGradient(colors = listOf(HomeAccent, WorkAccent))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.saved_places_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.saved_places_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
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
            }

            // 2. Home & Work Cards
            item(key = "home_slot") {
                HomeWorkSlotCard(
                    slotType = SavedPlaceType.HOME,
                    place = homePlace,
                    userLocation = userLocation,
                    isEditing = editingSlot == SavedPlaceType.HOME,
                    onEditClick = {
                        editingSlot = if (editingSlot == SavedPlaceType.HOME) null else SavedPlaceType.HOME
                    },
                    onDirections = { homePlace?.let(onDirectionsToPlace) },
                    onFocusMap = { homePlace?.let(onFocusPlaceOnMap) },
                    onRemove = { homePlace?.let { onRemoveSavedPlace(it.id) } }
                )
            }

            item(key = "work_slot") {
                HomeWorkSlotCard(
                    slotType = SavedPlaceType.WORK,
                    place = workPlace,
                    userLocation = userLocation,
                    isEditing = editingSlot == SavedPlaceType.WORK,
                    onEditClick = {
                        editingSlot = if (editingSlot == SavedPlaceType.WORK) null else SavedPlaceType.WORK
                    },
                    onDirections = { workPlace?.let(onDirectionsToPlace) },
                    onFocusMap = { workPlace?.let(onFocusPlaceOnMap) },
                    onRemove = { workPlace?.let { onRemoveSavedPlace(it.id) } }
                )
            }

            // 3. Inline Home / Work Location Chooser
            item(key = "slot_editor") {
                val activeSlot = editingSlot
                AnimatedVisibility(visible = activeSlot != null) {
                    if (activeSlot != null) {
                        val slotAccent = if (activeSlot == SavedPlaceType.HOME) HomeAccent else WorkAccent
                        val slotTitleLabel = stringResource(
                            if (activeSlot == SavedPlaceType.HOME) R.string.home_place_label else R.string.work_place_label
                        )
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = slotAccent.copy(alpha = 0.08f),
                            border = BorderStroke(1.2.dp, slotAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "تحديد موقع $slotTitleLabel",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = slotAccent
                                    )
                                    TextButton(onClick = { editingSlot = null }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }

                                OutlinedTextField(
                                    value = customTitle,
                                    onValueChange = { if (it.length <= 60) customTitle = it },
                                    label = { Text("الاسم المختصر (مثل: $slotTitleLabel)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_work_title_input")
                                )

                                // One-tap quick placement actions: GPS, Map Center, Pick on Map
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (userLocation != null) {
                                        Button(
                                            onClick = {
                                                onSaveHomeOrWork(
                                                    activeSlot,
                                                    customTitle.ifBlank { slotTitleLabel },
                                                    "موقعي الحالي • $defaultCityName",
                                                    userLocation.latitude,
                                                    userLocation.longitude,
                                                    defaultCityName
                                                )
                                                editingSlot = null
                                            },
                                            enabled = !isMutating,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = slotAccent,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("save_slot_gps_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MyLocation,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.save_current_gps_here),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onSaveHomeOrWork(
                                                    activeSlot,
                                                    customTitle.ifBlank { slotTitleLabel },
                                                    defaultCityName,
                                                    currentMapCenter.latitude,
                                                    currentMapCenter.longitude,
                                                    defaultCityName
                                                )
                                                editingSlot = null
                                            },
                                            enabled = !isMutating,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("save_slot_center_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LocationSearching,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = stringResource(R.string.save_map_center_here),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                editingSlot = null
                                                onStartPickOnMap(activeSlot)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("pick_slot_on_map_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.TouchApp,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = stringResource(R.string.pick_on_map_button),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Address Search for Home/Work
                                OutlinedTextField(
                                    value = addressQuery,
                                    onValueChange = { addressQuery = it },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = slotAccent
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSearchingAddress) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = slotAccent
                                            )
                                        } else if (addressQuery.isNotEmpty()) {
                                            IconButton(onClick = { addressQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Clear,
                                                    contentDescription = stringResource(R.string.close_cd)
                                                )
                                            }
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.search_address_for_slot_hint),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("slot_address_search_input")
                                )

                                if (searchResults.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        searchResults.take(5).forEach { result ->
                                            Surface(
                                                onClick = {
                                                    onSaveHomeOrWork(
                                                        activeSlot,
                                                        customTitle.ifBlank { slotTitleLabel },
                                                        listOf(result.title, result.subtitle)
                                                            .filter { it.isNotBlank() }
                                                            .distinct()
                                                            .joinToString("، "),
                                                        result.location.latitude,
                                                        result.location.longitude,
                                                        result.subtitle.ifBlank { result.title }
                                                    )
                                                    editingSlot = null
                                                    addressQuery = ""
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, slotAccent.copy(alpha = 0.2f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(text = result.flagEmoji)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = result.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (result.subtitle.isNotBlank()) {
                                                            Text(
                                                                text = result.subtitle,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "اختيار",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = slotAccent,
                                                        fontWeight = FontWeight.Bold
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
            }

            item(key = "fav_divider") {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // 4. Favorites Section Header
            item(key = "fav_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = FavoriteGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.favorites_section_title)} (${favoritePlaces.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (favoritePlaces.isEmpty()) {
                item(key = "fav_empty") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = FavoriteGold.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, FavoriteGold.copy(alpha = 0.24f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = FavoriteGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = stringResource(R.string.favorites_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = favoritePlaces,
                    key = { it.id }
                ) { fav ->
                    val dist = userLocation?.let {
                        calculateDistanceMeters(it.latitude, it.longitude, fav.latitude, fav.longitude)
                    }
                    Card(
                        onClick = { onFocusPlaceOnMap(fav) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                        ),
                        border = BorderStroke(1.dp, FavoriteGold.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("favorite_place_card_${fav.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(FavoriteGold.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = FavoriteGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fav.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = listOf(fav.subtitle, fav.cityName)
                                        .filter { it.isNotBlank() && it != fav.title }
                                        .distinct()
                                        .joinToString(" • ")
                                    if (sub.isNotBlank()) {
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (dist != null) {
                                        Text(
                                            text = formatDistanceArabic(context, dist),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseOcean,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onDirectionsToPlace(fav) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(PulseOcean),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NearMe,
                                            contentDescription = stringResource(R.string.directions_short),
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveSavedPlace(fav.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.remove_saved_place_cd),
                                        tint = MaterialTheme.colorScheme.error,
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
private fun HomeWorkSlotCard(
    slotType: SavedPlaceType,
    place: SavedPlace?,
    userLocation: LatLngPoint?,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onDirections: () -> Unit,
    onFocusMap: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val isHome = slotType == SavedPlaceType.HOME
    val accent = if (isHome) HomeAccent else WorkAccent
    val icon = if (isHome) Icons.Rounded.Home else Icons.Rounded.Work
    val defaultLabel = stringResource(if (isHome) R.string.home_place_label else R.string.work_place_label)
    val addActionLabel = stringResource(if (isHome) R.string.add_home_action else R.string.add_work_action)
    val dist = if (place != null && userLocation != null) {
        calculateDistanceMeters(userLocation.latitude, userLocation.longitude, place.latitude, place.longitude)
    } else {
        null
    }

    Card(
        onClick = { if (place != null) onFocusMap() else onEditClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            width = if (isEditing) 1.5.dp else 1.dp,
            color = accent.copy(alpha = if (place != null || isEditing) 0.45f else 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_work_slot_${slotType.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        .clip(RoundedCornerShape(15.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place?.title ?: defaultLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (place != null) {
                        val sub = place.subtitle.ifBlank { place.cityName }
                        Text(
                            text = if (dist != null) "$sub • ${formatDistanceArabic(context, dist)}" else sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "لم يتم التحديد بعد — اضغط للإضافة",
                            style = MaterialTheme.typography.bodySmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (place != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDirections,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("directions_${slotType.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PulseTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NearMe,
                                contentDescription = stringResource(R.string.directions_short),
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(if (isHome) R.string.edit_home_action else R.string.edit_work_action),
                            tint = accent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.remove_saved_place_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_${slotType.name.lowercase()}_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = addActionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
