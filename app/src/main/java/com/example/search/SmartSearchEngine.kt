package com.example.search

import com.example.model.CityPin
import com.example.model.HubKind
import com.example.model.LatLngPoint
import com.example.model.PinCategory
import com.example.model.TransportHub
import com.example.model.WorldCity
import com.example.model.calculateDistanceMeters
import kotlin.math.min

enum class SearchScopeFilter {
    ALL,
    PLACES,
    CITIES,
    HUBS,
    COMMUNITY
}

enum class SearchResultKind {
    COMMUNITY_PIN,
    PLACE_POI,
    CITY_COUNTRY,
    AIRPORT,
    SEAPORT
}

data class SmartSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val flagEmoji: String,
    val kind: SearchResultKind,
    val location: LatLngPoint,
    val recommendedZoom: Double,
    val distanceMeters: Float? = null,
    val relevanceScore: Double = 0.0,
    val categoryHint: PinCategory? = null,
    val pinId: String? = null,
    val worldCity: WorldCity? = null,
    val isLiveRemote: Boolean = false
)

object ArabicSearchNormalizer {

    private val diacriticsRegex = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
    private val whitespaceRegex = Regex("\\s+")

    // Bilingual aliases so typing English or Arabic finds cities, landmarks, and services seamlessly
    private val synonymGroups: List<List<String>> = listOf(
        listOf("الرياض", "رياض", "riyadh", "ryadh"),
        listOf("جدة", "جده", "jeddah", "jiddah"),
        listOf("مكة", "مكه", "مكة المكرمة", "makkah", "mecca"),
        listOf("المدينة", "المدينة المنورة", "madinah", "medina"),
        listOf("الدمام", "ددمام", "dammam"),
        listOf("الخبر", "khobar"),
        listOf("ابها", "أبها", "abha"),
        listOf("دبي", "dubai", "dxb"),
        listOf("ابوظبي", "أبوظبي", "abu dhabi", "abudhabi"),
        listOf("الدوحة", "دوحة", "doha"),
        listOf("الكويت", "kuwait"),
        listOf("المنامة", "البحرين", "manama", "bahrain"),
        listOf("مسقط", "عمان", "muscat", "oman"),
        listOf("بغداد", "baghdad", "bagdad"),
        listOf("البصرة", "بصرة", "basra"),
        listOf("اربيل", "أربيل", "erbil", "arbil"),
        listOf("النجف", "نجف", "najaf"),
        listOf("كربلاء", "karbala"),
        listOf("القاهرة", "قاهرة", "cairo"),
        listOf("الاسكندرية", "إسكندرية", "alexandria"),
        listOf("الجيزة", "giza"),
        listOf("عمان", "عمّان", "amman", "jordan"),
        listOf("بيروت", "beirut"),
        listOf("دمشق", "damascus"),
        listOf("الخرطوم", "khartoum"),
        listOf("تونس", "tunis", "tunisia"),
        listOf("الجزائر", "algiers", "algeria"),
        listOf("الدار البيضاء", "كازابلانكا", "casablanca"),
        listOf("الرباط", "rabat"),
        listOf("مراكش", "marrakech", "marrakesh"),
        listOf("طنجة", "tangier"),
        listOf("طرابلس", "tripoli"),
        listOf("صنعاء", "sanaa"),
        listOf("عدن", "aden"),
        listOf("اسطنبول", "إسطنبول", "istanbul"),
        listOf("انقرة", "أنقرة", "ankara"),
        listOf("لندن", "london"),
        listOf("باريس", "paris"),
        listOf("روما", "rome", "roma"),
        listOf("مدريد", "madrid"),
        listOf("برشلونة", "barcelona"),
        listOf("برلين", "berlin"),
        listOf("امستردام", "أمستردام", "amsterdam"),
        listOf("نيويورك", "new york", "nyc"),
        listOf("لوس انجلوس", "los angeles"),
        listOf("طوكيو", "tokyo"),
        listOf("سيول", "seoul"),
        listOf("بكين", "beijing"),
        listOf("سنغافورة", "singapore"),
        listOf("بانكوك", "bangkok"),
        listOf("كوالالمبور", "kuala lumpur"),
        listOf("جاكرتا", "jakarta"),
        listOf("سيدني", "sydney"),
        listOf("موسكو", "moscow"),
        listOf("مطار", "airport", "aerodrome"),
        listOf("ميناء", "port", "harbor", "ferry"),
        listOf("صيدلية", "صيدليه", "pharmacy"),
        listOf("مطعم", "مطاعم", "restaurant"),
        listOf("مستشفى", "مشفي", "hospital", "clinic"),
        listOf("مقهى", "كافيه", "كوفي", "cafe", "coffee"),
        listOf("محطة وقود", "محطة بنزين", "وقود", "بنزين", "fuel", "gas station"),
        listOf("فندق", "hotel"),
        listOf("مسجد", "جامع", "mosque"),
        listOf("مول", "سوق", "mall", "market")
    )

    private val normalizedSynonyms: Map<String, List<String>> by lazy {
        buildMap {
            synonymGroups.forEach { group ->
                val normGroup = group.map { normalize(it) }.distinct()
                normGroup.forEach { key ->
                    put(key, normGroup)
                }
            }
        }
    }

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        val sb = StringBuilder(raw.length)
        val noDiacritics = diacriticsRegex.replace(raw.trim().lowercase(), "")
        for (ch in noDiacritics) {
            sb.append(
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ة' -> 'ه'
                    'ى' -> 'ي'
                    'ؤ' -> 'و'
                    'ئ' -> 'ي'
                    else -> ch
                }
            )
        }
        return whitespaceRegex.replace(sb.toString(), " ").trim()
    }

    fun expandQueryVariants(rawQuery: String): List<String> {
        val norm = normalize(rawQuery)
        if (norm.isBlank()) return emptyList()
        val variants = linkedSetOf(norm)
        if (norm.startsWith("ال") && norm.length > 3) {
            variants.add(norm.removePrefix("ال"))
        }
        normalizedSynonyms[norm]?.let { variants.addAll(it) }
        return variants.toList()
    }

    /**
     * Scores text similarity from 0.0 (no match) up to ~1200.0 (exact match),
     * combining Arabic normalization, bilingual synonyms, prefix matching, and typo tolerance.
     */
    fun scoreCandidate(
        query: String,
        primaryTitle: String,
        secondaryText: String = "",
        distanceMeters: Float? = null
    ): Double {
        val variants = expandQueryVariants(query)
        if (variants.isEmpty()) return 0.0

        val normPrimary = normalize(primaryTitle)
        val normPrimaryNoAl = normPrimary.removePrefix("ال")
        val normSecondary = normalize(secondaryText)

        var bestTextScore = 0.0
        for (q in variants) {
            val qNoAl = q.removePrefix("ال")
            val candidateScore = when {
                normPrimary == q || normPrimaryNoAl == qNoAl -> 1050.0
                normPrimary.startsWith(q) || normPrimaryNoAl.startsWith(qNoAl) -> 760.0
                normPrimary.split(" ").any { it.startsWith(q) || it.removePrefix("ال").startsWith(qNoAl) } -> 620.0
                normPrimary.contains(q) || (qNoAl.length >= 2 && normPrimary.contains(qNoAl)) -> 440.0
                normSecondary.startsWith(q) -> 360.0
                normSecondary.contains(q) || (qNoAl.length >= 3 && normSecondary.contains(qNoAl)) -> 260.0
                q.length >= 4 && fuzzyWordMatch(qNoAl, normPrimaryNoAl) -> 165.0
                else -> 0.0
            }
            if (candidateScore > bestTextScore) {
                bestTextScore = candidateScore
            }
        }

        if (bestTextScore <= 0.0) return 0.0

        // Gentle proximity boost so closer cities/places edge out identical-name matches far away
        val proximityBoost = if (distanceMeters != null) {
            when {
                distanceMeters < 2_000f -> 180.0
                distanceMeters < 15_000f -> 120.0
                distanceMeters < 80_000f -> 60.0
                distanceMeters < 500_000f -> 20.0
                else -> 0.0
            }
        } else {
            0.0
        }

        return bestTextScore + proximityBoost
    }

    private fun fuzzyWordMatch(query: String, target: String): Boolean {
        val words = target.split(" ").filter { it.length >= 3 }
        for (word in words) {
            val w = word.removePrefix("ال")
            if (kotlin.math.abs(w.length - query.length) <= 2 && levenshtein(query, w) <= 2) {
                return true
            }
        }
        return false
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}

object LocalSmartSearchIndex {

    fun searchInstantLocal(
        query: String,
        pins: List<CityPin>,
        cities: List<WorldCity>,
        airports: List<TransportHub>,
        seaports: List<TransportHub>,
        referenceLocation: LatLngPoint?
    ): List<SmartSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<SmartSearchResult>()

        // 1. Live Community Pins on Map
        for (pin in pins) {
            val dist = referenceLocation?.let {
                calculateDistanceMeters(it.latitude, it.longitude, pin.latitude, pin.longitude)
            }
            val cat = pin.resolvedCategory()
            val score = ArabicSearchNormalizer.scoreCandidate(
                query = trimmed,
                primaryTitle = pin.title,
                secondaryText = "${pin.cityName} ${pin.description}",
                distanceMeters = dist
            )
            if (score > 0.0) {
                results.add(
                    SmartSearchResult(
                        id = "pin_${pin.id}",
                        title = pin.title,
                        subtitle = if (pin.cityName.isNotBlank()) pin.cityName else pin.description.take(45),
                        badge = "نقطة حية",
                        flagEmoji = "📍",
                        kind = SearchResultKind.COMMUNITY_PIN,
                        location = LatLngPoint(pin.latitude, pin.longitude),
                        recommendedZoom = 16.0,
                        distanceMeters = dist,
                        relevanceScore = score + 65.0,
                        categoryHint = cat,
                        pinId = pin.id
                    )
                )
            }
        }

        // 2. Global & Arab Airports
        for (airport in airports) {
            val dist = referenceLocation?.let {
                calculateDistanceMeters(it.latitude, it.longitude, airport.location.latitude, airport.location.longitude)
            }
            val score = ArabicSearchNormalizer.scoreCandidate(
                query = trimmed,
                primaryTitle = airport.nameAr,
                secondaryText = "${airport.cityAr} مطار",
                distanceMeters = dist
            )
            if (score > 0.0) {
                results.add(
                    SmartSearchResult(
                        id = "hub_${airport.id}",
                        title = airport.nameAr,
                        subtitle = airport.cityAr,
                        badge = "مطار",
                        flagEmoji = "✈️",
                        kind = SearchResultKind.AIRPORT,
                        location = airport.location,
                        recommendedZoom = 13.8,
                        distanceMeters = dist,
                        relevanceScore = score + 40.0
                    )
                )
            }
        }

        // 3. Global & Arab Seaports
        for (port in seaports) {
            val dist = referenceLocation?.let {
                calculateDistanceMeters(it.latitude, it.longitude, port.location.latitude, port.location.longitude)
            }
            val score = ArabicSearchNormalizer.scoreCandidate(
                query = trimmed,
                primaryTitle = port.nameAr,
                secondaryText = "${port.cityAr} ميناء بحري",
                distanceMeters = dist
            )
            if (score > 0.0) {
                results.add(
                    SmartSearchResult(
                        id = "hub_${port.id}",
                        title = port.nameAr,
                        subtitle = port.cityAr,
                        badge = "ميناء",
                        flagEmoji = "⚓",
                        kind = SearchResultKind.SEAPORT,
                        location = port.location,
                        recommendedZoom = 13.8,
                        distanceMeters = dist,
                        relevanceScore = score + 35.0
                    )
                )
            }
        }

        // 4. World & Arab Cities Catalog
        for (city in cities) {
            val dist = referenceLocation?.let {
                calculateDistanceMeters(it.latitude, it.longitude, city.location.latitude, city.location.longitude)
            }
            val score = ArabicSearchNormalizer.scoreCandidate(
                query = trimmed,
                primaryTitle = city.cityName,
                secondaryText = city.countryName,
                distanceMeters = dist
            )
            if (score > 0.0) {
                results.add(
                    SmartSearchResult(
                        id = "city_${city.id}",
                        title = city.cityName,
                        subtitle = city.countryName,
                        badge = "مدينة",
                        flagEmoji = city.flagEmoji,
                        kind = SearchResultKind.CITY_COUNTRY,
                        location = city.location,
                        recommendedZoom = 12.8,
                        distanceMeters = dist,
                        relevanceScore = score + 50.0,
                        categoryHint = city.sampleCategory,
                        worldCity = city
                    )
                )
            }
        }

        return results.sortedByDescending { it.relevanceScore }
    }
}
