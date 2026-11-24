package com.example.network

import androidx.annotation.Keep
import com.example.model.HubKind
import com.example.model.LatLngPoint
import com.example.model.TransportHub
import com.example.model.TransportHubCatalog
import com.example.model.calculateDistanceMeters
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Keep
data class OsrmRouteResponseDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "routes") val routes: List<OsrmRouteDto>? = null
)

@Keep
data class OsrmRouteDto(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "geometry") val geometry: OsrmGeometryDto? = null,
    @Json(name = "legs") val legs: List<OsrmLegDto>? = null
)

@Keep
data class OsrmGeometryDto(
    @Json(name = "coordinates") val coordinates: List<List<Double>>? = null
)

@Keep
data class OsrmLegDto(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "steps") val steps: List<OsrmStepDto>? = null
)

@Keep
data class OsrmStepDto(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "name") val name: String? = null,
    @Json(name = "mode") val mode: String? = null,
    @Json(name = "geometry") val geometry: OsrmGeometryDto? = null,
    @Json(name = "maneuver") val maneuver: OsrmManeuverDto? = null
)

@Keep
data class OsrmManeuverDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "modifier") val modifier: String? = null,
    @Json(name = "exit") val exit: Int? = null,
    @Json(name = "location") val location: List<Double>? = null
)

interface OsrmApiService {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getDrivingRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = true
    ): OsrmRouteResponseDto
}

enum class TravelMode {
    AUTO,
    ROAD,
    SEA,
    AIR
}

enum class RouteSegmentType {
    ROAD,
    SEA,
    AIR
}

data class RouteSegment(
    val type: RouteSegmentType,
    val points: List<LatLngPoint>
)

data class RouteHubMarker(
    val hub: TransportHub,
    val isDeparture: Boolean
)

enum class ManeuverDirection {
    DEPART,
    STRAIGHT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    U_TURN,
    ROUNDABOUT,
    AIRPORT,
    FLIGHT,
    PORT,
    SHIP,
    ARRIVE
}

data class RouteStep(
    val index: Int,
    val instructionAr: String,
    val voiceAnnouncementAr: String,
    val roadName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val direction: ManeuverDirection,
    val location: LatLngPoint
)

data class NavigationRoute(
    val origin: LatLngPoint,
    val destination: LatLngPoint,
    val destinationName: String,
    val resolvedMode: TravelMode,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val polylinePoints: List<LatLngPoint>,
    val segments: List<RouteSegment>,
    val hubMarkers: List<RouteHubMarker> = emptyList(),
    val vehicleLocation: LatLngPoint? = null,
    val vehicleBearingDegrees: Float = 0f,
    val steps: List<RouteStep>,
    val isDirectFallback: Boolean = false
)

class DirectionsRepository {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CityPulseMap/1.0 (com.aistudio.citypulse.q7m4xk)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val api: OsrmApiService by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OsrmApiService::class.java)
    }

    suspend fun fetchRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        destinationName: String,
        preferredMode: TravelMode = TravelMode.AUTO
    ): NavigationRoute {
        val straightMeters = calculateDistanceMeters(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude
        ).toDouble().coerceAtLeast(10.0)

        return when (preferredMode) {
            TravelMode.AIR -> buildAirportFlightRoute(origin, destination, destinationName)
            TravelMode.SEA -> buildSeaShipRoute(origin, destination, destinationName)
            TravelMode.ROAD -> {
                fetchOsrmDrivingOnly(origin, destination, destinationName)
                    ?: buildDirectCompassFallback(origin, destination, destinationName)
            }
            TravelMode.AUTO -> {
                // For very long transcontinental trips (> 2800km), fly via Airport + Airplane automatically
                if (straightMeters > 2_800_000.0) {
                    return buildAirportFlightRoute(origin, destination, destinationName)
                }

                val osrmRoad = fetchOsrmDrivingOnly(origin, destination, destinationName)
                if (osrmRoad != null) {
                    osrmRoad
                } else {
                    // Separated by sea/ocean (OSRM cannot connect by road): choose Ship for nearby seas or Flight for longer sea crossings
                    if (straightMeters <= 1_400_000.0) {
                        buildSeaShipRoute(origin, destination, destinationName)
                    } else {
                        buildAirportFlightRoute(origin, destination, destinationName)
                    }
                }
            }
        }
    }

    private suspend fun fetchOsrmDrivingOnly(
        origin: LatLngPoint,
        destination: LatLngPoint,
        destinationName: String
    ): NavigationRoute? {
        val coordsParam = "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
        return try {
            val response = withTimeout(12_000L) {
                api.getDrivingRoute(coordinates = coordsParam)
            }
            val routeDto = response.routes?.firstOrNull() ?: return null
            if (!response.code.equals("Ok", ignoreCase = true)) return null

            val allPoints = routeDto.geometry?.coordinates.orEmpty().mapNotNull { pair ->
                if (pair.size >= 2) LatLngPoint(latitude = pair[1], longitude = pair[0]) else null
            }.ifEmpty { listOf(origin, destination) }

            val rawSteps = routeDto.legs?.firstOrNull()?.steps.orEmpty()

            // Split ROAD vs OSRM FERRY steps seamlessly so any sea ferry inside OSRM renders a ship path
            val segments = mutableListOf<RouteSegment>()
            var hasFerry = false
            var ferryMidpoint: LatLngPoint? = null
            var ferryBearing = 0f

            for (stepDto in rawSteps) {
                val stepCoords = stepDto.geometry?.coordinates.orEmpty().mapNotNull { pair ->
                    if (pair.size >= 2) LatLngPoint(pair[1], pair[0]) else null
                }
                if (stepCoords.size >= 2) {
                    val isFerry = stepDto.mode.equals("ferry", ignoreCase = true)
                    val segType = if (isFerry) RouteSegmentType.SEA else RouteSegmentType.ROAD
                    if (isFerry && ferryMidpoint == null) {
                        hasFerry = true
                        ferryMidpoint = stepCoords[stepCoords.size / 2]
                        ferryBearing = computeBearingDegrees(stepCoords.first(), stepCoords.last())
                    }
                    val lastSeg = segments.lastOrNull()
                    if (lastSeg != null && lastSeg.type == segType) {
                        segments[segments.lastIndex] = lastSeg.copy(
                            points = lastSeg.points + stepCoords.drop(1)
                        )
                    } else {
                        segments.add(RouteSegment(type = segType, points = stepCoords))
                    }
                }
            }

            if (segments.isEmpty()) {
                segments.add(RouteSegment(type = RouteSegmentType.ROAD, points = allPoints))
            }

            val translatedSteps = rawSteps.mapIndexed { idx, stepDto ->
                translateOsrmStep(
                    index = idx,
                    dto = stepDto,
                    destinationName = destinationName,
                    fallbackPoint = allPoints.getOrElse(idx.coerceAtMost(allPoints.lastIndex)) { destination }
                )
            }.ifEmpty {
                listOf(
                    RouteStep(
                        index = 0,
                        instructionAr = "توجّه عبر الطريق نحو $destinationName",
                        voiceAnnouncementAr = "ابدأ المسار بالسيارة نحو $destinationName لمسافة ${formatRouteDistanceAr(routeDto.distance)}",
                        roadName = "",
                        distanceMeters = routeDto.distance,
                        durationSeconds = routeDto.duration,
                        direction = ManeuverDirection.DEPART,
                        location = origin
                    )
                )
            }

            NavigationRoute(
                origin = origin,
                destination = destination,
                destinationName = destinationName,
                resolvedMode = if (hasFerry) TravelMode.SEA else TravelMode.ROAD,
                totalDistanceMeters = routeDto.distance,
                totalDurationSeconds = routeDto.duration,
                polylinePoints = allPoints,
                segments = segments,
                hubMarkers = emptyList(),
                vehicleLocation = ferryMidpoint,
                vehicleBearingDegrees = ferryBearing,
                steps = translatedSteps,
                isDirectFallback = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Multi-modal Air Route:
     * 1) Real OSRM road route from origin -> nearest departure airport
     * 2) Curved flight arc with Airplane marker from departure airport -> destination airport
     * 3) Real OSRM road route from destination airport -> destination
     */
    private suspend fun buildAirportFlightRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        destinationName: String
    ): NavigationRoute {
        val depAirport = TransportHubCatalog.nearestAirport(origin)
        val arrAirport = TransportHubCatalog.nearestAirport(destination)

        val steps = mutableListOf<RouteStep>()
        val segments = mutableListOf<RouteSegment>()
        val allPoints = mutableListOf<LatLngPoint>()
        var totalDistance = 0.0
        var totalDuration = 0.0

        // Leg 1: Road to Departure Airport
        val toAirportDist = calculateDistanceMeters(
            origin.latitude, origin.longitude,
            depAirport.location.latitude, depAirport.location.longitude
        ).toDouble()

        if (toAirportDist > 400.0) {
            val leg1 = fetchOsrmDrivingOnly(origin, depAirport.location, depAirport.nameAr)
            if (leg1 != null) {
                segments.addAll(leg1.segments)
                allPoints.addAll(leg1.polylinePoints)
                totalDistance += leg1.totalDistanceMeters
                totalDuration += leg1.totalDurationSeconds
                leg1.steps.dropLast(1).forEach { s ->
                    steps.add(s.copy(index = steps.size))
                }
            } else {
                val fallbackRoad = listOf(origin, depAirport.location)
                segments.add(RouteSegment(RouteSegmentType.ROAD, fallbackRoad))
                allPoints.addAll(fallbackRoad)
                totalDistance += toAirportDist
                totalDuration += toAirportDist / 16.0
                steps.add(
                    RouteStep(
                        index = steps.size,
                        instructionAr = "توجّه بالسيارة إلى ${depAirport.nameAr}",
                        voiceAnnouncementAr = "توجّه بالسيارة نحو ${depAirport.nameAr} لمسافة ${formatRouteDistanceAr(toAirportDist)}",
                        roadName = depAirport.nameAr,
                        distanceMeters = toAirportDist,
                        durationSeconds = toAirportDist / 16.0,
                        direction = ManeuverDirection.AIRPORT,
                        location = depAirport.location
                    )
                )
            }
        }

        // Leg 2: Curved Airplane Flight Arc between Airports
        val flightDist = calculateDistanceMeters(
            depAirport.location.latitude, depAirport.location.longitude,
            arrAirport.location.latitude, arrAirport.location.longitude
        ).toDouble().coerceAtLeast(1000.0)
        // Average commercial jet speed ~820 km/h (227 m/s) + 30 min boarding buffer
        val flightDuration = (flightDist / 227.0) + 1800.0
        val arcPoints = buildCurvedFlightArc(depAirport.location, arrAirport.location)
        val planePos = arcPoints[arcPoints.size / 2]
        val planeNext = arcPoints.getOrElse(arcPoints.size / 2 + 1) { arrAirport.location }
        val planeBearing = computeBearingDegrees(planePos, planeNext)

        if (depAirport.id != arrAirport.id) {
            segments.add(RouteSegment(RouteSegmentType.AIR, arcPoints))
            allPoints.addAll(arcPoints)
            totalDistance += flightDist
            totalDuration += flightDuration

            steps.add(
                RouteStep(
                    index = steps.size,
                    instructionAr = "استقل الطائرة من ${depAirport.nameAr} إلى ${arrAirport.nameAr}",
                    voiceAnnouncementAr = "من ${depAirport.nameAr}، استقل الطائرة المتجهة إلى ${arrAirport.nameAr} لمسافة ${formatRouteDistanceAr(flightDist)}، والزمن الجوي التقريبي ${formatRouteDurationAr(flightDuration)}",
                    roadName = "${depAirport.cityAr} ← ${arrAirport.cityAr}",
                    distanceMeters = flightDist,
                    durationSeconds = flightDuration,
                    direction = ManeuverDirection.FLIGHT,
                    location = planePos
                )
            )
        }

        // Leg 3: Road from Arrival Airport -> Destination
        val fromAirportDist = calculateDistanceMeters(
            arrAirport.location.latitude, arrAirport.location.longitude,
            destination.latitude, destination.longitude
        ).toDouble()

        if (fromAirportDist > 400.0) {
            val leg3 = fetchOsrmDrivingOnly(arrAirport.location, destination, destinationName)
            if (leg3 != null) {
                segments.addAll(leg3.segments)
                allPoints.addAll(leg3.polylinePoints)
                totalDistance += leg3.totalDistanceMeters
                totalDuration += leg3.totalDurationSeconds
                leg3.steps.forEach { s ->
                    steps.add(s.copy(index = steps.size))
                }
            } else {
                val finalRoad = listOf(arrAirport.location, destination)
                segments.add(RouteSegment(RouteSegmentType.ROAD, finalRoad))
                allPoints.addAll(finalRoad)
                totalDistance += fromAirportDist
                totalDuration += fromAirportDist / 16.0
                steps.add(
                    RouteStep(
                        index = steps.size,
                        instructionAr = "بعد الهبوط في ${arrAirport.nameAr}، اتجه بالسيارة إلى $destinationName",
                        voiceAnnouncementAr = "بعد الوصول إلى ${arrAirport.nameAr}، أكمل طريقك بالسيارة نحو $destinationName لمسافة ${formatRouteDistanceAr(fromAirportDist)}",
                        roadName = arrAirport.nameAr,
                        distanceMeters = fromAirportDist,
                        durationSeconds = fromAirportDist / 16.0,
                        direction = ManeuverDirection.ARRIVE,
                        location = destination
                    )
                )
            }
        } else {
            steps.add(
                RouteStep(
                    index = steps.size,
                    instructionAr = "وصلت إلى وجهتك: $destinationName",
                    voiceAnnouncementAr = "وصلت إلى وجهتك $destinationName بالسلامة",
                    roadName = destinationName,
                    distanceMeters = 0.0,
                    durationSeconds = 0.0,
                    direction = ManeuverDirection.ARRIVE,
                    location = destination
                )
            )
        }

        val hubs = buildList {
            add(RouteHubMarker(depAirport, isDeparture = true))
            if (arrAirport.id != depAirport.id) {
                add(RouteHubMarker(arrAirport, isDeparture = false))
            }
        }

        return NavigationRoute(
            origin = origin,
            destination = destination,
            destinationName = destinationName,
            resolvedMode = TravelMode.AIR,
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDuration,
            polylinePoints = allPoints.ifEmpty { listOf(origin, destination) },
            segments = segments,
            hubMarkers = hubs,
            vehicleLocation = if (depAirport.id != arrAirport.id) planePos else null,
            vehicleBearingDegrees = planeBearing,
            steps = steps,
            isDirectFallback = false
        )
    }

    /**
     * Multi-modal Sea Ship Route:
     * 1) Real OSRM road route from origin -> nearest seaport
     * 2) Maritime sea crossing lane with Ship marker from departure port -> destination port
     * 3) Real OSRM road route from destination port -> destination
     */
    private suspend fun buildSeaShipRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        destinationName: String
    ): NavigationRoute {
        val depPort = TransportHubCatalog.nearestSeaport(origin)
        val arrPort = TransportHubCatalog.nearestSeaport(destination)

        val steps = mutableListOf<RouteStep>()
        val segments = mutableListOf<RouteSegment>()
        val allPoints = mutableListOf<LatLngPoint>()
        var totalDistance = 0.0
        var totalDuration = 0.0

        // Leg 1: Road to Departure Seaport
        val toPortDist = calculateDistanceMeters(
            origin.latitude, origin.longitude,
            depPort.location.latitude, depPort.location.longitude
        ).toDouble()

        if (toPortDist > 300.0) {
            val leg1 = fetchOsrmDrivingOnly(origin, depPort.location, depPort.nameAr)
            if (leg1 != null) {
                segments.addAll(leg1.segments)
                allPoints.addAll(leg1.polylinePoints)
                totalDistance += leg1.totalDistanceMeters
                totalDuration += leg1.totalDurationSeconds
                leg1.steps.dropLast(1).forEach { s ->
                    steps.add(s.copy(index = steps.size))
                }
            } else {
                val road = listOf(origin, depPort.location)
                segments.add(RouteSegment(RouteSegmentType.ROAD, road))
                allPoints.addAll(road)
                totalDistance += toPortDist
                totalDuration += toPortDist / 14.0
                steps.add(
                    RouteStep(
                        index = steps.size,
                        instructionAr = "توجّه عبر الطريق إلى ${depPort.nameAr}",
                        voiceAnnouncementAr = "توجّه بالسيارة إلى ${depPort.nameAr} لمسافة ${formatRouteDistanceAr(toPortDist)}",
                        roadName = depPort.nameAr,
                        distanceMeters = toPortDist,
                        durationSeconds = toPortDist / 14.0,
                        direction = ManeuverDirection.PORT,
                        location = depPort.location
                    )
                )
            }
        }

        // Leg 2: Maritime Ship Crossing
        val seaTarget = if (depPort.id != arrPort.id) arrPort.location else destination
        val seaTargetLabel = if (depPort.id != arrPort.id) arrPort.nameAr else destinationName
        val seaDist = calculateDistanceMeters(
            depPort.location.latitude, depPort.location.longitude,
            seaTarget.latitude, seaTarget.longitude
        ).toDouble().coerceAtLeast(500.0)
        // Average ferry / ship speed ~40 km/h (11.1 m/s)
        val seaDuration = seaDist / 11.1
        val seaPoints = buildSmoothSeaLane(depPort.location, seaTarget)
        val shipPos = seaPoints[seaPoints.size / 2]
        val shipNext = seaPoints.getOrElse(seaPoints.size / 2 + 1) { seaTarget }
        val shipBearing = computeBearingDegrees(shipPos, shipNext)

        segments.add(RouteSegment(RouteSegmentType.SEA, seaPoints))
        allPoints.addAll(seaPoints)
        totalDistance += seaDist
        totalDuration += seaDuration

        steps.add(
            RouteStep(
                index = steps.size,
                instructionAr = "اعبر البحر بالسفينة من ${depPort.nameAr} إلى $seaTargetLabel",
                voiceAnnouncementAr = "من ${depPort.nameAr}، استقل السفينة عبر البحر باتجاه $seaTargetLabel لمسافة ${formatRouteDistanceAr(seaDist)}",
                roadName = "${depPort.cityAr} ~ البحر",
                distanceMeters = seaDist,
                durationSeconds = seaDuration,
                direction = ManeuverDirection.SHIP,
                location = shipPos
            )
        )

        // Leg 3: Road from Arrival Seaport -> Destination
        if (depPort.id != arrPort.id) {
            val fromPortDist = calculateDistanceMeters(
                arrPort.location.latitude, arrPort.location.longitude,
                destination.latitude, destination.longitude
            ).toDouble()

            if (fromPortDist > 300.0) {
                val leg3 = fetchOsrmDrivingOnly(arrPort.location, destination, destinationName)
                if (leg3 != null) {
                    segments.addAll(leg3.segments)
                    allPoints.addAll(leg3.polylinePoints)
                    totalDistance += leg3.totalDistanceMeters
                    totalDuration += leg3.totalDurationSeconds
                    leg3.steps.forEach { s ->
                        steps.add(s.copy(index = steps.size))
                    }
                } else {
                    val road = listOf(arrPort.location, destination)
                    segments.add(RouteSegment(RouteSegmentType.ROAD, road))
                    allPoints.addAll(road)
                    totalDistance += fromPortDist
                    totalDuration += fromPortDist / 14.0
                    steps.add(
                        RouteStep(
                            index = steps.size,
                            instructionAr = "بعد النزول في ${arrPort.nameAr}، أكمل بالسيارة إلى $destinationName",
                            voiceAnnouncementAr = "بعد الوصول إلى ${arrPort.nameAr}، أكمل طريقك بالسيارة إلى $destinationName",
                            roadName = arrPort.nameAr,
                            distanceMeters = fromPortDist,
                            durationSeconds = fromPortDist / 14.0,
                            direction = ManeuverDirection.ARRIVE,
                            location = destination
                        )
                    )
                }
            }
        }

        val hubs = buildList {
            add(RouteHubMarker(depPort, isDeparture = true))
            if (arrPort.id != depPort.id) {
                add(RouteHubMarker(arrPort, isDeparture = false))
            }
        }

        return NavigationRoute(
            origin = origin,
            destination = destination,
            destinationName = destinationName,
            resolvedMode = TravelMode.SEA,
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDuration,
            polylinePoints = allPoints.ifEmpty { listOf(origin, destination) },
            segments = segments,
            hubMarkers = hubs,
            vehicleLocation = shipPos,
            vehicleBearingDegrees = shipBearing,
            steps = steps,
            isDirectFallback = false
        )
    }

    private fun buildCurvedFlightArc(start: LatLngPoint, end: LatLngPoint): List<LatLngPoint> {
        val pointsCount = 28
        val midLat = (start.latitude + end.latitude) / 2.0
        val midLon = (start.longitude + end.longitude) / 2.0
        val dLat = end.latitude - start.latitude
        val dLon = end.longitude - start.longitude
        val chord = kotlin.math.hypot(dLat, dLon).coerceAtLeast(0.01)
        // Lift the flight arc gracefully northward for an authentic airborne trajectory feel
        val arcHeight = (chord * 0.16).coerceIn(0.15, 9.5)
        val ctrlLat = (midLat + arcHeight).coerceIn(-82.0, 82.0)
        val ctrlLon = midLon

        return (0..pointsCount).map { i ->
            val t = i.toDouble() / pointsCount
            val oneMinusT = 1.0 - t
            val lat = oneMinusT * oneMinusT * start.latitude +
                2 * oneMinusT * t * ctrlLat +
                t * t * end.latitude
            val lon = oneMinusT * oneMinusT * start.longitude +
                2 * oneMinusT * t * ctrlLon +
                t * t * end.longitude
            LatLngPoint(lat, lon)
        }
    }

    private fun buildSmoothSeaLane(start: LatLngPoint, end: LatLngPoint): List<LatLngPoint> {
        val pointsCount = 20
        val midLat = (start.latitude + end.latitude) / 2.0
        val midLon = (start.longitude + end.longitude) / 2.0
        val dLat = end.latitude - start.latitude
        val dLon = end.longitude - start.longitude
        val bendLat = (midLat - dLon * 0.08).coerceIn(-82.0, 82.0)
        val bendLon = (midLon + dLat * 0.08).coerceIn(-179.0, 179.0)

        return (0..pointsCount).map { i ->
            val t = i.toDouble() / pointsCount
            val oneMinusT = 1.0 - t
            val lat = oneMinusT * oneMinusT * start.latitude +
                2 * oneMinusT * t * bendLat +
                t * t * end.latitude
            val lon = oneMinusT * oneMinusT * start.longitude +
                2 * oneMinusT * t * bendLon +
                t * t * end.longitude
            LatLngPoint(lat, lon)
        }
    }

    private fun translateOsrmStep(
        index: Int,
        dto: OsrmStepDto,
        destinationName: String,
        fallbackPoint: LatLngPoint
    ): RouteStep {
        val maneuverType = dto.maneuver?.type.orEmpty().lowercase()
        val modifier = dto.maneuver?.modifier.orEmpty().lowercase()
        val isFerry = dto.mode.equals("ferry", ignoreCase = true)
        val road = dto.name?.trim().orEmpty()
        val locPair = dto.maneuver?.location
        val point = if (locPair != null && locPair.size >= 2) {
            LatLngPoint(latitude = locPair[1], longitude = locPair[0])
        } else {
            fallbackPoint
        }

        val direction = if (isFerry) ManeuverDirection.SHIP else resolveDirection(maneuverType, modifier)
        val roadSuffix = if (road.isNotBlank()) " عبر $road" else ""
        val intoRoadSuffix = if (road.isNotBlank()) " نحو $road" else ""

        val actionText = when (direction) {
            ManeuverDirection.SHIP -> if (road.isNotBlank()) "اعبر البحر بالسفينة عبر $road" else "اعبر البحر بالسفينة للأمام"
            ManeuverDirection.AIRPORT -> "توجّه نحو $road"
            ManeuverDirection.FLIGHT -> "رحلة جوية نحو $road"
            ManeuverDirection.PORT -> "توجّه إلى الميناء $road"
            ManeuverDirection.DEPART -> if (road.isNotBlank()) "انطلق عبر $road" else "انطلق للأمام"
            ManeuverDirection.ARRIVE -> "وصلت إلى وجهتك: $destinationName"
            ManeuverDirection.ROUNDABOUT -> {
                val exit = dto.maneuver?.exit
                if (exit != null && exit > 0) {
                    "ادخل الدوار ثم اسلك المخرج رقم $exit$intoRoadSuffix"
                } else {
                    "اعبر الدوار$intoRoadSuffix"
                }
            }
            ManeuverDirection.U_TURN -> "استدر للخلف (U-Turn)$intoRoadSuffix"
            ManeuverDirection.RIGHT -> "انعطف يميناً$intoRoadSuffix"
            ManeuverDirection.SLIGHT_RIGHT -> "انحرف قليلاً لليمين$intoRoadSuffix"
            ManeuverDirection.SHARP_RIGHT -> "انعطف بحدة لليمين$intoRoadSuffix"
            ManeuverDirection.LEFT -> "انعطف يساراً$intoRoadSuffix"
            ManeuverDirection.SLIGHT_LEFT -> "انحرف قليلاً لليسار$intoRoadSuffix"
            ManeuverDirection.SHARP_LEFT -> "انعطف بحدة لليسار$intoRoadSuffix"
            ManeuverDirection.STRAIGHT -> "واصل السير مستقيماً$roadSuffix"
        }

        val voiceText = if (direction == ManeuverDirection.ARRIVE) {
            "وصلت إلى وجهتك $destinationName بالسلامة"
        } else if (dto.distance >= 25.0) {
            "$actionText واستمر لمسافة ${formatRouteDistanceAr(dto.distance)}"
        } else {
            actionText
        }

        return RouteStep(
            index = index,
            instructionAr = actionText,
            voiceAnnouncementAr = voiceText,
            roadName = road,
            distanceMeters = dto.distance,
            durationSeconds = dto.duration,
            direction = direction,
            location = point
        )
    }

    private fun resolveDirection(type: String, modifier: String): ManeuverDirection {
        return when (type) {
            "depart" -> ManeuverDirection.DEPART
            "arrive" -> ManeuverDirection.ARRIVE
            "roundabout", "rotary", "roundabout turn", "exit roundabout", "exit rotary" ->
                ManeuverDirection.ROUNDABOUT
            else -> when (modifier) {
                "uturn" -> ManeuverDirection.U_TURN
                "right" -> ManeuverDirection.RIGHT
                "slight right" -> ManeuverDirection.SLIGHT_RIGHT
                "sharp right" -> ManeuverDirection.SHARP_RIGHT
                "left" -> ManeuverDirection.LEFT
                "slight left" -> ManeuverDirection.SLIGHT_LEFT
                "sharp left" -> ManeuverDirection.SHARP_LEFT
                else -> ManeuverDirection.STRAIGHT
            }
        }
    }

    private fun buildDirectCompassFallback(
        origin: LatLngPoint,
        destination: LatLngPoint,
        destinationName: String
    ): NavigationRoute {
        val straightMeters = calculateDistanceMeters(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude
        ).toDouble().coerceAtLeast(10.0)
        val estimatedSeconds = straightMeters / 13.8
        val bearingName = computeArabicCompassBearing(origin, destination)
        val distText = formatRouteDistanceAr(straightMeters)

        val steps = listOf(
            RouteStep(
                index = 0,
                instructionAr = "اتجه $bearingName نحو $destinationName",
                voiceAnnouncementAr = "اتجه $bearingName نحو $destinationName لمسافة $distText",
                roadName = "مسار مباشر",
                distanceMeters = straightMeters,
                durationSeconds = estimatedSeconds,
                direction = ManeuverDirection.DEPART,
                location = origin
            ),
            RouteStep(
                index = 1,
                instructionAr = "الوصول إلى $destinationName",
                voiceAnnouncementAr = "وصلت إلى وجهتك $destinationName",
                roadName = destinationName,
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                direction = ManeuverDirection.ARRIVE,
                location = destination
            )
        )

        val points = listOf(origin, destination)
        return NavigationRoute(
            origin = origin,
            destination = destination,
            destinationName = destinationName,
            resolvedMode = TravelMode.ROAD,
            totalDistanceMeters = straightMeters,
            totalDurationSeconds = estimatedSeconds,
            polylinePoints = points,
            segments = listOf(RouteSegment(RouteSegmentType.ROAD, points)),
            steps = steps,
            isDirectFallback = true
        )
    }

    private fun computeBearingDegrees(from: LatLngPoint, to: LatLngPoint): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    private fun computeArabicCompassBearing(from: LatLngPoint, to: LatLngPoint): String {
        val deg = computeBearingDegrees(from, to)
        return when {
            deg < 22.5 || deg >= 337.5 -> "شمالاً"
            deg < 67.5 -> "شمال شرق"
            deg < 112.5 -> "شرقاً"
            deg < 157.5 -> "جنوب شرق"
            deg < 202.5 -> "جنوباً"
            deg < 247.5 -> "جنوب غرب"
            deg < 292.5 -> "غرباً"
            else -> "شمال غرب"
        }
    }
}

fun formatRouteDistanceAr(meters: Double): String {
    return if (meters < 1000.0) {
        "${meters.roundToInt().coerceAtLeast(1)} م"
    } else {
        val km = meters / 1000.0
        if (km >= 100.0) {
            "${km.roundToInt()} كم"
        } else {
            String.format(java.util.Locale.US, "%.1f كم", km)
        }
    }
}

fun formatRouteDurationAr(seconds: Double): String {
    val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours <= 0 -> "$totalMinutes د"
        mins == 0 -> "$hours س"
        else -> "$hours س و $mins د"
    }
}
