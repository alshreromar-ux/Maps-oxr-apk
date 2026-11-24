package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.HubKind
import com.example.model.LatLngPoint
import com.example.model.PinCategory
import com.example.model.PinWithDistance
import com.example.model.SavedPlace
import com.example.model.SavedPlaceType
import com.example.model.SelectedMapPlace
import com.example.model.WorldCatalog
import com.example.network.NavigationRoute
import com.example.network.RouteSegmentType
import com.example.network.TravelMode
import com.example.search.SmartSearchResult
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

data class CameraTargetRequest(
    val point: LatLngPoint,
    val zoom: Double = 15.0,
    val nonce: Long = System.nanoTime()
)

data class ZoomDeltaRequest(
    val delta: Double,
    val nonce: Long = System.nanoTime()
)

@Composable
fun OsmMapView(
    pins: List<PinWithDistance>,
    selectedPinId: String?,
    userLocation: LatLngPoint?,
    cameraRequest: CameraTargetRequest?,
    zoomDeltaRequest: ZoomDeltaRequest? = null,
    mapStyleMode: MapStyleMode = MapStyleMode.OSM_BORDERS,
    activeRoute: NavigationRoute? = null,
    routeFocusNonce: Long = 0L,
    highlightedSearchResult: SmartSearchResult? = null,
    savedPlaces: List<SavedPlace> = emptyList(),
    selectedMapPlace: SelectedMapPlace? = null,
    onPinClick: (String) -> Unit,
    onSavedPlaceClick: (SavedPlace) -> Unit = {},
    onMapClick: (LatLngPoint) -> Unit,
    onMapLongPress: (LatLngPoint) -> Unit = {},
    onMapCenterChanged: (LatLngPoint) -> Unit,
    onUserLocationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val currentOnSavedPlaceClick by rememberUpdatedState(onSavedPlaceClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongPress by rememberUpdatedState(onMapLongPress)
    val currentOnMapCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val currentOnUserLocationClick by rememberUpdatedState(onUserLocationClick)

    var isGlobeScale by remember {
        mutableStateOf(
            (cameraRequest?.zoom ?: if (userLocation != null) 14.3 else WorldCatalog.WORLD_OVERVIEW_ZOOM) < 5.6
        )
    }

    val mapView = remember {
        val appContext = context.applicationContext
        val config = Configuration.getInstance()
        config.userAgentValue = "CityPulseMap/1.0 (${appContext.packageName})"
        val basePath = File(appContext.cacheDir, "osmdroid")
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = File(basePath, "tiles")
        config.tileFileSystemCacheMaxBytes = 1024L * 1024L * 1024L
        config.tileFileSystemCacheTrimBytes = 850L * 1024L * 1024L

        MapView(context).apply {
            setTileSource(MapTileSources.tileSourceFor(mapStyleMode))
            isTilesScaledToDpi = true
            tilesScaleFactor = 1.25f
            overlayManager.tilesOverlay.setColorFilter(
                MapTileSources.borderClarityColorFilter(mapStyleMode)
            )
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(85.0, -85.0, 0)
            minZoomLevel = 1.5
            maxZoomLevel = 19.5
            val initial = cameraRequest?.point ?: userLocation ?: WorldCatalog.WORLD_OVERVIEW_CENTER
            val initialZoom = cameraRequest?.zoom ?: if (userLocation != null) 14.3 else WorldCatalog.WORLD_OVERVIEW_ZOOM
            controller.setZoom(initialZoom)
            controller.setCenter(GeoPoint(initial.latitude, initial.longitude))
        }
    }

    LaunchedEffect(mapStyleMode) {
        mapView.setTileSource(MapTileSources.tileSourceFor(mapStyleMode))
        mapView.overlayManager.tilesOverlay.setColorFilter(
            MapTileSources.borderClarityColorFilter(mapStyleMode)
        )
        mapView.invalidate()
    }

    val density = context.resources.displayMetrics.density
    val graticuleOverlay = remember(density) {
        AtlasGraticuleOverlay(density = density, isNightMode = mapStyleMode == MapStyleMode.OSM_NIGHT)
    }
    val labelsOverlay = remember(density) {
        CartographicLabelsOverlay(
            density = density,
            pins = pins,
            selectedPinId = selectedPinId,
            isNightMode = mapStyleMode == MapStyleMode.OSM_NIGHT
        )
    }

    // Triple state keys: (category, selected, isGlobeScale)
    val pinDrawables = remember(context) {
        buildMap {
            PinCategory.entries.forEach { category ->
                put(
                    Triple(category, false, false),
                    createCategoryPinDrawable(context, category, category.color.toArgb(), density, selected = false)
                )
                put(
                    Triple(category, true, false),
                    createCategoryPinDrawable(context, category, category.color.toArgb(), density, selected = true)
                )
                put(
                    Triple(category, false, true),
                    createCompactGlobeDotDrawable(context, category.color.toArgb(), density, selected = false)
                )
                put(
                    Triple(category, true, true),
                    createCompactGlobeDotDrawable(context, category.color.toArgb(), density, selected = true)
                )
            }
        }
    }

    val userBeaconFrames = remember(context) {
        createUserLocationBeaconFrames(context, density)
    }
    val airportHubDrawable = remember(context) {
        createTransportHubDrawable(context, HubKind.AIRPORT, density)
    }
    val seaportHubDrawable = remember(context) {
        createTransportHubDrawable(context, HubKind.SEAPORT, density)
    }
    val shipVehicleDrawable = remember(context) {
        createShipMarkerDrawable(context, density)
    }
    val searchSpotlightDrawable = remember(context) {
        createSearchSpotlightDrawable(context, density)
    }
    val savedPlaceDrawables = remember(context) {
        mapOf(
            SavedPlaceType.HOME to createSavedPlaceMarkerDrawable(context, SavedPlaceType.HOME, density),
            SavedPlaceType.WORK to createSavedPlaceMarkerDrawable(context, SavedPlaceType.WORK, density),
            SavedPlaceType.FAVORITE to createSavedPlaceMarkerDrawable(context, SavedPlaceType.FAVORITE, density)
        )
    }
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }

    // Smooth radar pulse animation exclusively on the user's personal navigation beacon
    LaunchedEffect(userLocation, userMarkerRef) {
        val marker = userMarkerRef
        if (userLocation == null || marker == null) return@LaunchedEffect
        var frameIndex = 0
        while (true) {
            marker.icon = userBeaconFrames[frameIndex % userBeaconFrames.size]
            mapView.postInvalidate()
            frameIndex++
            delay(160L)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val mapListener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val center = mapView.mapCenter
                currentOnMapCenterChanged(LatLngPoint(center.latitude, center.longitude))
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val center = mapView.mapCenter
                currentOnMapCenterChanged(LatLngPoint(center.latitude, center.longitude))
                val nextGlobe = mapView.zoomLevelDouble < 5.6
                if (nextGlobe != isGlobeScale) {
                    isGlobeScale = nextGlobe
                }
                return false
            }
        }
        mapView.addMapListener(mapListener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.removeMapListener(mapListener)
            mapView.onDetach()
        }
    }

    LaunchedEffect(cameraRequest?.nonce) {
        val req = cameraRequest ?: return@LaunchedEffect
        mapView.controller.animateTo(
            GeoPoint(req.point.latitude, req.point.longitude),
            req.zoom,
            480L
        )
    }

    LaunchedEffect(zoomDeltaRequest?.nonce) {
        val req = zoomDeltaRequest ?: return@LaunchedEffect
        val targetZoom = (mapView.zoomLevelDouble + req.delta).coerceIn(1.5, 19.5)
        mapView.controller.setZoom(targetZoom)
    }

    LaunchedEffect(routeFocusNonce) {
        val points = activeRoute?.polylinePoints.orEmpty()
        if (routeFocusNonce == 0L || points.size < 2) return@LaunchedEffect
        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
        val bbox = BoundingBox.fromGeoPointsSafe(geoPoints)
        mapView.post {
            if (mapView.width > 0 && mapView.height > 0) {
                mapView.zoomToBoundingBox(bbox, true, (72 * density).toInt())
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val nightMode = mapStyleMode == MapStyleMode.OSM_NIGHT
            graticuleOverlay.isNightMode = nightMode
            labelsOverlay.pins = pins
            labelsOverlay.selectedPinId = selectedPinId
            labelsOverlay.isNightMode = nightMode

            view.overlays.clear()

            view.overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (p != null) {
                            currentOnMapClick(LatLngPoint(p.latitude, p.longitude))
                            return true
                        }
                        return false
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        if (p != null) {
                            currentOnMapLongPress(LatLngPoint(p.latitude, p.longitude))
                            return true
                        }
                        return false
                    }
                })
            )

            // Subtle geographic atlas grid at global zoom
            view.overlays.add(graticuleOverlay)

            // Active multi-modal navigation route polylines (Road, Sea Ship lane, Air Flight arc)
            if (activeRoute != null) {
                activeRoute.segments.forEach { segment ->
                    if (segment.points.size < 2) return@forEach
                    val segGeos = segment.points.map { GeoPoint(it.latitude, it.longitude) }
                    when (segment.type) {
                        RouteSegmentType.ROAD -> {
                            val outerLine = Polyline(view).apply {
                                setPoints(segGeos)
                                outlinePaint.apply {
                                    color = android.graphics.Color.rgb(10, 22, 40)
                                    strokeWidth = 11.5f * density
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                            }
                            val innerLine = Polyline(view).apply {
                                setPoints(segGeos)
                                outlinePaint.apply {
                                    color = android.graphics.Color.rgb(20, 184, 166)
                                    strokeWidth = 6.8f * density
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                            }
                            view.overlays.add(outerLine)
                            view.overlays.add(innerLine)
                        }

                        RouteSegmentType.SEA -> {
                            val haloSea = Polyline(view).apply {
                                setPoints(segGeos)
                                outlinePaint.apply {
                                    color = android.graphics.Color.argb(115, 255, 255, 255)
                                    strokeWidth = 9.5f * density
                                    strokeCap = Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                            }
                            val dashedSea = Polyline(view).apply {
                                setPoints(segGeos)
                                outlinePaint.apply {
                                    color = android.graphics.Color.rgb(2, 132, 199)
                                    strokeWidth = 5.4f * density
                                    strokeCap = Paint.Cap.ROUND
                                    pathEffect = DashPathEffect(
                                        floatArrayOf(18f * density, 12f * density),
                                        0f
                                    )
                                    isAntiAlias = true
                                }
                            }
                            view.overlays.add(haloSea)
                            view.overlays.add(dashedSea)
                        }

                        RouteSegmentType.AIR -> {
                            val haloAir = Polyline(view).apply {
                                setPoints(segGeos)
                                outlinePaint.apply {
                                    color = android.graphics.Color.argb(120, 255, 255, 255)
                                    strokeWidth = 9.0f * density
                                    strokeCap = Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                            }
                            val dashedAir = Polyline(view).apply {
                                setPoints(segGeos)
                                outlinePaint.apply {
                                    color = android.graphics.Color.rgb(79, 70, 229)
                                    strokeWidth = 5.2f * density
                                    strokeCap = Paint.Cap.ROUND
                                    pathEffect = DashPathEffect(
                                        floatArrayOf(15f * density, 10f * density),
                                        0f
                                    )
                                    isAntiAlias = true
                                }
                            }
                            view.overlays.add(haloAir)
                            view.overlays.add(dashedAir)
                        }
                    }
                }

                // Draw departure & arrival Airport / Seaport markers
                activeRoute.hubMarkers.forEach { hubMarker ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(
                            hubMarker.hub.location.latitude,
                            hubMarker.hub.location.longitude
                        )
                        title = hubMarker.hub.nameAr
                        icon = if (hubMarker.hub.kind == HubKind.AIRPORT) {
                            airportHubDrawable
                        } else {
                            seaportHubDrawable
                        }
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setInfoWindow(null)
                    }
                    view.overlays.add(marker)
                }

                // Draw crisp Airplane (✈️) or Sea Ship (🚢) right on the Air / Sea route leg
                val vehiclePos = activeRoute.vehicleLocation
                if (vehiclePos != null) {
                    val vehicleDrawable = when (activeRoute.resolvedMode) {
                        TravelMode.AIR -> createAirplaneMarkerDrawable(
                            context = context,
                            density = density,
                            bearingDegrees = activeRoute.vehicleBearingDegrees
                        )
                        TravelMode.SEA -> shipVehicleDrawable
                        else -> null
                    }
                    if (vehicleDrawable != null) {
                        val vehicleMarker = Marker(view).apply {
                            position = GeoPoint(vehiclePos.latitude, vehiclePos.longitude)
                            icon = vehicleDrawable
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setInfoWindow(null)
                        }
                        view.overlays.add(vehicleMarker)
                    }
                }
            }

            // Draw unselected pins first, then selected pin on top
            val sortedPins = pins.sortedBy { it.pin.id == selectedPinId }
            sortedPins.forEach { item ->
                val pin = item.pin
                val category = pin.resolvedCategory()
                val isSelected = pin.id == selectedPinId
                val marker = Marker(view).apply {
                    position = GeoPoint(pin.latitude, pin.longitude)
                    title = pin.title
                    icon = pinDrawables[Triple(category, isSelected, isGlobeScale)]
                    if (isGlobeScale) {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    } else {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        currentOnPinClick(pin.id)
                        true
                    }
                }
                view.overlays.add(marker)
            }

            // Saved Home, Work & Favorite Places Markers
            savedPlaces.forEach { saved ->
                val savedType = saved.resolvedType()
                val marker = Marker(view).apply {
                    position = GeoPoint(saved.latitude, saved.longitude)
                    title = saved.title
                    icon = savedPlaceDrawables[savedType]
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        currentOnSavedPlaceClick(saved)
                        true
                    }
                }
                view.overlays.add(marker)
            }

            // Highlighted search target or tapped place spotlight marker
            val spotlightLocation = selectedMapPlace?.location ?: highlightedSearchResult?.location
            val spotlightTitle = selectedMapPlace?.title ?: highlightedSearchResult?.title
            if (spotlightLocation != null) {
                val targetMarker = Marker(view).apply {
                    position = GeoPoint(
                        spotlightLocation.latitude,
                        spotlightLocation.longitude
                    )
                    title = spotlightTitle
                    icon = searchSpotlightDrawable
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setInfoWindow(null)
                }
                view.overlays.add(targetMarker)
            }

            // Crisp Arabic country names & collision-culled place/city names ("واساميهم")
            view.overlays.add(labelsOverlay)

            if (userLocation != null) {
                val userMarker = Marker(view).apply {
                    position = GeoPoint(userLocation.latitude, userLocation.longitude)
                    icon = userBeaconFrames.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        currentOnUserLocationClick()
                        true
                    }
                }
                userMarkerRef = userMarker
                view.overlays.add(userMarker)
            } else {
                userMarkerRef = null
            }

            view.invalidate()
        }
    )
}

private fun createCategoryPinDrawable(
    context: Context,
    category: PinCategory,
    colorArgb: Int,
    density: Float,
    selected: Boolean
): BitmapDrawable {
    val scale = if (selected) 1.28f else 1.0f
    val widthPx = (46 * density * scale).toInt().coerceAtLeast(24)
    val heightPx = (58 * density * scale).toInt().coerceAtLeast(30)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val radius = widthPx * 0.35f
    val cy = radius + 6f * density * scale
    val tipY = heightPx - 4f * density * scale

    // Ground shadow ellipse
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(if (selected) 90 else 65, 10, 22, 40)
        style = Paint.Style.FILL
    }
    canvas.drawOval(
        RectF(cx - radius * 0.52f, tipY - 2.5f * density, cx + radius * 0.52f, tipY + 2.5f * density),
        shadowPaint
    )

    // Selected outer glow halo
    if (selected) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(
                75,
                android.graphics.Color.red(colorArgb),
                android.graphics.Color.green(colorArgb),
                android.graphics.Color.blue(colorArgb)
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius * 1.24f, haloPaint)
    }

    // Crisp white outer pin silhouette
    val outerPinPath = Path().apply {
        addCircle(cx, cy, radius + 2.4f * density * scale, Path.Direction.CW)
        moveTo(cx - radius * 0.85f, cy + radius * 0.50f)
        lineTo(cx, tipY)
        lineTo(cx + radius * 0.85f, cy + radius * 0.50f)
        close()
    }
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawPath(outerPinPath, whitePaint)

    // Colored inner pin silhouette
    val innerPinPath = Path().apply {
        addCircle(cx, cy, radius, Path.Direction.CW)
        moveTo(cx - radius * 0.72f, cy + radius * 0.44f)
        lineTo(cx, tipY - 3.0f * density * scale)
        lineTo(cx + radius * 0.72f, cy + radius * 0.44f)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        style = Paint.Style.FILL
    }
    canvas.drawPath(innerPinPath, fillPaint)

    val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val glyphStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density * scale
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val r = radius * 0.52f
    when (category) {
        PinCategory.PHARMACY -> {
            val arm = r * 0.34f
            canvas.drawRoundRect(
                RectF(cx - arm, cy - r * 0.85f, cx + arm, cy + r * 0.85f),
                arm * 0.4f, arm * 0.4f, glyphPaint
            )
            canvas.drawRoundRect(
                RectF(cx - r * 0.85f, cy - arm, cx + r * 0.85f, cy + arm),
                arm * 0.4f, arm * 0.4f, glyphPaint
            )
        }

        PinCategory.RESTAURANT -> {
            val forkX = cx - r * 0.38f
            canvas.drawLine(forkX, cy - r * 0.78f, forkX, cy + r * 0.78f, glyphStroke)
            canvas.drawLine(forkX - r * 0.22f, cy - r * 0.78f, forkX - r * 0.22f, cy - r * 0.15f, glyphStroke)
            canvas.drawLine(forkX + r * 0.22f, cy - r * 0.78f, forkX + r * 0.22f, cy - r * 0.15f, glyphStroke)
            canvas.drawLine(forkX - r * 0.22f, cy - r * 0.15f, forkX + r * 0.22f, cy - r * 0.15f, glyphStroke)

            val knifeX = cx + r * 0.38f
            canvas.drawLine(knifeX, cy - r * 0.78f, knifeX, cy + r * 0.78f, glyphStroke)
            canvas.drawArc(
                RectF(knifeX - r * 0.26f, cy - r * 0.80f, knifeX + r * 0.14f, cy + r * 0.05f),
                270f, 180f, true, glyphPaint
            )
        }

        PinCategory.MAINTENANCE -> {
            val wPaint = Paint(glyphStroke).apply {
                strokeWidth = 2.6f * density * scale
            }
            canvas.drawLine(cx - r * 0.55f, cy + r * 0.55f, cx + r * 0.45f, cy - r * 0.45f, wPaint)
            canvas.drawCircle(cx + r * 0.48f, cy - r * 0.48f, r * 0.36f, glyphPaint)
            val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx + r * 0.60f, cy - r * 0.60f, r * 0.18f, cutPaint)
            canvas.drawCircle(cx - r * 0.56f, cy + r * 0.56f, r * 0.22f, glyphPaint)
        }

        PinCategory.CAFE -> {
            val cupRect = RectF(cx - r * 0.56f, cy - r * 0.22f, cx + r * 0.34f, cy + r * 0.68f)
            canvas.drawRoundRect(cupRect, r * 0.25f, r * 0.25f, glyphPaint)
            canvas.drawArc(
                RectF(cx + r * 0.22f, cy - r * 0.10f, cx + r * 0.72f, cy + r * 0.42f),
                270f, 180f, false, glyphStroke
            )
            val steamStroke = Paint(glyphStroke).apply { strokeWidth = 1.5f * density * scale }
            canvas.drawLine(cx - r * 0.28f, cy - r * 0.45f, cx - r * 0.20f, cy - r * 0.78f, steamStroke)
            canvas.drawLine(cx + r * 0.08f, cy - r * 0.45f, cx + r * 0.16f, cy - r * 0.78f, steamStroke)
        }

        PinCategory.DEAL -> {
            val star = Path().apply {
                moveTo(cx, cy - r * 0.90f)
                lineTo(cx + r * 0.24f, cy - r * 0.24f)
                lineTo(cx + r * 0.90f, cy)
                lineTo(cx + r * 0.24f, cy + r * 0.24f)
                lineTo(cx, cy + r * 0.90f)
                lineTo(cx - r * 0.24f, cy + r * 0.24f)
                lineTo(cx - r * 0.90f, cy)
                lineTo(cx - r * 0.24f, cy - r * 0.24f)
                close()
            }
            canvas.drawPath(star, glyphPaint)
        }

        PinCategory.ROAD_REPORT -> {
            val tri = Path().apply {
                moveTo(cx, cy - r * 0.80f)
                lineTo(cx + r * 0.82f, cy + r * 0.68f)
                lineTo(cx - r * 0.82f, cy + r * 0.68f)
                close()
            }
            canvas.drawPath(tri, glyphPaint)
            val alertCut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb
                style = Paint.Style.STROKE
                strokeWidth = 2.0f * density * scale
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.16f, alertCut)
            val dotCut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy + r * 0.42f, 1.3f * density * scale, dotCut)
        }
    }

    return BitmapDrawable(context.resources, bitmap)
}

private fun createAirplaneMarkerDrawable(
    context: Context,
    density: Float,
    bearingDegrees: Float
): BitmapDrawable {
    val sizePx = (52 * density).toInt().coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx / 2f

    // Glowing indigo badge circle
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(75, 10, 22, 40)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c + 2f * density, sizePx * 0.42f, shadowPaint)

    val whiteRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, sizePx * 0.40f, whiteRim)

    val circleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(79, 70, 229)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, sizePx * 0.34f, circleBg)

    // Rotated crisp Airplane silhouette oriented along flight heading
    canvas.save()
    canvas.rotate(bearingDegrees, c, c)
    val r = sizePx * 0.24f
    val planePath = Path().apply {
        // Nose -> right wing -> tail -> left wing
        moveTo(c, c - r * 1.15f)
        lineTo(c + r * 0.22f, c - r * 0.30f)
        lineTo(c + r * 1.10f, c + r * 0.22f)
        lineTo(c + r * 1.10f, c + r * 0.48f)
        lineTo(c + r * 0.22f, c + r * 0.24f)
        lineTo(c + r * 0.20f, c + r * 0.72f)
        lineTo(c + r * 0.50f, c + r * 0.96f)
        lineTo(c + r * 0.50f, c + r * 1.12f)
        lineTo(c, c + r * 0.96f)
        lineTo(c - r * 0.50f, c + r * 1.12f)
        lineTo(c - r * 0.50f, c + r * 0.96f)
        lineTo(c - r * 0.20f, c + r * 0.72f)
        lineTo(c - r * 0.22f, c + r * 0.24f)
        lineTo(c - r * 1.10f, c + r * 0.48f)
        lineTo(c - r * 1.10f, c + r * 0.22f)
        lineTo(c - r * 0.22f, c - r * 0.30f)
        close()
    }
    canvas.drawPath(planePath, whiteRim)
    canvas.restore()

    return BitmapDrawable(context.resources, bitmap)
}

private fun createShipMarkerDrawable(
    context: Context,
    density: Float
): BitmapDrawable {
    val sizePx = (50 * density).toInt().coerceAtLeast(46)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(75, 10, 22, 40)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c + 2f * density, sizePx * 0.42f, shadowPaint)

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, sizePx * 0.40f, whitePaint)

    val seaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(2, 132, 199)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, sizePx * 0.34f, seaPaint)

    val r = sizePx * 0.22f
    // Cabin & smokestack
    canvas.drawRoundRect(
        RectF(c - r * 0.55f, c - r * 0.55f, c + r * 0.55f, c + r * 0.08f),
        2f * density, 2f * density, whitePaint
    )
    canvas.drawRect(
        RectF(c - r * 0.22f, c - r * 0.90f, c + r * 0.16f, c - r * 0.52f),
        whitePaint
    )
    // Ship hull
    val hullPath = Path().apply {
        moveTo(c - r * 0.95f, c + r * 0.08f)
        lineTo(c + r * 0.95f, c + r * 0.08f)
        lineTo(c + r * 0.65f, c + r * 0.68f)
        lineTo(c - r * 0.65f, c + r * 0.68f)
        close()
    }
    canvas.drawPath(hullPath, whitePaint)

    // Wave accent below hull
    val waveStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(186, 230, 253)
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(c - r * 0.78f, c + r * 0.92f, c + r * 0.78f, c + r * 0.92f, waveStroke)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createTransportHubDrawable(
    context: Context,
    kind: HubKind,
    density: Float
): BitmapDrawable {
    val sizePx = (36 * density).toInt().coerceAtLeast(32)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx / 2f

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, sizePx * 0.46f, whitePaint)

    val bgColor = if (kind == HubKind.AIRPORT) {
        android.graphics.Color.rgb(67, 56, 202)
    } else {
        android.graphics.Color.rgb(3, 105, 161)
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, sizePx * 0.38f, fillPaint)

    val r = sizePx * 0.21f
    if (kind == HubKind.AIRPORT) {
        val plane = Path().apply {
            moveTo(c, c - r)
            lineTo(c + r * 0.2f, c - r * 0.25f)
            lineTo(c + r * 0.95f, c + r * 0.2f)
            lineTo(c + r * 0.95f, c + r * 0.42f)
            lineTo(c + r * 0.2f, c + r * 0.2f)
            lineTo(c + r * 0.16f, c + r * 0.7f)
            lineTo(c + r * 0.45f, c + r * 0.92f)
            lineTo(c, c + r * 0.8f)
            lineTo(c - r * 0.45f, c + r * 0.92f)
            lineTo(c - r * 0.16f, c + r * 0.7f)
            lineTo(c - r * 0.2f, c + r * 0.2f)
            lineTo(c - r * 0.95f, c + r * 0.42f)
            lineTo(c - r * 0.95f, c + r * 0.2f)
            lineTo(c - r * 0.2f, c - r * 0.25f)
            close()
        }
        canvas.drawPath(plane, whitePaint)
    } else {
        val anchorStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(c, c - r * 0.55f, r * 0.26f, anchorStroke)
        canvas.drawLine(c, c - r * 0.30f, c, c + r * 0.82f, anchorStroke)
        canvas.drawLine(c - r * 0.55f, c + r * 0.05f, c + r * 0.55f, c + r * 0.05f, anchorStroke)
        canvas.drawArc(
            RectF(c - r * 0.75f, c - r * 0.15f, c + r * 0.75f, c + r * 0.85f),
            15f, 150f, false, anchorStroke
        )
    }

    return BitmapDrawable(context.resources, bitmap)
}

private fun createSearchSpotlightDrawable(
    context: Context,
    density: Float
): BitmapDrawable {
    val widthPx = (48 * density).toInt().coerceAtLeast(42)
    val heightPx = (60 * density).toInt().coerceAtLeast(52)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = widthPx / 2f
    val radius = widthPx * 0.34f
    val cy = radius + 6f * density
    val tipY = heightPx - 4f * density

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(85, 245, 158, 11)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius * 1.32f, haloPaint)

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val outerPin = Path().apply {
        addCircle(cx, cy, radius + 2.5f * density, Path.Direction.CW)
        moveTo(cx - radius * 0.85f, cy + radius * 0.50f)
        lineTo(cx, tipY)
        lineTo(cx + radius * 0.85f, cy + radius * 0.50f)
        close()
    }
    canvas.drawPath(outerPin, whitePaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(225, 29, 72)
        style = Paint.Style.FILL
    }
    val innerPin = Path().apply {
        addCircle(cx, cy, radius, Path.Direction.CW)
        moveTo(cx - radius * 0.72f, cy + radius * 0.44f)
        lineTo(cx, tipY - 3.0f * density)
        lineTo(cx + radius * 0.72f, cy + radius * 0.44f)
        close()
    }
    canvas.drawPath(innerPin, fillPaint)

    val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(253, 224, 71)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius * 0.38f, starPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createSavedPlaceMarkerDrawable(
    context: Context,
    type: SavedPlaceType,
    density: Float
): BitmapDrawable {
    val widthPx = (48 * density).toInt().coerceAtLeast(42)
    val heightPx = (60 * density).toInt().coerceAtLeast(52)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = widthPx / 2f
    val radius = widthPx * 0.35f
    val cy = radius + 6f * density
    val tipY = heightPx - 4f * density

    val fillColor = when (type) {
        SavedPlaceType.HOME -> android.graphics.Color.rgb(15, 157, 138)
        SavedPlaceType.WORK -> android.graphics.Color.rgb(59, 130, 246)
        SavedPlaceType.FAVORITE -> android.graphics.Color.rgb(245, 158, 11)
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 10, 22, 40)
        style = Paint.Style.FILL
    }
    canvas.drawOval(
        RectF(cx - radius * 0.55f, tipY - 2.5f * density, cx + radius * 0.55f, tipY + 2.5f * density),
        shadowPaint
    )

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val outerPin = Path().apply {
        addCircle(cx, cy, radius + 2.6f * density, Path.Direction.CW)
        moveTo(cx - radius * 0.85f, cy + radius * 0.50f)
        lineTo(cx, tipY)
        lineTo(cx + radius * 0.85f, cy + radius * 0.50f)
        close()
    }
    canvas.drawPath(outerPin, whitePaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val innerPin = Path().apply {
        addCircle(cx, cy, radius, Path.Direction.CW)
        moveTo(cx - radius * 0.72f, cy + radius * 0.44f)
        lineTo(cx, tipY - 3.0f * density)
        lineTo(cx + radius * 0.72f, cy + radius * 0.44f)
        close()
    }
    canvas.drawPath(innerPin, fillPaint)

    val r = radius * 0.54f
    val glyphStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    when (type) {
        SavedPlaceType.HOME -> {
            val house = Path().apply {
                moveTo(cx, cy - r * 0.90f)
                lineTo(cx + r * 0.85f, cy - r * 0.10f)
                lineTo(cx + r * 0.65f, cy - r * 0.10f)
                lineTo(cx + r * 0.65f, cy + r * 0.76f)
                lineTo(cx - r * 0.65f, cy + r * 0.76f)
                lineTo(cx - r * 0.65f, cy - r * 0.10f)
                lineTo(cx - r * 0.85f, cy - r * 0.10f)
                close()
            }
            canvas.drawPath(house, whitePaint)
            val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cx - r * 0.22f, cy + r * 0.16f, cx + r * 0.22f, cy + r * 0.76f),
                1.5f * density, 1.5f * density, doorPaint
            )
        }

        SavedPlaceType.WORK -> {
            // Briefcase handle + body
            canvas.drawRoundRect(
                RectF(cx - r * 0.36f, cy - r * 0.78f, cx + r * 0.36f, cy - r * 0.35f),
                2f * density, 2f * density, glyphStroke
            )
            canvas.drawRoundRect(
                RectF(cx - r * 0.82f, cy - r * 0.42f, cx + r * 0.82f, cy + r * 0.68f),
                3f * density, 3f * density, whitePaint
            )
            val latchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.STROKE
                strokeWidth = 1.6f * density
            }
            canvas.drawLine(cx - r * 0.78f, cy + r * 0.04f, cx + r * 0.78f, cy + r * 0.04f, latchPaint)
        }

        SavedPlaceType.FAVORITE -> {
            // 5-point Star
            val star = Path()
            val outerR = r * 0.94f
            val innerR = r * 0.42f
            for (i in 0 until 10) {
                val angle = Math.toRadians(-90.0 + i * 36.0)
                val rad = if (i % 2 == 0) outerR else innerR
                val px = (cx + rad * kotlin.math.cos(angle)).toFloat()
                val py = (cy + rad * kotlin.math.sin(angle)).toFloat()
                if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
            }
            star.close()
            canvas.drawPath(star, whitePaint)
        }
    }

    return BitmapDrawable(context.resources, bitmap)
}
