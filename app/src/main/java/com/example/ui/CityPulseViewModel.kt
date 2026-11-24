package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatMessage
import com.example.ai.CityPulseAiRepository
import com.example.data.CityPulseRepository
import com.example.location.LocationHelper
import com.example.model.CityPin
import com.example.model.LatLngPoint
import com.example.model.PinCategory
import com.example.model.PinStatus
import com.example.model.PinStatusUpdate
import com.example.model.PinWithDistance
import com.example.model.QuickFilter
import com.example.model.SavedPlace
import com.example.model.SavedPlaceType
import com.example.model.SelectedMapPlace
import com.example.model.TransportHubCatalog
import com.example.model.WorldCatalog
import com.example.model.WorldCity
import com.example.model.calculateDistanceMeters
import com.example.network.DirectionsRepository
import com.example.network.NavigationRoute
import com.example.network.TravelMode
import com.example.network.formatRouteDistanceAr
import com.example.network.formatRouteDurationAr
import com.example.network.GlobalGeocodingRepository
import com.example.search.ArabicSearchNormalizer
import com.example.search.LocalSmartSearchIndex
import com.example.search.SearchResultKind
import com.example.search.SearchScopeFilter
import com.example.search.SmartSearchResult
import com.example.ui.components.AiReplyUiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SpokenAnnouncement(
    val text: String,
    val nonce: Long = System.nanoTime()
)

class CityPulseViewModel(
    private val repository: CityPulseRepository,
    val currentUserId: String,
    private val currentUserDisplayName: String,
    val isGuestMode: Boolean = false,
    private val geocodingRepository: GlobalGeocodingRepository = GlobalGeocodingRepository(),
    private val directionsRepository: DirectionsRepository = DirectionsRepository(),
    private val aiRepository: CityPulseAiRepository = CityPulseAiRepository()
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<QuickFilter>(QuickFilter.All)
    val selectedFilter: StateFlow<QuickFilter> = _selectedFilter.asStateFlow()

    private val _userLocation = MutableStateFlow<LatLngPoint?>(null)
    val userLocation: StateFlow<LatLngPoint?> = _userLocation.asStateFlow()

    private val _selectedPinId = MutableStateFlow<String?>(null)
    val selectedPinId: StateFlow<String?> = _selectedPinId.asStateFlow()

    private val _activeWorldCity = MutableStateFlow<WorldCity?>(null)
    val activeWorldCity: StateFlow<WorldCity?> = _activeWorldCity.asStateFlow()

    private val _isGlobalWorldOverview = MutableStateFlow(true)
    val isGlobalWorldOverview: StateFlow<Boolean> = _isGlobalWorldOverview.asStateFlow()

    private val _detectedUserCityLabel = MutableStateFlow<String?>(null)
    val detectedUserCityLabel: StateFlow<String?> = _detectedUserCityLabel.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _remoteSmartResults = MutableStateFlow<List<SmartSearchResult>>(emptyList())

    private val _searchScope = MutableStateFlow(SearchScopeFilter.ALL)
    val searchScope: StateFlow<SearchScopeFilter> = _searchScope.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _highlightedSearchResult = MutableStateFlow<SmartSearchResult?>(null)
    val highlightedSearchResult: StateFlow<SmartSearchResult?> = _highlightedSearchResult.asStateFlow()

    private val _selectedMapPlace = MutableStateFlow<SelectedMapPlace?>(null)
    val selectedMapPlace: StateFlow<SelectedMapPlace?> = _selectedMapPlace.asStateFlow()

    private val _pendingHomeWorkPickType = MutableStateFlow<SavedPlaceType?>(null)
    val pendingHomeWorkPickType: StateFlow<SavedPlaceType?> = _pendingHomeWorkPickType.asStateFlow()

    private val _isSearchingGlobalCities = MutableStateFlow(false)
    val isSearchingGlobalCities: StateFlow<Boolean> = _isSearchingGlobalCities.asStateFlow()

    private val _globalSearchError = MutableStateFlow<String?>(null)
    val globalSearchError: StateFlow<String?> = _globalSearchError.asStateFlow()

    private val _resolvedPinCityName = MutableStateFlow<String?>(null)
    val resolvedPinCityName: StateFlow<String?> = _resolvedPinCityName.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _hiddenPinIds = MutableStateFlow<Set<String>>(emptySet())
    private val _areSymbolsHidden = MutableStateFlow(false)
    val areSymbolsHidden: StateFlow<Boolean> = _areSymbolsHidden.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Turn-by-Turn Navigation & Spoken Guidance State
    private val _activeRoute = MutableStateFlow<NavigationRoute?>(null)
    val activeRoute: StateFlow<NavigationRoute?> = _activeRoute.asStateFlow()

    private val _selectedTravelMode = MutableStateFlow(TravelMode.AUTO)
    val selectedTravelMode: StateFlow<TravelMode> = _selectedTravelMode.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _isLoadingRoute = MutableStateFlow(false)
    val isLoadingRoute: StateFlow<Boolean> = _isLoadingRoute.asStateFlow()

    private val _routeFocusNonce = MutableStateFlow(0L)
    val routeFocusNonce: StateFlow<Long> = _routeFocusNonce.asStateFlow()

    private val _isVoiceMuted = MutableStateFlow(false)
    val isVoiceMuted: StateFlow<Boolean> = _isVoiceMuted.asStateFlow()

    private val _spokenAnnouncement = MutableStateFlow<SpokenAnnouncement?>(null)
    val spokenAnnouncement: StateFlow<SpokenAnnouncement?> = _spokenAnnouncement.asStateFlow()

    // Firebase AI Logic Assistant State
    private val _aiMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "welcome",
                isUser = false,
                text = "أهلاً بك! أنا مساعد نبض المدينة الذكي. اسألني عن أقرب الصيدليات والمطاعم، حالة الازدحام والحوادث، أو اطلب نصائح لمسارك الحالي وسأقرأها لك بالصوت."
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _aiReplyState = MutableStateFlow<AiReplyUiState>(AiReplyUiState.Idle)
    val aiReplyState: StateFlow<AiReplyUiState> = _aiReplyState.asStateFlow()

    private var hasAttemptedAutoSeed = false
    private var globalSearchJob: Job? = null
    private var reverseLookupJob: Job? = null
    private var routeJob: Job? = null
    private var aiJob: Job? = null

    val savedPlaces: StateFlow<List<SavedPlace>> = repository.observeSavedPlaces()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing saved places", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val homePlace: StateFlow<SavedPlace?> = savedPlaces
        .map { places -> places.firstOrNull { it.resolvedType() == SavedPlaceType.HOME } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val workPlace: StateFlow<SavedPlace?> = savedPlaces
        .map { places -> places.firstOrNull { it.resolvedType() == SavedPlaceType.WORK } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val favoritePlaces: StateFlow<List<SavedPlace>> = savedPlaces
        .map { places -> places.filter { it.resolvedType() == SavedPlaceType.FAVORITE } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val pinsState: StateFlow<UiState<List<CityPin>>> = repository.observePins()
        .map<List<CityPin>, UiState<List<CityPin>>> { pins ->
            if (pins.isEmpty() && !hasAttemptedAutoSeed) {
                hasAttemptedAutoSeed = true
                seedStarterPinsInternal()
            }
            UiState.Success(pins)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing city pins", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "تعذّر تحميل النقاط من الخريطة"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredPins: StateFlow<List<PinWithDistance>> = combine(
        pinsState,
        _selectedFilter,
        _userLocation,
        _hiddenPinIds
    ) { state, filter, location, hiddenIds ->
        val allPins = (state as? UiState.Success)?.data ?: emptyList()
        val rawPins = if (hiddenIds.isEmpty()) allPins else allPins.filterNot { it.id in hiddenIds }
        val withDistance = rawPins.map { pin ->
            val ref = location ?: _activeWorldCity.value?.location ?: LocationHelper.DEFAULT_CITY_CENTER
            val meters = calculateDistanceMeters(ref.latitude, ref.longitude, pin.latitude, pin.longitude)
            PinWithDistance(pin = pin, distanceMeters = meters)
        }
        when (filter) {
            QuickFilter.All -> withDistance
            QuickFilter.Nearest -> withDistance.sortedBy { it.distanceMeters ?: Float.MAX_VALUE }
            is QuickFilter.Category -> withDistance.filter { it.pin.resolvedCategory() == filter.category }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val selectedPin: StateFlow<PinWithDistance?> = combine(
        filteredPins,
        pinsState,
        _selectedPinId,
        _userLocation
    ) { filtered, state, selectedId, location ->
        if (selectedId == null) return@combine null
        filtered.firstOrNull { it.pin.id == selectedId }
            ?: (state as? UiState.Success)?.data?.firstOrNull { it.id == selectedId }?.let { pin ->
                val ref = location ?: _activeWorldCity.value?.location ?: LocationHelper.DEFAULT_CITY_CENTER
                val meters = calculateDistanceMeters(ref.latitude, ref.longitude, pin.latitude, pin.longitude)
                PinWithDistance(pin, meters)
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPinUpdates: StateFlow<List<PinStatusUpdate>> = _selectedPinId
        .flatMapLatest { pinId ->
            if (pinId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeStatusUpdates(pinId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Error observing status updates for $pinId", firestoreError ?: error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val smartSearchResults: StateFlow<List<SmartSearchResult>> = combine(
        pinsState,
        _globalSearchQuery,
        _remoteSmartResults,
        _userLocation,
        _searchScope
    ) { state, query, remoteResults, userLoc, scope ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@combine emptyList()

        val refLoc = userLoc ?: _activeWorldCity.value?.location
        val allPins = (state as? UiState.Success)?.data ?: emptyList()

        val localMatches = LocalSmartSearchIndex.searchInstantLocal(
            query = trimmed,
            pins = allPins,
            cities = WorldCatalog.cities,
            airports = TransportHubCatalog.airports,
            seaports = TransportHubCatalog.seaports,
            referenceLocation = refLoc
        )

        val merged = mutableListOf<SmartSearchResult>()
        merged.addAll(localMatches)

        for (remote in remoteResults) {
            val dup = merged.any { existing ->
                val near = calculateDistanceMeters(
                    existing.location.latitude,
                    existing.location.longitude,
                    remote.location.latitude,
                    remote.location.longitude
                ) < 550f
                val sameTitle = ArabicSearchNormalizer.normalize(existing.title) ==
                    ArabicSearchNormalizer.normalize(remote.title)
                near && sameTitle
            }
            if (!dup) {
                merged.add(remote)
            }
        }

        val scoped = when (scope) {
            SearchScopeFilter.ALL -> merged
            SearchScopeFilter.PLACES -> merged.filter { it.kind == SearchResultKind.PLACE_POI || it.kind == SearchResultKind.COMMUNITY_PIN }
            SearchScopeFilter.CITIES -> merged.filter { it.kind == SearchResultKind.CITY_COUNTRY }
            SearchScopeFilter.HUBS -> merged.filter { it.kind == SearchResultKind.AIRPORT || it.kind == SearchResultKind.SEAPORT }
            SearchScopeFilter.COMMUNITY -> merged.filter { it.kind == SearchResultKind.COMMUNITY_PIN }
        }

        scoped.sortedByDescending { it.relevanceScore }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun setFilter(filter: QuickFilter) {
        _selectedFilter.value = filter
    }

    fun toggleHideAllSymbols() {
        if (_areSymbolsHidden.value) {
            _hiddenPinIds.value = emptySet()
            _areSymbolsHidden.value = false
            _actionMessage.value = "تم إظهار جميع الرموز على الخريطة"
        } else {
            val currentIds = (pinsState.value as? UiState.Success)?.data?.map { it.id }?.toSet() ?: emptySet()
            _hiddenPinIds.value = currentIds
            _areSymbolsHidden.value = true
            _selectedPinId.value = null
            _actionMessage.value = "تم إخفاء كل الرموز — يمكنك إضافة نقاطك الخاصة بحرية"
        }
    }

    fun updateUserLocation(latLng: LatLngPoint) {
        val isFirstFix = _userLocation.value == null
        _userLocation.value = latLng
        if (isFirstFix) {
            viewModelScope.launch {
                val resolved = geocodingRepository.resolveCityName(latLng.latitude, latLng.longitude)
                if (!resolved.isNullOrBlank()) {
                    _detectedUserCityLabel.value = resolved
                }
            }
        }
    }

    fun selectPin(pinId: String?) {
        _selectedPinId.value = pinId
        if (pinId != null) {
            _selectedMapPlace.value = null
            _highlightedSearchResult.value = null
        }
    }

    fun selectWorldCity(city: WorldCity) {
        _activeWorldCity.value = city
        _isGlobalWorldOverview.value = false
    }

    fun selectGlobalWorldOverview() {
        _activeWorldCity.value = null
        _isGlobalWorldOverview.value = true
    }

    fun postMessage(message: String) {
        _actionMessage.value = message
    }

    fun setSearchScope(scope: SearchScopeFilter) {
        _searchScope.value = scope
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun recordRecentSearch(term: String) {
        val clean = term.trim()
        if (clean.length < 2) return
        _recentSearches.value = (listOf(clean) + _recentSearches.value.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
    }

    fun onSmartSearchResultFocused(result: SmartSearchResult) {
        if (_globalSearchQuery.value.isNotBlank()) {
            recordRecentSearch(_globalSearchQuery.value)
        } else {
            recordRecentSearch(result.title)
        }
        if (result.pinId != null) {
            _selectedPinId.value = result.pinId
            _highlightedSearchResult.value = null
            _selectedMapPlace.value = null
        } else {
            if (result.worldCity != null) {
                _activeWorldCity.value = result.worldCity
                _isGlobalWorldOverview.value = false
            }
            _highlightedSearchResult.value = result
            _selectedMapPlace.value = SelectedMapPlace(
                title = result.title,
                subtitle = result.subtitle,
                cityName = result.subtitle.ifBlank { result.title },
                badge = result.badge,
                location = result.location,
                sourcePinId = null,
                isResolvingDetails = false
            )
        }
    }

    fun clearHighlightedSearchResult() {
        _highlightedSearchResult.value = null
    }

    fun startPickHomeWorkOnMap(type: SavedPlaceType?) {
        _pendingHomeWorkPickType.value = type
        if (type != null) {
            _selectedPinId.value = null
            _selectedMapPlace.value = null
        }
    }

    fun onMapTapped(point: LatLngPoint) {
        val pickSlot = _pendingHomeWorkPickType.value
        if (pickSlot != null) {
            _pendingHomeWorkPickType.value = null
            viewModelScope.launch {
                val details = geocodingRepository.resolvePlaceDetails(point.latitude, point.longitude)
                val defaultTitle = if (pickSlot == SavedPlaceType.HOME) "بيتي" else "عملي"
                val subtitle = details?.let {
                    listOf(it.title, it.subtitle).filter { s -> s.isNotBlank() }.distinct().joinToString("، ")
                }.orEmpty().ifBlank {
                    "(${String.format(java.util.Locale.US, "%.4f", point.latitude)}, ${String.format(java.util.Locale.US, "%.4f", point.longitude)})"
                }
                val city = details?.cityName ?: _activeWorldCity.value?.cityName ?: _detectedUserCityLabel.value ?: "مدينتي"
                saveHomeOrWorkLocation(
                    type = pickSlot,
                    title = defaultTitle,
                    subtitle = subtitle,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    cityName = city
                )
            }
            return
        }

        _selectedPinId.value = null
        _highlightedSearchResult.value = null
        val fallbackCity = _activeWorldCity.value?.cityName ?: _detectedUserCityLabel.value ?: "مدينتي"
        val coordsLabel = "${String.format(java.util.Locale.US, "%.4f", point.latitude)}, ${String.format(java.util.Locale.US, "%.4f", point.longitude)}"

        _selectedMapPlace.value = SelectedMapPlace(
            title = "جارٍ تحديد اسم المكان...",
            subtitle = coordsLabel,
            cityName = fallbackCity,
            badge = "موقع محدد",
            location = point,
            isResolvingDetails = true
        )

        reverseLookupJob?.cancel()
        reverseLookupJob = viewModelScope.launch {
            val details = geocodingRepository.resolvePlaceDetails(point.latitude, point.longitude)
            val currentLoc = _selectedMapPlace.value?.location
            if (currentLoc == point) {
                if (details != null) {
                    _selectedMapPlace.value = SelectedMapPlace(
                        title = details.title,
                        subtitle = details.subtitle.ifBlank { coordsLabel },
                        cityName = details.cityName,
                        badge = details.badge,
                        location = point,
                        isResolvingDetails = false
                    )
                } else {
                    _selectedMapPlace.value = SelectedMapPlace(
                        title = "موقع على الخريطة",
                        subtitle = "$fallbackCity ($coordsLabel)",
                        cityName = fallbackCity,
                        badge = "موقع محدد",
                        location = point,
                        isResolvingDetails = false
                    )
                }
            }
        }
    }

    fun selectSavedPlaceOnMap(place: SavedPlace) {
        _selectedPinId.value = null
        _highlightedSearchResult.value = null
        val badge = when (place.resolvedType()) {
            SavedPlaceType.HOME -> "بيتي"
            SavedPlaceType.WORK -> "عملي"
            SavedPlaceType.FAVORITE -> "مفضلة"
        }
        _selectedMapPlace.value = SelectedMapPlace(
            title = place.title,
            subtitle = place.subtitle.ifBlank { place.cityName },
            cityName = place.cityName,
            badge = badge,
            location = LatLngPoint(place.latitude, place.longitude),
            sourcePinId = place.sourcePinId,
            isResolvingDetails = false
        )
    }

    fun clearSelectedMapPlace() {
        reverseLookupJob?.cancel()
        _selectedMapPlace.value = null
        _highlightedSearchResult.value = null
    }

    fun isPlaceFavorited(
        latitude: Double,
        longitude: Double,
        sourcePinId: String? = null
    ): Boolean {
        return favoritePlaces.value.any { saved ->
            (!sourcePinId.isNullOrBlank() && saved.sourcePinId == sourcePinId) ||
                calculateDistanceMeters(saved.latitude, saved.longitude, latitude, longitude) < 45f
        }
    }

    fun saveHomeOrWorkLocation(
        type: SavedPlaceType,
        title: String,
        subtitle: String,
        latitude: Double,
        longitude: Double,
        cityName: String
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveHomeOrWork(
                type = type,
                title = title,
                subtitle = subtitle,
                latitude = latitude,
                longitude = longitude,
                cityName = cityName
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    val slotName = if (type == SavedPlaceType.HOME) "بيتي 🏠" else "عملي 💼"
                    _actionMessage.value = "تم حفظ موقع $slotName بنجاح"
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر حفظ الموقع"
                }
            )
        }
    }

    fun toggleFavorite(
        title: String,
        subtitle: String,
        latitude: Double,
        longitude: Double,
        cityName: String,
        sourcePinId: String? = null
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.toggleFavoritePlace(
                title = title,
                subtitle = subtitle,
                latitude = latitude,
                longitude = longitude,
                cityName = cityName,
                sourcePinId = sourcePinId,
                currentSavedPlaces = savedPlaces.value
            )
            _isMutating.value = false
            result.fold(
                onSuccess = { added ->
                    _actionMessage.value = if (added) {
                        "⭐ تمت إضافة «$title» إلى المفضلة"
                    } else {
                        "تمت إزالة «$title» من المفضلة"
                    }
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر تحديث المفضلة"
                }
            )
        }
    }

    fun removeSavedPlace(placeId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.removeSavedPlace(placeId)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _actionMessage.value = "تم حذف المكان المحفوظ"
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر حذف المكان"
                }
            )
        }
    }

    suspend fun searchAddressForSlot(query: String): List<SmartSearchResult> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        val bias = _userLocation.value ?: _activeWorldCity.value?.location
        val local = LocalSmartSearchIndex.searchInstantLocal(
            query = clean,
            pins = (pinsState.value as? UiState.Success)?.data ?: emptyList(),
            cities = WorldCatalog.cities,
            airports = TransportHubCatalog.airports,
            seaports = TransportHubCatalog.seaports,
            referenceLocation = bias
        ).take(5)
        val remote = geocodingRepository.searchGlobalPlaces(clean, bias).getOrDefault(emptyList())
        return (local + remote).distinctBy { it.title to it.location.latitude }.take(12)
    }

    fun updateGlobalSearchQuery(query: String) {
        _globalSearchQuery.value = query
        globalSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _remoteSmartResults.value = emptyList()
            _isSearchingGlobalCities.value = false
            _globalSearchError.value = null
            return
        }
        globalSearchJob = viewModelScope.launch {
            _isSearchingGlobalCities.value = true
            _globalSearchError.value = null
            delay(160L)
            val biasLoc = _userLocation.value ?: _activeWorldCity.value?.location
            val result = geocodingRepository.searchGlobalPlaces(trimmed, biasLocation = biasLoc)
            _isSearchingGlobalCities.value = false
            result.fold(
                onSuccess = { places ->
                    _remoteSmartResults.value = places
                },
                onFailure = { error ->
                    Log.w(TAG, "Global smart search failed", error)
                    _remoteSmartResults.value = emptyList()
                    _globalSearchError.value = "تعذّر البحث المباشر عبر الشبكة — يتم عرض النتائج المحفوظة والقريبة"
                }
            )
        }
    }

    fun resolveCityForCoordinates(point: LatLngPoint) {
        _resolvedPinCityName.value = _activeWorldCity.value?.cityName ?: _detectedUserCityLabel.value
        viewModelScope.launch {
            val resolved = geocodingRepository.resolveCityName(point.latitude, point.longitude)
            if (!resolved.isNullOrBlank()) {
                _resolvedPinCityName.value = resolved
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun updatePinStatus(
        pinId: String,
        newStatus: PinStatus,
        note: String,
        incrementConfirmation: Boolean = true
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updatePinStatus(
                pinId = pinId,
                newStatus = newStatus,
                note = note,
                authorName = currentUserDisplayName,
                incrementConfirmation = incrementConfirmation
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _actionMessage.value = "تم تحديث حالة المكان بنجاح"
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر تحديث الحالة"
                }
            )
        }
    }

    fun createPin(
        title: String,
        description: String,
        category: PinCategory,
        status: PinStatus,
        latitude: Double,
        longitude: Double,
        cityName: String,
        photoBase64: String?,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createPin(
                title = title,
                description = description,
                category = category,
                status = status,
                latitude = latitude,
                longitude = longitude,
                cityName = cityName,
                photoBase64 = photoBase64,
                authorName = currentUserDisplayName
            )
            _isMutating.value = false
            result.fold(
                onSuccess = { newId ->
                    if (_hiddenPinIds.value.contains(newId)) {
                        _hiddenPinIds.value = _hiddenPinIds.value - newId
                    }
                    _actionMessage.value = "تمت إضافة النقطة إلى الخريطة"
                    _selectedPinId.value = newId
                    onCreated(newId)
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر إضافة النقطة"
                }
            )
        }
    }

    fun deletePin(pinId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deletePin(pinId)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    if (_selectedPinId.value == pinId) {
                        _selectedPinId.value = null
                    }
                    _actionMessage.value = "تم حذف النقطة"
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر حذف النقطة"
                }
            )
        }
    }

    fun seedDemoPins() {
        viewModelScope.launch {
            _isMutating.value = true
            val center = _userLocation.value ?: _activeWorldCity.value?.location ?: LocationHelper.DEFAULT_CITY_CENTER
            val existingTitles = (pinsState.value as? UiState.Success)?.data
                ?.map { it.title }
                ?.toSet()
                ?: emptySet()
            val result = repository.seedDemoPins(
                centerLat = center.latitude,
                centerLng = center.longitude,
                authorName = currentUserDisplayName,
                existingTitles = existingTitles
            )
            _isMutating.value = false
            result.fold(
                onSuccess = { count ->
                    _hiddenPinIds.value = emptySet()
                    _areSymbolsHidden.value = false
                    _actionMessage.value = if (count > 0) {
                        "تم نشر $count نقطة عبر مدن العالم"
                    } else {
                        "جميع نقاط مدن العالم محمّلة بالفعل على الخريطة"
                    }
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "تعذّر تحميل البيانات التجريبية"
                }
            )
        }
    }

    fun startDirectionsTo(
        destination: LatLngPoint,
        destinationName: String,
        fallbackOrigin: LatLngPoint,
        preferredMode: TravelMode = TravelMode.AUTO
    ) {
        val origin = _userLocation.value ?: fallbackOrigin
        _selectedTravelMode.value = preferredMode
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            _isLoadingRoute.value = true
            _selectedPinId.value = null
            try {
                val route = directionsRepository.fetchRoute(
                    origin = origin,
                    destination = destination,
                    destinationName = destinationName,
                    preferredMode = preferredMode
                )
                _activeRoute.value = route
                _currentStepIndex.value = 0
                _routeFocusNonce.value = System.nanoTime()

                val firstStep = route.steps.firstOrNull()
                val durationStr = formatRouteDurationAr(route.totalDurationSeconds)
                val distStr = formatRouteDistanceAr(route.totalDistanceMeters)
                val modePrefix = when (route.resolvedMode) {
                    TravelMode.SEA -> "تم تجهيز المسار عبر الطريق والبحر بالسفينة إلى $destinationName."
                    TravelMode.AIR -> "تم تجهيز المسار عبر الطريق إلى المطار ثم الطائرة إلى $destinationName."
                    else -> "بدأت الاتجاهات عبر الطريق إلى $destinationName."
                }
                val overviewSpeech = buildString {
                    append("$modePrefix المسافة $distStr والوقت المتوقع $durationStr. ")
                    if (firstStep != null) {
                        append(firstStep.voiceAnnouncementAr)
                    }
                }
                emitSpeech(overviewSpeech)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start directions", e)
                _actionMessage.value = e.localizedMessage ?: "تعذّر حساب المسار حالياً"
            } finally {
                _isLoadingRoute.value = false
            }
        }
    }

    fun switchTravelMode(mode: TravelMode) {
        val current = _activeRoute.value ?: return
        startDirectionsTo(
            destination = current.destination,
            destinationName = current.destinationName,
            fallbackOrigin = current.origin,
            preferredMode = mode
        )
    }

    fun nextRouteStep() {
        val route = _activeRoute.value ?: return
        val next = (_currentStepIndex.value + 1).coerceAtMost(route.steps.lastIndex)
        if (next != _currentStepIndex.value) {
            _currentStepIndex.value = next
            route.steps.getOrNull(next)?.let { step ->
                emitSpeech(step.voiceAnnouncementAr)
            }
        }
    }

    fun prevRouteStep() {
        val route = _activeRoute.value ?: return
        val prev = (_currentStepIndex.value - 1).coerceAtLeast(0)
        if (prev != _currentStepIndex.value) {
            _currentStepIndex.value = prev
            route.steps.getOrNull(prev)?.let { step ->
                emitSpeech(step.voiceAnnouncementAr)
            }
        }
    }

    fun repeatCurrentStep() {
        val route = _activeRoute.value ?: return
        val step = route.steps.getOrNull(_currentStepIndex.value) ?: return
        emitSpeech(step.voiceAnnouncementAr)
    }

    fun toggleVoiceMuted() {
        val nextMuted = !_isVoiceMuted.value
        _isVoiceMuted.value = nextMuted
        if (!nextMuted) {
            repeatCurrentStep()
        }
    }

    fun stopNavigation() {
        routeJob?.cancel()
        _isLoadingRoute.value = false
        _activeRoute.value = null
        _currentStepIndex.value = 0
    }

    fun speakCustomText(text: String) {
        if (text.isNotBlank()) {
            _spokenAnnouncement.value = SpokenAnnouncement(text = text)
        }
    }

    private fun emitSpeech(text: String) {
        if (!_isVoiceMuted.value && text.isNotBlank()) {
            _spokenAnnouncement.value = SpokenAnnouncement(text = text)
        }
    }

    fun sendAiPrompt(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        aiJob?.cancel()
        val userMsg = AiChatMessage(
            id = "u_${UUID.randomUUID()}",
            isUser = true,
            text = cleanPrompt
        )
        val historySnapshot = _aiMessages.value
        _aiMessages.value = historySnapshot + userMsg

        val liveContext = buildLiveAiContext()

        aiJob = viewModelScope.launch {
            _aiReplyState.value = AiReplyUiState.Loading
            var finalReply = ""
            try {
                aiRepository.streamReply(
                    userPrompt = cleanPrompt,
                    liveContextSummary = liveContext,
                    history = historySnapshot
                ).collect { partial ->
                    finalReply = partial
                    _aiReplyState.value = AiReplyUiState.Streaming(partial)
                }

                if (finalReply.isNotBlank()) {
                    _aiMessages.value = _aiMessages.value + AiChatMessage(
                        id = "a_${UUID.randomUUID()}",
                        isUser = false,
                        text = finalReply
                    )
                    _aiReplyState.value = AiReplyUiState.Success
                } else {
                    _aiReplyState.value = AiReplyUiState.Error("لم تصل استجابة من الذكاء الاصطناعي.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI assistant request timed out", e)
                _aiReplyState.value = AiReplyUiState.Error("انتهت مهلة الاتصال بالذكاء الاصطناعي. حاول مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _aiReplyState.value = AiReplyUiState.Error(
                    e.localizedMessage ?: "تعذّر الاتصال بالمساعد الذكي حالياً."
                )
            }
        }
    }

    private fun buildLiveAiContext(): String {
        val userLoc = _userLocation.value
        val activeCity = _activeWorldCity.value
        val cityLabel = when {
            activeCity != null -> "${activeCity.cityName}، ${activeCity.countryName}"
            !_detectedUserCityLabel.value.isNullOrBlank() -> _detectedUserCityLabel.value!!
            else -> "نطاق الخريطة الحالية"
        }

        val nearestPins = filteredPins.value
            .sortedBy { it.distanceMeters ?: Float.MAX_VALUE }
            .take(10)
            .joinToString("\n") { item ->
                val pin = item.pin
                val distKm = item.distanceMeters?.let { String.format(java.util.Locale.US, "%.1f كم", it / 1000f) } ?: "غير محدد"
                "- ${pin.title} (${pin.cityName}) | التصنيف: ${pin.category} | الحالة: ${pin.status} | المسافة: $distKm"
            }

        val route = _activeRoute.value
        val routeText = if (route != null) {
            val step = route.steps.getOrNull(_currentStepIndex.value)
            buildString {
                appendLine("المسار النشط حالياً إلى: ${route.destinationName}")
                appendLine("المسافة الإجمالية: ${formatRouteDistanceAr(route.totalDistanceMeters)}، الزمن المتوقع: ${formatRouteDurationAr(route.totalDurationSeconds)}")
                if (step != null) {
                    appendLine("الخطوة الحالية (${_currentStepIndex.value + 1}/${route.steps.size}): ${step.instructionAr}")
                }
            }
        } else {
            "لا يوجد مسار ملاحة نشط حالياً."
        }

        val savedHome = homePlace.value
        val savedWork = workPlace.value
        val savedFavs = favoritePlaces.value.take(5)

        return buildString {
            appendLine("المدينة / الموقع النشط: $cityLabel")
            if (userLoc != null) {
                appendLine("إحداثيات GPS للمستخدم: ${userLoc.latitude}, ${userLoc.longitude}")
            }
            if (savedHome != null) {
                appendLine("موقع بيت المستخدم المحفوظ (${savedHome.title}): ${savedHome.subtitle} (${savedHome.latitude}, ${savedHome.longitude})")
            }
            if (savedWork != null) {
                appendLine("موقع عمل المستخدم المحفوظ (${savedWork.title}): ${savedWork.subtitle} (${savedWork.latitude}, ${savedWork.longitude})")
            }
            if (savedFavs.isNotEmpty()) {
                appendLine("الأماكن المفضلة للمستخدم: ${savedFavs.joinToString("، ") { it.title }}")
            }
            appendLine(routeText)
            appendLine("أبرز النقاط والبلاغات المسجلة على الخريطة:")
            append(if (nearestPins.isNotBlank()) nearestPins else "لا توجد نقاط مسجلة قريباً.")
        }
    }

    private fun seedStarterPinsInternal() {
        viewModelScope.launch {
            val center = _userLocation.value ?: LocationHelper.DEFAULT_CITY_CENTER
            repository.seedDemoPins(
                centerLat = center.latitude,
                centerLng = center.longitude,
                authorName = currentUserDisplayName
            )
        }
    }

    private companion object {
        const val TAG = "CityPulseVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
