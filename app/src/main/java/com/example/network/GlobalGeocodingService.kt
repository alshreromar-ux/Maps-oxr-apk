package com.example.network

import androidx.annotation.Keep
import com.example.model.LatLngPoint
import com.example.model.PinCategory
import com.example.model.PinStatus
import com.example.model.WorldCity
import com.example.model.WorldRegion
import com.example.model.calculateDistanceMeters
import com.example.model.countryCodeToFlagEmoji
import com.example.search.ArabicSearchNormalizer
import com.example.search.SearchResultKind
import com.example.search.SmartSearchResult
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// ==================== Nominatim DTOs ====================

@Keep
data class NominatimAddressDto(
    @Json(name = "amenity") val amenity: String? = null,
    @Json(name = "shop") val shop: String? = null,
    @Json(name = "road") val road: String? = null,
    @Json(name = "suburb") val suburb: String? = null,
    @Json(name = "neighbourhood") val neighbourhood: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "town") val town: String? = null,
    @Json(name = "village") val village: String? = null,
    @Json(name = "municipality") val municipality: String? = null,
    @Json(name = "county") val county: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)

@Keep
data class NominatimPlaceDto(
    @Json(name = "place_id") val placeId: Long = 0L,
    @Json(name = "lat") val lat: String = "",
    @Json(name = "lon") val lon: String = "",
    @Json(name = "name") val name: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "class") val osmClass: String? = null,
    @Json(name = "type") val osmType: String? = null,
    @Json(name = "addresstype") val addressType: String? = null,
    @Json(name = "address") val address: NominatimAddressDto? = null
)

interface NominatimApiService {
    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 25,
        @Query("accept-language") acceptLanguage: String = "ar,en"
    ): List<NominatimPlaceDto>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("zoom") zoom: Int = 10,
        @Query("accept-language") acceptLanguage: String = "ar,en"
    ): NominatimPlaceDto
}

// ==================== Photon Fast Autocomplete DTOs ====================

@Keep
data class PhotonResponseDto(
    @Json(name = "features") val features: List<PhotonFeatureDto>? = null
)

@Keep
data class PhotonFeatureDto(
    @Json(name = "geometry") val geometry: PhotonGeometryDto? = null,
    @Json(name = "properties") val properties: PhotonPropertiesDto? = null
)

@Keep
data class PhotonGeometryDto(
    @Json(name = "coordinates") val coordinates: List<Double>? = null
)

@Keep
data class PhotonPropertiesDto(
    @Json(name = "osm_id") val osmId: Long? = null,
    @Json(name = "osm_key") val osmKey: String? = null,
    @Json(name = "osm_value") val osmValue: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "street") val street: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "countrycode") val countryCode: String? = null
)

interface PhotonApiService {
    @GET("api/")
    suspend fun autocomplete(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null
    ): PhotonResponseDto
}

class GlobalGeocodingRepository {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CityPulseMap/1.0 (com.aistudio.citypulse.q7m4xk)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val nominatimApi: NominatimApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApiService::class.java)
    }

    private val photonApi: PhotonApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PhotonApiService::class.java)
    }

    private val queryCache: MutableMap<String, List<SmartSearchResult>> = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<SmartSearchResult>>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SmartSearchResult>>?): Boolean {
                return size > 60
            }
        }
    )

    /**
     * Parallel Dual-Engine Global Search (Photon Fast Typeahead + Nominatim Localized Arabic Geocoder)
     * with LRU cache, proximity biasing, Arabic badge mapping, and spatial deduplication.
     */
    suspend fun searchGlobalPlaces(
        query: String,
        biasLocation: LatLngPoint?
    ): Result<List<SmartSearchResult>> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return Result.success(emptyList())

        val latBucket = biasLocation?.latitude?.let { (it * 2.0).roundToInt() } ?: 0
        val lonBucket = biasLocation?.longitude?.let { (it * 2.0).roundToInt() } ?: 0
        val cacheKey = "${ArabicSearchNormalizer.normalize(cleanQuery)}_${latBucket}_$lonBucket"

        queryCache[cacheKey]?.let { cached ->
            return Result.success(cached)
        }

        return coroutineScope {
            val nominatimDeferred = async {
                try {
                    withTimeout(8_500L) {
                        nominatimApi.searchPlaces(query = cleanQuery)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }

            val photonDeferred = async {
                try {
                    withTimeout(6_500L) {
                        photonApi.autocomplete(
                            query = cleanQuery,
                            limit = 22,
                            lat = biasLocation?.latitude,
                            lon = biasLocation?.longitude
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }

            val nominatimDtos = nominatimDeferred.await()
            val photonResponse = photonDeferred.await()

            if (nominatimDtos == null && photonResponse == null) {
                return@coroutineScope Result.failure(IllegalStateException("تعذّر الاتصال بخدمات البحث العالمية"))
            }

            val merged = mutableListOf<SmartSearchResult>()

            // 1. Map Nominatim (Arabic-first authoritative results)
            nominatimDtos.orEmpty().forEach { dto ->
                mapNominatimToSmartResult(dto, cleanQuery, biasLocation)?.let { merged.add(it) }
            }

            // 2. Map Photon (Ultra-fast streets, shops, landmarks, POIs)
            photonResponse?.features.orEmpty().forEachIndexed { idx, feature ->
                mapPhotonToSmartResult(idx, feature, cleanQuery, biasLocation)?.let { candidate ->
                    val duplicateExists = merged.any { existing ->
                        val near = calculateDistanceMeters(
                            existing.location.latitude,
                            existing.location.longitude,
                            candidate.location.latitude,
                            candidate.location.longitude
                        ) < 450f
                        val sameName = ArabicSearchNormalizer.normalize(existing.title) ==
                            ArabicSearchNormalizer.normalize(candidate.title)
                        near && sameName
                    }
                    if (!duplicateExists) {
                        merged.add(candidate)
                    }
                }
            }

            val sorted = merged
                .distinctBy { "${ArabicSearchNormalizer.normalize(it.title)}_${(it.location.latitude * 100).roundToInt()}_${(it.location.longitude * 100).roundToInt()}" }
                .sortedByDescending { it.relevanceScore }
                .take(35)

            if (sorted.isNotEmpty()) {
                queryCache[cacheKey] = sorted
            }
            Result.success(sorted)
        }
    }

    suspend fun searchWorldCities(query: String): Result<List<WorldCity>> {
        return searchGlobalPlaces(query, biasLocation = null).map { results ->
            results.map { r ->
                r.worldCity ?: WorldCity(
                    id = r.id,
                    countryName = r.subtitle,
                    cityName = r.title,
                    flagEmoji = r.flagEmoji,
                    region = WorldRegion.ALL,
                    location = r.location,
                    subtitle = r.subtitle,
                    sampleCategory = r.categoryHint ?: PinCategory.CAFE,
                    sampleStatus = PinStatus.OPEN,
                    isFromGlobalSearch = true
                )
            }
        }
    }

    suspend fun resolveCityName(lat: Double, lng: Double): String? {
        return try {
            val dto = nominatimApi.reverseGeocode(lat = lat, lon = lng)
            val addr = dto.address
            val city = listOfNotNull(
                addr?.city,
                addr?.town,
                addr?.village,
                addr?.municipality,
                addr?.county,
                addr?.state,
                dto.name
            ).firstOrNull { it.isNotBlank() }
            val country = addr?.country?.takeIf { it.isNotBlank() }
            when {
                city != null && country != null && city != country -> "$city، $country"
                city != null -> city
                country != null -> country
                else -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    data class ResolvedPlaceDetails(
        val title: String,
        val subtitle: String,
        val cityName: String,
        val badge: String
    )

    suspend fun resolvePlaceDetails(lat: Double, lng: Double): ResolvedPlaceDetails? {
        return try {
            val dto = withTimeout(6_000L) {
                nominatimApi.reverseGeocode(lat = lat, lon = lng, zoom = 18)
            }
            val addr = dto.address
            val title = listOfNotNull(
                dto.name?.takeIf { it.isNotBlank() },
                addr?.amenity?.takeIf { it.isNotBlank() },
                addr?.shop?.takeIf { it.isNotBlank() },
                addr?.road?.takeIf { it.isNotBlank() },
                addr?.neighbourhood?.takeIf { it.isNotBlank() },
                addr?.suburb?.takeIf { it.isNotBlank() },
                addr?.city?.takeIf { it.isNotBlank() },
                addr?.town?.takeIf { it.isNotBlank() },
                addr?.country?.takeIf { it.isNotBlank() }
            ).firstOrNull() ?: return null

            val cityPart = listOfNotNull(
                addr?.city,
                addr?.town,
                addr?.village,
                addr?.municipality,
                addr?.state,
                addr?.country
            ).firstOrNull { it.isNotBlank() } ?: title

            val subtitleParts = listOfNotNull(
                addr?.road?.takeIf { it.isNotBlank() && it != title },
                addr?.neighbourhood?.takeIf { it.isNotBlank() && it != title },
                addr?.suburb?.takeIf { it.isNotBlank() && it != title },
                cityPart.takeIf { it != title },
                addr?.country?.takeIf { it.isNotBlank() && it != cityPart && it != title }
            ).distinct()

            val classification = classifyOsmTag(
                key = dto.osmClass.orEmpty().lowercase(),
                value = (dto.osmType ?: dto.addressType).orEmpty().lowercase()
            )

            ResolvedPlaceDetails(
                title = title,
                subtitle = subtitleParts.joinToString("، "),
                cityName = cityPart,
                badge = classification.badgeAr
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun mapNominatimToSmartResult(
        dto: NominatimPlaceDto,
        query: String,
        biasLocation: LatLngPoint?
    ): SmartSearchResult? {
        val lat = dto.lat.toDoubleOrNull() ?: return null
        val lon = dto.lon.toDoubleOrNull() ?: return null
        val addr = dto.address

        val title = listOfNotNull(
            dto.name?.takeIf { it.isNotBlank() },
            addr?.amenity,
            addr?.shop,
            addr?.road,
            addr?.city,
            addr?.town,
            addr?.village,
            addr?.municipality,
            addr?.state,
            addr?.country
        ).firstOrNull { it.isNotBlank() } ?: return null

        val cityPart = listOfNotNull(addr?.suburb, addr?.city, addr?.town, addr?.state)
            .firstOrNull { it.isNotBlank() && it != title }
        val countryPart = addr?.country?.takeIf { it.isNotBlank() && it != title }
        val subtitle = listOfNotNull(cityPart, countryPart).joinToString("، ")
            .ifBlank { dto.displayName.orEmpty() }

        val osmClass = dto.osmClass.orEmpty().lowercase()
        val osmType = (dto.osmType ?: dto.addressType).orEmpty().lowercase()
        val classification = classifyOsmTag(osmClass, osmType)
        val dist = biasLocation?.let {
            calculateDistanceMeters(it.latitude, it.longitude, lat, lon)
        }

        val score = ArabicSearchNormalizer.scoreCandidate(
            query = query,
            primaryTitle = title,
            secondaryText = subtitle,
            distanceMeters = dist
        ).coerceAtLeast(180.0) + 25.0 // slight boost for Arabic-localized Nominatim

        val flag = countryCodeToFlagEmoji(addr?.countryCode)
        val worldCity = if (classification.kind == SearchResultKind.CITY_COUNTRY) {
            WorldCity(
                id = "nom_${dto.placeId}_${lat}_${lon}",
                countryName = countryPart ?: subtitle,
                cityName = title,
                flagEmoji = flag,
                region = WorldRegion.ALL,
                location = LatLngPoint(lat, lon),
                subtitle = subtitle,
                sampleCategory = classification.categoryHint ?: PinCategory.CAFE,
                sampleStatus = PinStatus.OPEN,
                isFromGlobalSearch = true
            )
        } else null

        return SmartSearchResult(
            id = "nom_${dto.placeId}_${lat}_${lon}",
            title = title,
            subtitle = subtitle,
            badge = classification.badgeAr,
            flagEmoji = flag,
            kind = classification.kind,
            location = LatLngPoint(lat, lon),
            recommendedZoom = classification.zoom,
            distanceMeters = dist,
            relevanceScore = score,
            categoryHint = classification.categoryHint,
            worldCity = worldCity,
            isLiveRemote = true
        )
    }

    private fun mapPhotonToSmartResult(
        idx: Int,
        feature: PhotonFeatureDto,
        query: String,
        biasLocation: LatLngPoint?
    ): SmartSearchResult? {
        val coords = feature.geometry?.coordinates ?: return null
        if (coords.size < 2) return null
        val lon = coords[0]
        val lat = coords[1]
        val props = feature.properties ?: return null

        val title = listOfNotNull(
            props.name?.takeIf { it.isNotBlank() },
            props.street?.takeIf { it.isNotBlank() },
            props.city?.takeIf { it.isNotBlank() },
            props.state?.takeIf { it.isNotBlank() },
            props.country?.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: return null

        val subtitleParts = listOfNotNull(
            props.street?.takeIf { it.isNotBlank() && it != title },
            props.district?.takeIf { it.isNotBlank() && it != title },
            props.city?.takeIf { it.isNotBlank() && it != title },
            props.state?.takeIf { it.isNotBlank() && it != title },
            props.country?.takeIf { it.isNotBlank() && it != title }
        ).distinct()
        val subtitle = subtitleParts.joinToString("، ").ifBlank { props.country.orEmpty() }

        val osmKey = props.osmKey.orEmpty().lowercase()
        val osmVal = (props.osmValue ?: props.type).orEmpty().lowercase()
        val classification = classifyOsmTag(osmKey, osmVal)
        val dist = biasLocation?.let {
            calculateDistanceMeters(it.latitude, it.longitude, lat, lon)
        }

        val score = ArabicSearchNormalizer.scoreCandidate(
            query = query,
            primaryTitle = title,
            secondaryText = subtitle,
            distanceMeters = dist
        ).coerceAtLeast(150.0)

        val flag = countryCodeToFlagEmoji(props.countryCode)
        val osmId = props.osmId ?: idx.toLong()
        val worldCity = if (classification.kind == SearchResultKind.CITY_COUNTRY) {
            WorldCity(
                id = "ph_${osmId}_${lat}_${lon}",
                countryName = props.country.orEmpty().ifBlank { subtitle },
                cityName = title,
                flagEmoji = flag,
                region = WorldRegion.ALL,
                location = LatLngPoint(lat, lon),
                subtitle = subtitle,
                sampleCategory = classification.categoryHint ?: PinCategory.CAFE,
                sampleStatus = PinStatus.OPEN,
                isFromGlobalSearch = true
            )
        } else null

        return SmartSearchResult(
            id = "ph_${osmId}_${lat}_${lon}",
            title = title,
            subtitle = subtitle,
            badge = classification.badgeAr,
            flagEmoji = flag,
            kind = classification.kind,
            location = LatLngPoint(lat, lon),
            recommendedZoom = classification.zoom,
            distanceMeters = dist,
            relevanceScore = score,
            categoryHint = classification.categoryHint,
            worldCity = worldCity,
            isLiveRemote = true
        )
    }

    private data class PlaceClassification(
        val kind: SearchResultKind,
        val badgeAr: String,
        val zoom: Double,
        val categoryHint: PinCategory?
    )

    private fun classifyOsmTag(key: String, value: String): PlaceClassification {
        return when {
            key == "aeroway" || value in setOf("aerodrome", "airport", "terminal") ->
                PlaceClassification(SearchResultKind.AIRPORT, "مطار", 14.0, null)

            value in setOf("ferry_terminal", "harbour", "harbor", "port", "marina") ->
                PlaceClassification(SearchResultKind.SEAPORT, "ميناء", 14.2, null)

            value in setOf("pharmacy", "chemist") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "صيدلية", 16.5, PinCategory.PHARMACY)

            value in setOf("restaurant", "fast_food", "food_court") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "مطعم", 16.5, PinCategory.RESTAURANT)

            value in setOf("cafe", "coffee_shop") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "مقهى", 16.5, PinCategory.CAFE)

            value in setOf("hospital", "clinic", "doctors") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "مستشفى", 16.2, PinCategory.PHARMACY)

            value in setOf("fuel", "charging_station", "car_repair") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "محطة / صيانة", 16.2, PinCategory.MAINTENANCE)

            value in setOf("mosque", "place_of_worship") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "مسجد / معلم", 16.2, null)

            value in setOf("hotel", "mall", "supermarket", "marketplace", "bank", "university", "school") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "معلم / خدمة", 16.0, null)

            key == "highway" || value in setOf("residential", "primary", "secondary", "tertiary", "street", "road") ->
                PlaceClassification(SearchResultKind.PLACE_POI, "شارع / طريق", 15.8, null)

            value == "country" ->
                PlaceClassification(SearchResultKind.CITY_COUNTRY, "دولة", 5.2, null)

            value in setOf("city", "town", "village", "municipality", "state", "administrative") || key == "place" ->
                PlaceClassification(SearchResultKind.CITY_COUNTRY, "مدينة", 12.8, null)

            else ->
                PlaceClassification(SearchResultKind.PLACE_POI, "موقع", 15.5, null)
        }
    }
}
