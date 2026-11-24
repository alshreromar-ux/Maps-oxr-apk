package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.location.LocationHelper
import com.example.model.LatLngPoint
import com.example.model.SavedPlaceType
import com.example.model.WorldCatalog
import com.example.navigation.VoiceNavigator
import com.example.ui.components.AddPinSheet
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.CameraTargetRequest
import com.example.ui.components.CategoryFilterBar
import com.example.ui.components.MapStyleMode
import com.example.ui.components.NavigationBanner
import com.example.ui.components.NearbyPinsSheet
import com.example.ui.components.OsmMapView
import com.example.ui.components.PinDetailSheet
import com.example.ui.components.SavedPlacesSheet
import com.example.ui.components.SelectedPlaceCard
import com.example.ui.components.WorldPickerSheet
import com.example.ui.components.ZoomDeltaRequest
import com.example.ui.theme.CategoryPharmacyColor
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCoralEnd
import com.example.ui.theme.PulseOcean
import com.example.ui.theme.PulseTeal
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch

@Composable
fun CityPulseMapScreen(
    viewModel: CityPulseViewModel,
    onSignOut: () -> Unit,
    onGuestUpgradeToGoogle: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pinsState by viewModel.pinsState.collectAsStateWithLifecycle()
    val filteredPins by viewModel.filteredPins.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedPin by viewModel.selectedPin.collectAsStateWithLifecycle()
    val selectedPinUpdates by viewModel.selectedPinUpdates.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val activeWorldCity by viewModel.activeWorldCity.collectAsStateWithLifecycle()
    val isGlobalWorldOverview by viewModel.isGlobalWorldOverview.collectAsStateWithLifecycle()
    val detectedUserCityLabel by viewModel.detectedUserCityLabel.collectAsStateWithLifecycle()
    val globalSearchQuery by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val searchScope by viewModel.searchScope.collectAsStateWithLifecycle()
    val smartSearchResults by viewModel.smartSearchResults.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val highlightedSearchResult by viewModel.highlightedSearchResult.collectAsStateWithLifecycle()
    val isSearchingGlobalCities by viewModel.isSearchingGlobalCities.collectAsStateWithLifecycle()
    val globalSearchError by viewModel.globalSearchError.collectAsStateWithLifecycle()
    val resolvedPinCityName by viewModel.resolvedPinCityName.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val areSymbolsHidden by viewModel.areSymbolsHidden.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val activeRoute by viewModel.activeRoute.collectAsStateWithLifecycle()
    val selectedTravelMode by viewModel.selectedTravelMode.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val isLoadingRoute by viewModel.isLoadingRoute.collectAsStateWithLifecycle()
    val routeFocusNonce by viewModel.routeFocusNonce.collectAsStateWithLifecycle()
    val isVoiceMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
    val spokenAnnouncement by viewModel.spokenAnnouncement.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiReplyState by viewModel.aiReplyState.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val homePlace by viewModel.homePlace.collectAsStateWithLifecycle()
    val workPlace by viewModel.workPlace.collectAsStateWithLifecycle()
    val favoritePlaces by viewModel.favoritePlaces.collectAsStateWithLifecycle()
    val selectedMapPlace by viewModel.selectedMapPlace.collectAsStateWithLifecycle()
    val pendingHomeWorkPickType by viewModel.pendingHomeWorkPickType.collectAsStateWithLifecycle()

    val voiceNavigator = remember(context) { VoiceNavigator(context) }
    val isSpeaking by voiceNavigator.isSpeaking.collectAsStateWithLifecycle()

    DisposableEffect(voiceNavigator) {
        onDispose { voiceNavigator.shutdown() }
    }

    LaunchedEffect(isVoiceMuted) {
        voiceNavigator.setMuted(isVoiceMuted)
    }

    LaunchedEffect(spokenAnnouncement?.nonce) {
        val announcement = spokenAnnouncement ?: return@LaunchedEffect
        voiceNavigator.speak(announcement.text, flush = true)
    }

    var showAddPinSheet by remember { mutableStateOf(false) }
    var customDropLatLng by remember { mutableStateOf<LatLngPoint?>(null) }
    var showNearbyListSheet by remember { mutableStateOf(false) }
    var showWorldSheet by remember { mutableStateOf(false) }
    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var showSavedPlacesSheet by remember { mutableStateOf(false) }
    var initialSavedPlacesSlot by remember { mutableStateOf<SavedPlaceType?>(null) }
    var currentMapCenter by remember { mutableStateOf(WorldCatalog.WORLD_OVERVIEW_CENTER) }
    var cameraRequest by remember { mutableStateOf<CameraTargetRequest?>(null) }
    var zoomDeltaRequest by remember { mutableStateOf<ZoomDeltaRequest?>(null) }
    var mapStyleMode by remember { mutableStateOf(MapStyleMode.OSM_BORDERS) }

    BackHandler(enabled = selectedMapPlace != null || pendingHomeWorkPickType != null) {
        if (pendingHomeWorkPickType != null) {
            viewModel.startPickHomeWorkOnMap(null)
        } else {
            viewModel.clearSelectedMapPlace()
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            scope.launch {
                val loc = LocationHelper.fetchCurrentLocation(context)
                if (loc != null) {
                    viewModel.updateUserLocation(loc)
                    cameraRequest = CameraTargetRequest(loc, zoom = 14.8)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            val loc = LocationHelper.fetchCurrentLocation(context)
            if (loc != null) {
                viewModel.updateUserLocation(loc)
                cameraRequest = CameraTargetRequest(loc, zoom = 14.5)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Bleed OpenStreetMap with animated personal radar beacon
        OsmMapView(
            pins = filteredPins,
            selectedPinId = selectedPin?.pin?.id,
            userLocation = userLocation,
            cameraRequest = cameraRequest,
            zoomDeltaRequest = zoomDeltaRequest,
            mapStyleMode = mapStyleMode,
            activeRoute = activeRoute,
            routeFocusNonce = routeFocusNonce,
            highlightedSearchResult = highlightedSearchResult,
            savedPlaces = savedPlaces,
            selectedMapPlace = selectedMapPlace,
            onPinClick = { pinId -> viewModel.selectPin(pinId) },
            onSavedPlaceClick = { saved ->
                viewModel.selectSavedPlaceOnMap(saved)
                cameraRequest = CameraTargetRequest(
                    LatLngPoint(saved.latitude, saved.longitude),
                    zoom = 15.8
                )
            },
            onMapClick = { tappedPoint ->
                viewModel.onMapTapped(tappedPoint)
            },
            onMapLongPress = { droppedPoint ->
                customDropLatLng = droppedPoint
                viewModel.resolveCityForCoordinates(droppedPoint)
                showAddPinSheet = true
            },
            onMapCenterChanged = { center -> currentMapCenter = center },
            onUserLocationClick = {
                val loc = userLocation
                if (loc != null) {
                    cameraRequest = CameraTargetRequest(loc, zoom = 16.0)
                    viewModel.postMessage(
                        detectedUserCityLabel?.let { "موقعك الحالي: $it" } ?: "هذا هو موقعك الحالي"
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle atmospheric top gradient for crisp legibility over bright map tiles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Floating Top Bar, Global Search Trigger & Category Filter Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Top Brand & Account Controls Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 640.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PulseCoral, PulseCoralEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (viewModel.isGuestMode) {
                                    Surface(
                                        onClick = onGuestUpgradeToGoogle,
                                        shape = RoundedCornerShape(8.dp),
                                        color = PulseOcean.copy(alpha = 0.14f),
                                        modifier = Modifier.testTag("guest_upgrade_chip")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.guest_upgrade_google),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseOcean,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .alpha(pulseAlpha)
                                        .clip(CircleShape)
                                        .background(CategoryPharmacyColor)
                                )
                                val statusLine = if (viewModel.isGuestMode) {
                                    "${stringResource(R.string.guest_badge_label)} • ${filteredPins.size} نقطة"
                                } else {
                                    "مباشر • ${filteredPins.size} نقطة"
                                }
                                Text(
                                    text = statusLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.seedDemoPins() },
                            enabled = !isMutating,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("seed_demo_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = stringResource(R.string.load_demo_pins),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showNearbyListSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_nearby_list_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                    contentDescription = stringResource(R.string.pin_list_toggle_cd),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isGuestMode) Icons.Rounded.Person else Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Turn-by-Turn Voice Navigation HUD (replaces search & filter bars when navigating)
            NavigationBanner(
                route = activeRoute,
                selectedMode = selectedTravelMode,
                currentStepIndex = currentStepIndex,
                isLoadingRoute = isLoadingRoute,
                isVoiceMuted = isVoiceMuted,
                isSpeaking = isSpeaking,
                onSelectTravelMode = viewModel::switchTravelMode,
                onPrevStep = viewModel::prevRouteStep,
                onNextStep = viewModel::nextRouteStep,
                onRepeatVoice = viewModel::repeatCurrentStep,
                onToggleMute = viewModel::toggleVoiceMuted,
                onAskAiAboutRoute = {
                    showAiAssistantSheet = true
                    viewModel.sendAiPrompt("قيّم مساري الحالي واذكر لي أهم النصائح والخدمات أو التنبيهات القريبة.")
                },
                onStopNavigation = {
                    voiceNavigator.stop()
                    viewModel.stopNavigation()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Pick Home/Work on Map Instruction Banner
            AnimatedVisibility(visible = pendingHomeWorkPickType != null) {
                val pickType = pendingHomeWorkPickType
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 640.dp)
                        .align(Alignment.CenterHorizontally)
                        .testTag("pick_on_map_banner"),
                    shape = RoundedCornerShape(20.dp),
                    color = PulseTeal,
                    contentColor = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (pickType == SavedPlaceType.WORK) {
                                        R.string.pick_work_on_map_banner
                                    } else {
                                        R.string.pick_home_on_map_banner
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = { viewModel.startPickHomeWorkOnMap(null) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = activeRoute == null && !isLoadingRoute && pendingHomeWorkPickType == null) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    // Sleek Global Search & City Explorer Bar
                    Surface(
                        onClick = { showWorldSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .widthIn(max = 640.dp)
                            .heightIn(min = 50.dp)
                            .align(Alignment.CenterHorizontally)
                            .testTag("world_picker_button"),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        tonalElevation = 4.dp,
                        shadowElevation = 7.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = PulseTeal.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(PulseTeal.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = stringResource(R.string.world_picker_cd),
                                        tint = PulseTeal,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                val locationSummary = when {
                                    highlightedSearchResult != null ->
                                        "${highlightedSearchResult?.flagEmoji.orEmpty()}  ${highlightedSearchResult?.title.orEmpty()}"
                                    !isGlobalWorldOverview && activeWorldCity != null ->
                                        "${activeWorldCity?.flagEmoji.orEmpty()}  ${activeWorldCity?.cityName.orEmpty()}، ${activeWorldCity?.countryName.orEmpty()}"
                                    userLocation != null && !detectedUserCityLabel.isNullOrBlank() ->
                                        "📍  ${detectedUserCityLabel} • ${stringResource(R.string.search_bar_placeholder)}"
                                    else ->
                                        "🔍  ${stringResource(R.string.search_bar_placeholder)}"
                                }
                                Text(
                                    text = locationSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PulseOcean.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = PulseOcean,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "العالم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulseOcean,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    CategoryFilterBar(
                        selectedFilter = selectedFilter,
                        areSymbolsHidden = areSymbolsHidden,
                        savedPlacesCount = savedPlaces.size,
                        onSelectFilter = viewModel::setFilter,
                        onToggleHideSymbols = viewModel::toggleHideAllSymbols,
                        onOpenHomeWorkSheet = {
                            initialSavedPlacesSlot = null
                            showSavedPlacesSheet = true
                        }
                    )
                }
            }

            AnimatedVisibility(visible = !hasLocationPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.location_permission_banner),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.grant_location_permission),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Loading or Error Banner
        when (val state = pinsState) {
            is UiState.Loading -> {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = PulseOcean
                        )
                        Text(
                            text = "جارٍ تحميل نبض المدينة...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            is UiState.Error -> {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = { viewModel.seedDemoPins() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            is UiState.Success -> Unit
        }

        // 4. Bottom Floating Controls OR Selected Place Quick-Favorite Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            val tappedPlace = selectedMapPlace
            val showSelectedCard = tappedPlace != null && activeRoute == null && !isLoadingRoute && pendingHomeWorkPickType == null

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = if (showSelectedCard) 172.dp else 86.dp)
            )

            if (showSelectedCard && tappedPlace != null) {
                val isFav = favoritePlaces.any { saved ->
                    (!tappedPlace.sourcePinId.isNullOrBlank() && saved.sourcePinId == tappedPlace.sourcePinId) ||
                        com.example.model.calculateDistanceMeters(
                            saved.latitude,
                            saved.longitude,
                            tappedPlace.location.latitude,
                            tappedPlace.location.longitude
                        ) < 45f
                }
                SelectedPlaceCard(
                    place = tappedPlace,
                    userLocation = userLocation,
                    isFavorited = isFav,
                    onToggleFavorite = {
                        viewModel.toggleFavorite(
                            title = tappedPlace.title,
                            subtitle = tappedPlace.subtitle,
                            latitude = tappedPlace.location.latitude,
                            longitude = tappedPlace.location.longitude,
                            cityName = tappedPlace.cityName,
                            sourcePinId = tappedPlace.sourcePinId
                        )
                    },
                    onSetHome = {
                        viewModel.saveHomeOrWorkLocation(
                            type = SavedPlaceType.HOME,
                            title = "بيتي",
                            subtitle = tappedPlace.title,
                            latitude = tappedPlace.location.latitude,
                            longitude = tappedPlace.location.longitude,
                            cityName = tappedPlace.cityName
                        )
                    },
                    onSetWork = {
                        viewModel.saveHomeOrWorkLocation(
                            type = SavedPlaceType.WORK,
                            title = "عملي",
                            subtitle = tappedPlace.title,
                            latitude = tappedPlace.location.latitude,
                            longitude = tappedPlace.location.longitude,
                            cityName = tappedPlace.cityName
                        )
                    },
                    onStartDirections = {
                        viewModel.clearSelectedMapPlace()
                        viewModel.startDirectionsTo(
                            destination = tappedPlace.location,
                            destinationName = tappedPlace.title,
                            fallbackOrigin = currentMapCenter
                        )
                    },
                    onCreatePinHere = {
                        customDropLatLng = tappedPlace.location
                        viewModel.resolveCityForCoordinates(tappedPlace.location)
                        viewModel.clearSelectedMapPlace()
                        showAddPinSheet = true
                    },
                    onDismiss = { viewModel.clearSelectedMapPlace() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gemini AI Assistant Launcher Pill
                    Surface(
                        onClick = { showAiAssistantSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        shadowElevation = 9.dp,
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4F46E5), PulseTeal)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .testTag("ai_assistant_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.ai_assistant_fab),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Compact Map Drawing / Borders Style Switcher Pill
                    Surface(
                        onClick = { mapStyleMode = mapStyleMode.next() },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 7.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = PulseOcean.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("map_style_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = stringResource(R.string.map_style_cd),
                                tint = PulseOcean,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(mapStyleMode.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = { showNearbyListSheet = true },
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 8.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("nearby_pill_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                contentDescription = null,
                                tint = PulseTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "الأماكن القريبة",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${filteredPins.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Compact Zoom +/- Control Pill
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 7.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { zoomDeltaRequest = ZoomDeltaRequest(1.3) }
                                    .testTag("zoom_in_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.zoom_in_cd),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.width(28.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { zoomDeltaRequest = ZoomDeltaRequest(-1.3) }
                                    .testTag("zoom_out_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = stringResource(R.string.zoom_out_cd),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // One-Tap Full World Overview Button
                    SmallFloatingActionButton(
                        onClick = {
                            viewModel.selectGlobalWorldOverview()
                            cameraRequest = CameraTargetRequest(
                                WorldCatalog.WORLD_OVERVIEW_CENTER,
                                zoom = WorldCatalog.WORLD_OVERVIEW_ZOOM
                            )
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = PulseTeal,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("world_overview_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = stringResource(R.string.world_overview_cd)
                        )
                    }

                    // Sculpted Navigation Leaf-Squircle Badge for User Centering (distinct from circle dot)
                    val navShape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 6.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                    Surface(
                        onClick = {
                            if (hasLocationPermission) {
                                scope.launch {
                                    val loc = LocationHelper.fetchCurrentLocation(context)
                                    if (loc != null) {
                                        viewModel.updateUserLocation(loc)
                                        cameraRequest = CameraTargetRequest(loc, zoom = 15.6)
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = navShape,
                        color = Color.Transparent,
                        shadowElevation = 9.dp,
                        border = BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(navShape)
                            .background(
                                Brush.linearGradient(colors = listOf(PulseTeal, PulseOcean)),
                                shape = navShape
                            )
                            .testTag("my_location_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NearMe,
                                contentDescription = stringResource(R.string.my_location_cd),
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = stringResource(R.string.my_location_chip),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            customDropLatLng = null
                            viewModel.resolveCityForCoordinates(userLocation ?: currentMapCenter)
                            showAddPinSheet = true
                        },
                        shape = RoundedCornerShape(22.dp),
                        containerColor = PulseCoral,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
                        modifier = Modifier.testTag("add_pin_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddLocationAlt,
                            contentDescription = stringResource(R.string.add_pin_cd)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.add_pin_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    }

    // 5. Pin Detail Modal Bottom Sheet
    val currentSelected = selectedPin
    if (currentSelected != null) {
        val isPinFavorited = favoritePlaces.any { saved ->
            saved.sourcePinId == currentSelected.pin.id ||
                com.example.model.calculateDistanceMeters(
                    saved.latitude,
                    saved.longitude,
                    currentSelected.pin.latitude,
                    currentSelected.pin.longitude
                ) < 45f
        }
        PinDetailSheet(
            item = currentSelected,
            updates = selectedPinUpdates,
            currentUserId = viewModel.currentUserId,
            isMutating = isMutating,
            isFavorited = isPinFavorited,
            onDismiss = { viewModel.selectPin(null) },
            onToggleFavorite = {
                viewModel.toggleFavorite(
                    title = currentSelected.pin.title,
                    subtitle = currentSelected.pin.description.ifBlank { currentSelected.pin.cityName },
                    latitude = currentSelected.pin.latitude,
                    longitude = currentSelected.pin.longitude,
                    cityName = currentSelected.pin.cityName,
                    sourcePinId = currentSelected.pin.id
                )
            },
            onSetAsHome = {
                viewModel.saveHomeOrWorkLocation(
                    type = SavedPlaceType.HOME,
                    title = "بيتي",
                    subtitle = currentSelected.pin.title,
                    latitude = currentSelected.pin.latitude,
                    longitude = currentSelected.pin.longitude,
                    cityName = currentSelected.pin.cityName
                )
            },
            onSetAsWork = {
                viewModel.saveHomeOrWorkLocation(
                    type = SavedPlaceType.WORK,
                    title = "عملي",
                    subtitle = currentSelected.pin.title,
                    latitude = currentSelected.pin.latitude,
                    longitude = currentSelected.pin.longitude,
                    cityName = currentSelected.pin.cityName
                )
            },
            onStartDirections = {
                viewModel.startDirectionsTo(
                    destination = LatLngPoint(
                        currentSelected.pin.latitude,
                        currentSelected.pin.longitude
                    ),
                    destinationName = currentSelected.pin.title,
                    fallbackOrigin = currentMapCenter
                )
            },
            onConfirmCurrentStatus = {
                viewModel.updatePinStatus(
                    pinId = currentSelected.pin.id,
                    newStatus = currentSelected.pin.resolvedStatus(),
                    note = "",
                    incrementConfirmation = true
                )
            },
            onSubmitStatusUpdate = { newStatus, note ->
                viewModel.updatePinStatus(
                    pinId = currentSelected.pin.id,
                    newStatus = newStatus,
                    note = note,
                    incrementConfirmation = true
                )
            },
            onDeletePin = {
                viewModel.deletePin(currentSelected.pin.id)
            }
        )
    }

    // 6. Add Pin Modal Bottom Sheet
    if (showAddPinSheet) {
        val fallbackCity = stringResource(R.string.pin_city_default)
        AddPinSheet(
            gpsLatLng = userLocation,
            mapCenterLatLng = currentMapCenter,
            customDropLatLng = customDropLatLng,
            defaultCityName = resolvedPinCityName ?: activeWorldCity?.cityName ?: fallbackCity,
            isMutating = isMutating,
            onDismiss = {
                showAddPinSheet = false
                customDropLatLng = null
            },
            onCreatePin = { title, desc, category, status, city, targetLat, targetLng, photoBase64 ->
                viewModel.createPin(
                    title = title,
                    description = desc,
                    category = category,
                    status = status,
                    latitude = targetLat,
                    longitude = targetLng,
                    cityName = city,
                    photoBase64 = photoBase64,
                    onCreated = {
                        showAddPinSheet = false
                        customDropLatLng = null
                        cameraRequest = CameraTargetRequest(LatLngPoint(targetLat, targetLng), zoom = 15.5)
                    }
                )
            }
        )
    }

    // 7. Nearby Pins List Sheet
    if (showNearbyListSheet) {
        NearbyPinsSheet(
            pins = filteredPins,
            isMutating = isMutating,
            isPlaceFavorited = { lat, lng, sourcePinId ->
                favoritePlaces.any { saved ->
                    (!sourcePinId.isNullOrBlank() && saved.sourcePinId == sourcePinId) ||
                        com.example.model.calculateDistanceMeters(saved.latitude, saved.longitude, lat, lng) < 45f
                }
            },
            onToggleFavorite = { item ->
                viewModel.toggleFavorite(
                    title = item.pin.title,
                    subtitle = item.pin.description.ifBlank { item.pin.cityName },
                    latitude = item.pin.latitude,
                    longitude = item.pin.longitude,
                    cityName = item.pin.cityName,
                    sourcePinId = item.pin.id
                )
            },
            onDismiss = { showNearbyListSheet = false },
            onSelectPin = { item ->
                showNearbyListSheet = false
                viewModel.selectPin(item.pin.id)
                cameraRequest = CameraTargetRequest(
                    LatLngPoint(item.pin.latitude, item.pin.longitude),
                    zoom = 15.5
                )
            },
            onLoadDemoPins = {
                viewModel.seedDemoPins()
            }
        )
    }

    // 8. Universal Smart Search & World Explorer Sheet
    if (showWorldSheet) {
        WorldPickerSheet(
            activeCity = activeWorldCity,
            isOverviewMode = isGlobalWorldOverview,
            isMutating = isMutating,
            searchQuery = globalSearchQuery,
            searchScope = searchScope,
            smartResults = smartSearchResults,
            recentSearches = recentSearches,
            isSearchingRemote = isSearchingGlobalCities,
            remoteSearchError = globalSearchError,
            isResultFavorited = { result ->
                favoritePlaces.any { saved ->
                    (!result.pinId.isNullOrBlank() && saved.sourcePinId == result.pinId) ||
                        com.example.model.calculateDistanceMeters(
                            saved.latitude,
                            saved.longitude,
                            result.location.latitude,
                            result.location.longitude
                        ) < 45f
                }
            },
            onToggleFavoriteResult = { result ->
                viewModel.toggleFavorite(
                    title = result.title,
                    subtitle = result.subtitle,
                    latitude = result.location.latitude,
                    longitude = result.location.longitude,
                    cityName = result.subtitle.ifBlank { result.title },
                    sourcePinId = result.pinId
                )
            },
            onSearchQueryChange = viewModel::updateGlobalSearchQuery,
            onSearchScopeChange = viewModel::setSearchScope,
            onClearRecentSearches = viewModel::clearRecentSearches,
            onDismiss = { showWorldSheet = false },
            onSelectWorldOverview = {
                viewModel.selectGlobalWorldOverview()
                viewModel.clearHighlightedSearchResult()
                cameraRequest = CameraTargetRequest(
                    WorldCatalog.WORLD_OVERVIEW_CENTER,
                    zoom = WorldCatalog.WORLD_OVERVIEW_ZOOM
                )
                showWorldSheet = false
            },
            onSelectArabOverview = {
                viewModel.selectGlobalWorldOverview()
                viewModel.clearHighlightedSearchResult()
                cameraRequest = CameraTargetRequest(
                    WorldCatalog.ARAB_WORLD_OVERVIEW_CENTER,
                    zoom = WorldCatalog.ARAB_WORLD_OVERVIEW_ZOOM
                )
                showWorldSheet = false
            },
            onSelectSmartResult = { result ->
                viewModel.onSmartSearchResultFocused(result)
                cameraRequest = CameraTargetRequest(result.location, zoom = result.recommendedZoom)
                showWorldSheet = false
                viewModel.postMessage("${result.title} — اضغط النجمة لإضافته للمفضلة أو اتبع الاتجاهات")
            },
            onDirectionsToSmartResult = { result ->
                viewModel.onSmartSearchResultFocused(result)
                showWorldSheet = false
                viewModel.startDirectionsTo(
                    destination = result.location,
                    destinationName = result.title,
                    fallbackOrigin = currentMapCenter
                )
            },
            onSelectCity = { city ->
                viewModel.selectWorldCity(city)
                viewModel.clearHighlightedSearchResult()
                cameraRequest = CameraTargetRequest(city.location, zoom = 13.2)
                showWorldSheet = false
            },
            onDirectionsToCity = { city ->
                viewModel.selectWorldCity(city)
                viewModel.clearHighlightedSearchResult()
                showWorldSheet = false
                viewModel.startDirectionsTo(
                    destination = city.location,
                    destinationName = city.cityName,
                    fallbackOrigin = currentMapCenter
                )
            },
            onPopulateWorldPins = {
                viewModel.seedDemoPins()
                showWorldSheet = false
            }
        )
    }

    // 9. Firebase AI Logic Smart Assistant Sheet
    if (showAiAssistantSheet) {
        AiAssistantSheet(
            messages = aiMessages,
            replyState = aiReplyState,
            hasActiveRoute = activeRoute != null,
            isSpeaking = isSpeaking,
            onSendPrompt = viewModel::sendAiPrompt,
            onSpeakText = { text ->
                voiceNavigator.setMuted(false)
                voiceNavigator.speak(text, flush = true)
            },
            onStopSpeaking = { voiceNavigator.stop() },
            onDismiss = { showAiAssistantSheet = false }
        )
    }

    // 10. Home, Work & Favorites Management Sheet
    if (showSavedPlacesSheet) {
        val fallbackCity = stringResource(R.string.pin_city_default)
        SavedPlacesSheet(
            homePlace = homePlace,
            workPlace = workPlace,
            favoritePlaces = favoritePlaces,
            userLocation = userLocation,
            currentMapCenter = currentMapCenter,
            defaultCityName = detectedUserCityLabel ?: activeWorldCity?.cityName ?: fallbackCity,
            isMutating = isMutating,
            initialEditingSlot = initialSavedPlacesSlot,
            onSearchAddress = viewModel::searchAddressForSlot,
            onSaveHomeOrWork = { type, title, subtitle, lat, lng, city ->
                viewModel.saveHomeOrWorkLocation(
                    type = type,
                    title = title,
                    subtitle = subtitle,
                    latitude = lat,
                    longitude = lng,
                    cityName = city
                )
                cameraRequest = CameraTargetRequest(LatLngPoint(lat, lng), zoom = 15.6)
            },
            onStartPickOnMap = { slotType ->
                showSavedPlacesSheet = false
                viewModel.startPickHomeWorkOnMap(slotType)
            },
            onFocusPlaceOnMap = { place ->
                showSavedPlacesSheet = false
                viewModel.selectSavedPlaceOnMap(place)
                cameraRequest = CameraTargetRequest(
                    LatLngPoint(place.latitude, place.longitude),
                    zoom = 16.0
                )
            },
            onDirectionsToPlace = { place ->
                showSavedPlacesSheet = false
                viewModel.clearSelectedMapPlace()
                viewModel.startDirectionsTo(
                    destination = LatLngPoint(place.latitude, place.longitude),
                    destinationName = place.title,
                    fallbackOrigin = currentMapCenter
                )
            },
            onRemoveSavedPlace = viewModel::removeSavedPlace,
            onDismiss = {
                showSavedPlacesSheet = false
                initialSavedPlacesSlot = null
            }
        )
    }
}
