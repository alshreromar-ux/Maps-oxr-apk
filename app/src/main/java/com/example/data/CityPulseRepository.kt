package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CityPin
import com.example.model.PinCategory
import com.example.model.PinStatus
import com.example.model.PinStatusUpdate
import com.example.model.SavedPlace
import com.example.model.SavedPlaceType
import com.example.model.WorldCatalog
import com.example.model.calculateDistanceMeters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt

class CityPulseRepository(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth = Firebase.auth,
    private val localStore: SavedPlacesLocalStore? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localStore = SavedPlacesLocalStore(context.applicationContext)
    )

    // In-memory / local stores used in Guest Mode (when db == null and user has not signed in with Google)
    private val guestPinsFlow = MutableStateFlow<List<CityPin>>(buildInitialGuestWorldPins())
    private val guestUpdatesFlow = MutableStateFlow<Map<String, List<PinStatusUpdate>>>(emptyMap())
    private val guestSavedPlacesFlow = MutableStateFlow<List<SavedPlace>>(localStore?.loadAll().orEmpty())

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePins(): Flow<List<CityPin>> {
        val firestore = db ?: return guestPinsFlow
        return flow {
            val uid = requireUserId()
            val path = COLLECTION_PINS
            emitAll(
                firestore.collection(COLLECTION_PINS)
                    .whereEqualTo("visibility", VISIBILITY_PUBLIC)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CityPin::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        }
    }

    fun observeStatusUpdates(pinId: String): Flow<List<PinStatusUpdate>> {
        val firestore = db ?: return guestUpdatesFlow.map { it[pinId].orEmpty() }
        return flow {
            val uid = requireUserId()
            require(pinId.isNotBlank()) { "Pin ID cannot be blank" }
            val path = "$COLLECTION_PINS/$pinId/$SUBCOLLECTION_UPDATES"
            emitAll(
                firestore.collection(COLLECTION_PINS)
                    .document(pinId)
                    .collection(SUBCOLLECTION_UPDATES)
                    .whereEqualTo("visibility", VISIBILITY_PUBLIC)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            PinStatusUpdate::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        }
    }

    suspend fun createPin(
        title: String,
        description: String,
        category: PinCategory,
        status: PinStatus,
        latitude: Double,
        longitude: Double,
        cityName: String,
        photoBase64: String?,
        authorName: String
    ): Result<String> {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanCity = cityName.trim().ifBlank { "مدينتي" }.take(60)
        val cleanAuthor = authorName.trim().ifBlank { "مستخدم" }.take(60)

        val firestore = db
        if (firestore == null) {
            return try {
                require(cleanTitle.length in 2..80) { "عنوان النقطة يجب أن يكون بين حرفين و80 حرفاً." }
                require(cleanDesc.length <= 300) { "الوصف طويل جداً (الحد الأقصى 300 حرف)." }
                require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات الموقع غير صالحة." }
                val newId = "guest_${UUID.randomUUID().toString().take(12)}"
                val now = Timestamp.now()
                val newPin = CityPin(
                    id = newId,
                    visibility = VISIBILITY_PUBLIC,
                    title = cleanTitle,
                    description = cleanDesc,
                    category = category.name,
                    status = status.name,
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cleanCity,
                    photoBase64 = photoBase64,
                    confirmationsCount = 1L,
                    createdBy = GUEST_USER_ID,
                    createdByName = cleanAuthor,
                    updatedBy = GUEST_USER_ID,
                    updatedByName = cleanAuthor,
                    createdAt = now,
                    updatedAt = now
                )
                guestPinsFlow.value = listOf(newPin) + guestPinsFlow.value
                Result.success(newId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        var docPath = COLLECTION_PINS
        return try {
            val uid = requireUserId()
            require(cleanTitle.length in 2..80) { "عنوان النقطة يجب أن يكون بين حرفين و80 حرفاً." }
            require(cleanDesc.length <= 300) { "الوصف طويل جداً (الحد الأقصى 300 حرف)." }
            require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات الموقع غير صالحة." }
            if (photoBase64 != null) {
                require(photoBase64.length <= 120_000) { "حجم الصورة كبير جداً." }
            }

            val docRef = firestore.collection(COLLECTION_PINS).document()
            docPath = docRef.path
            val payload = mapOf(
                "visibility" to VISIBILITY_PUBLIC,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category.name,
                "status" to status.name,
                "latitude" to latitude,
                "longitude" to longitude,
                "cityName" to cleanCity,
                "photoBase64" to photoBase64,
                "confirmationsCount" to 1L,
                "createdBy" to uid,
                "createdByName" to cleanAuthor,
                "updatedBy" to uid,
                "updatedByName" to cleanAuthor,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePinStatus(
        pinId: String,
        newStatus: PinStatus,
        note: String,
        authorName: String,
        incrementConfirmation: Boolean = true
    ): Result<Unit> {
        val cleanNote = note.trim().take(160)
        val cleanAuthor = authorName.trim().ifBlank { "مستخدم" }.take(60)

        val firestore = db
        if (firestore == null) {
            return try {
                require(pinId.isNotBlank()) { "معرف النقطة غير صالح." }
                val now = Timestamp.now()
                guestPinsFlow.value = guestPinsFlow.value.map { pin ->
                    if (pin.id == pinId) {
                        pin.copy(
                            status = newStatus.name,
                            updatedBy = GUEST_USER_ID,
                            updatedByName = cleanAuthor,
                            updatedAt = now,
                            confirmationsCount = if (incrementConfirmation) pin.confirmationsCount + 1L else pin.confirmationsCount
                        )
                    } else {
                        pin
                    }
                }
                val newUpdate = PinStatusUpdate(
                    id = "upd_${UUID.randomUUID().toString().take(8)}",
                    pinId = pinId,
                    visibility = VISIBILITY_PUBLIC,
                    status = newStatus.name,
                    note = cleanNote,
                    authorId = GUEST_USER_ID,
                    authorName = cleanAuthor,
                    createdAt = now
                )
                val existingList = guestUpdatesFlow.value[pinId].orEmpty()
                guestUpdatesFlow.value = guestUpdatesFlow.value + (pinId to (listOf(newUpdate) + existingList))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val docPath = "$COLLECTION_PINS/$pinId"
        return try {
            val uid = requireUserId()
            require(pinId.isNotBlank()) { "معرف النقطة غير صالح." }

            val pinRef = firestore.collection(COLLECTION_PINS).document(pinId)
            val updateRef = pinRef.collection(SUBCOLLECTION_UPDATES).document()

            val pinUpdatePayload = buildMap<String, Any> {
                put("status", newStatus.name)
                put("updatedBy", uid)
                put("updatedByName", cleanAuthor)
                put("updatedAt", FieldValue.serverTimestamp())
                if (incrementConfirmation) {
                    put("confirmationsCount", FieldValue.increment(1L))
                }
            }

            val statusLogPayload = mapOf(
                "pinId" to pinId,
                "visibility" to VISIBILITY_PUBLIC,
                "status" to newStatus.name,
                "note" to cleanNote,
                "authorId" to uid,
                "authorName" to cleanAuthor,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = firestore.batch()
            batch.update(pinRef, pinUpdatePayload)
            batch.set(updateRef, statusLogPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePin(pinId: String): Result<Unit> {
        val firestore = db
        if (firestore == null) {
            guestPinsFlow.value = guestPinsFlow.value.filterNot { it.id == pinId }
            return Result.success(Unit)
        }

        val docPath = "$COLLECTION_PINS/$pinId"
        return try {
            requireUserId()
            require(pinId.isNotBlank()) { "معرف النقطة غير صالح." }
            firestore.collection(COLLECTION_PINS).document(pinId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedDemoPins(
        centerLat: Double,
        centerLng: Double,
        authorName: String,
        existingTitles: Set<String> = emptySet()
    ): Result<Int> {
        val localSamples = buildLocalSamples(centerLat, centerLng)
        val worldSamples = buildWorldSamples()

        val firestore = db
        if (firestore == null) {
            val currentTitles = guestPinsFlow.value.map { it.title }.toSet()
            val now = Timestamp.now()
            val toAdd = (worldSamples + localSamples)
                .filter { it.title !in currentTitles }
                .mapIndexed { idx, sample ->
                    CityPin(
                        id = "guest_seed_${idx}_${System.currentTimeMillis()}",
                        visibility = VISIBILITY_PUBLIC,
                        title = sample.title,
                        description = sample.description,
                        category = sample.category.name,
                        status = sample.status.name,
                        latitude = sample.lat,
                        longitude = sample.lng,
                        cityName = sample.cityName,
                        confirmationsCount = 1L,
                        createdBy = "community_world",
                        createdByName = "نبض العالم",
                        updatedBy = "community_world",
                        updatedByName = "نبض العالم",
                        createdAt = now,
                        updatedAt = now
                    )
                }
            if (toAdd.isNotEmpty()) {
                guestPinsFlow.value = guestPinsFlow.value + toAdd
            }
            return Result.success(toAdd.size)
        }

        return try {
            val uid = requireUserId()
            val cleanAuthor = authorName.trim().ifBlank { "مستخدم" }.take(60)
            var addedCount = 0

            val pendingSamples = (worldSamples + localSamples)
                .filter { it.title !in existingTitles }

            pendingSamples.chunked(400).forEach { chunk ->
                val chunkBatch = firestore.batch()
                chunk.forEach { sample ->
                    val ref = firestore.collection(COLLECTION_PINS).document()
                    val payload = mapOf(
                        "visibility" to VISIBILITY_PUBLIC,
                        "title" to sample.title,
                        "description" to sample.description,
                        "category" to sample.category.name,
                        "status" to sample.status.name,
                        "latitude" to sample.lat,
                        "longitude" to sample.lng,
                        "cityName" to sample.cityName,
                        "confirmationsCount" to 1L,
                        "createdBy" to uid,
                        "createdByName" to cleanAuthor,
                        "updatedBy" to uid,
                        "updatedByName" to cleanAuthor,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    chunkBatch.set(ref, payload)
                    addedCount++
                }
                chunkBatch.commit().await()
            }

            Result.success(addedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, COLLECTION_PINS)
            Result.failure(e)
        }
    }

    private fun buildInitialGuestWorldPins(): List<CityPin> {
        val now = Timestamp.now()
        val worldPins = buildWorldSamples().mapIndexed { index, sample ->
            CityPin(
                id = "world_pin_$index",
                visibility = VISIBILITY_PUBLIC,
                title = sample.title,
                description = sample.description,
                category = sample.category.name,
                status = sample.status.name,
                latitude = sample.lat,
                longitude = sample.lng,
                cityName = sample.cityName,
                confirmationsCount = (index % 5 + 1).toLong(),
                createdBy = "community_world",
                createdByName = "نبض العالم",
                updatedBy = "community_world",
                updatedByName = "نبض العالم",
                createdAt = now,
                updatedAt = now
            )
        }
        return worldPins
    }

    private fun buildLocalSamples(centerLat: Double, centerLng: Double): List<SeedSample> = listOf(
        SeedSample(
            title = "صيدلية النبض المناوبة القريبة",
            description = "مناوبة ليلية 24 ساعة مع توفر أدوية الطوارئ وخدمة التوصيل السريع.",
            category = PinCategory.PHARMACY,
            status = PinStatus.OPEN,
            lat = (centerLat + 0.0042).coerceIn(-89.9, 89.9),
            lng = (centerLng + 0.0031).coerceIn(-179.9, 179.9),
            cityName = "منطقتي"
        ),
        SeedSample(
            title = "مطعم ومشويات الساحة — خصم 25%",
            description = "عرض لحظي على وجبات الغداء، الجلسات متوفرة حالياً.",
            category = PinCategory.RESTAURANT,
            status = PinStatus.DEAL_ACTIVE,
            lat = (centerLat - 0.0031).coerceIn(-89.9, 89.9),
            lng = (centerLng + 0.0048).coerceIn(-179.9, 179.9),
            cityName = "منطقتي"
        ),
        SeedSample(
            title = "بلاغ مروري: أعمال صيانة طريق",
            description = "تباطؤ مروري بسبب إغلاق مسار واحد، يُنصح بطريق بديل.",
            category = PinCategory.ROAD_REPORT,
            status = PinStatus.TRAFFIC_ALERT,
            lat = (centerLat + 0.0018).coerceIn(-89.9, 89.9),
            lng = (centerLng - 0.0062).coerceIn(-179.9, 179.9),
            cityName = "منطقتي"
        )
    )

    private fun buildWorldSamples(): List<SeedSample> = WorldCatalog.cities
        .filter { it.sampleTitle.isNotBlank() }
        .map { city ->
            SeedSample(
                title = city.sampleTitle.take(80),
                description = city.sampleDescription.take(300),
                category = city.sampleCategory,
                status = city.sampleStatus,
                lat = city.location.latitude,
                lng = city.location.longitude,
                cityName = "${city.cityName}، ${city.countryName}".take(60)
            )
        }

    private data class SeedSample(
        val title: String,
        val description: String,
        val category: PinCategory,
        val status: PinStatus,
        val lat: Double,
        val lng: Double,
        val cityName: String
    )

    // ==================== Saved Places (Home, Work, Favorites) ====================

    fun observeSavedPlaces(): Flow<List<SavedPlace>> {
        val firestore = db ?: return guestSavedPlacesFlow
        return flow {
            val uid = requireUserId()
            val path = "$COLLECTION_USERS/$uid/$SUBCOLLECTION_SAVED_PLACES"
            emitAll(
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(SUBCOLLECTION_SAVED_PLACES)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            SavedPlace::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedWith(
                            compareBy<SavedPlace> {
                                when (it.resolvedType()) {
                                    SavedPlaceType.HOME -> 0
                                    SavedPlaceType.WORK -> 1
                                    SavedPlaceType.FAVORITE -> 2
                                }
                            }.thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                        )
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        }
    }

    suspend fun saveHomeOrWork(
        type: SavedPlaceType,
        title: String,
        subtitle: String,
        latitude: Double,
        longitude: Double,
        cityName: String
    ): Result<String> {
        require(type == SavedPlaceType.HOME || type == SavedPlaceType.WORK) {
            "Slot must be HOME or WORK"
        }
        val docId = if (type == SavedPlaceType.HOME) DOC_ID_HOME else DOC_ID_WORK
        val defaultTitle = if (type == SavedPlaceType.HOME) "بيتي" else "عملي"
        val cleanTitle = title.trim().ifBlank { defaultTitle }.take(80)
        val cleanSubtitle = subtitle.trim().take(160)
        val cleanCity = cityName.trim().ifBlank { "مدينتي" }.take(60)

        val firestore = db
        if (firestore == null) {
            return try {
                require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات غير صالحة." }
                val now = Timestamp.now()
                val existingCreatedAt = guestSavedPlacesFlow.value.firstOrNull { it.id == docId }?.createdAt ?: now
                val saved = SavedPlace(
                    id = docId,
                    placeType = type.name,
                    title = cleanTitle,
                    subtitle = cleanSubtitle,
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cleanCity,
                    sourcePinId = null,
                    createdAt = existingCreatedAt,
                    updatedAt = now
                )
                val updatedList = listOf(saved) + guestSavedPlacesFlow.value.filterNot { it.id == docId }
                guestSavedPlacesFlow.value = updatedList
                localStore?.saveAll(updatedList)
                Result.success(docId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        var docPath = "$COLLECTION_USERS/unknown/$SUBCOLLECTION_SAVED_PLACES/$docId"
        return try {
            val uid = requireUserId()
            require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات غير صالحة." }
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_SAVED_PLACES)
                .document(docId)
            docPath = docRef.path

            val exists = docRef.get().await().exists()
            if (exists) {
                val updatePayload = mapOf(
                    "placeType" to type.name,
                    "title" to cleanTitle,
                    "subtitle" to cleanSubtitle,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "cityName" to cleanCity,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "placeType" to type.name,
                    "title" to cleanTitle,
                    "subtitle" to cleanSubtitle,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "cityName" to cleanCity,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoritePlace(
        title: String,
        subtitle: String,
        latitude: Double,
        longitude: Double,
        cityName: String,
        sourcePinId: String?,
        currentSavedPlaces: List<SavedPlace>
    ): Result<Boolean> {
        val cleanTitle = title.trim().ifBlank { "مكان مفضّل" }.take(80)
        val cleanSubtitle = subtitle.trim().take(160)
        val cleanCity = cityName.trim().ifBlank { "مدينتي" }.take(60)
        val cleanPinId = sourcePinId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.take(120)?.ifBlank { null }

        val existingFav = currentSavedPlaces.firstOrNull { saved ->
            saved.resolvedType() == SavedPlaceType.FAVORITE && (
                (cleanPinId != null && saved.sourcePinId == cleanPinId) ||
                    calculateDistanceMeters(saved.latitude, saved.longitude, latitude, longitude) < 45f
                )
        }

        if (existingFav != null) {
            return removeSavedPlace(existingFav.id).map { false }
        }

        val docId = buildFavoriteDocId(cleanPinId, latitude, longitude)
        val firestore = db
        if (firestore == null) {
            return try {
                require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات غير صالحة." }
                val now = Timestamp.now()
                val newFav = SavedPlace(
                    id = docId,
                    placeType = SavedPlaceType.FAVORITE.name,
                    title = cleanTitle,
                    subtitle = cleanSubtitle,
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cleanCity,
                    sourcePinId = cleanPinId,
                    createdAt = now,
                    updatedAt = now
                )
                val updatedList = listOf(newFav) + guestSavedPlacesFlow.value.filterNot { it.id == docId }
                guestSavedPlacesFlow.value = updatedList
                localStore?.saveAll(updatedList)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        var docPath = "$COLLECTION_USERS/unknown/$SUBCOLLECTION_SAVED_PLACES/$docId"
        return try {
            val uid = requireUserId()
            require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات غير صالحة." }
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_SAVED_PLACES)
                .document(docId)
            docPath = docRef.path

            val payload = mapOf(
                "placeType" to SavedPlaceType.FAVORITE.name,
                "title" to cleanTitle,
                "subtitle" to cleanSubtitle,
                "latitude" to latitude,
                "longitude" to longitude,
                "cityName" to cleanCity,
                "sourcePinId" to cleanPinId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeSavedPlace(placeId: String): Result<Unit> {
        val firestore = db
        if (firestore == null) {
            val updatedList = guestSavedPlacesFlow.value.filterNot { it.id == placeId }
            guestSavedPlacesFlow.value = updatedList
            localStore?.saveAll(updatedList)
            return Result.success(Unit)
        }

        var docPath = "$COLLECTION_USERS/unknown/$SUBCOLLECTION_SAVED_PLACES/$placeId"
        return try {
            val uid = requireUserId()
            require(placeId.isNotBlank()) { "معرف المكان غير صالح." }
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_SAVED_PLACES)
                .document(placeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildFavoriteDocId(cleanPinId: String?, lat: Double, lng: Double): String {
        if (!cleanPinId.isNullOrBlank()) {
            return "fav_pin_${cleanPinId.take(100)}"
        }
        val latInt = (lat * 10000.0).roundToInt().toString().replace('-', 'n')
        val lngInt = (lng * 10000.0).roundToInt().toString().replace('-', 'w')
        return "fav_geo_${latInt}_${lngInt}".take(120)
    }

    companion object {
        const val COLLECTION_PINS = "city_pins"
        const val SUBCOLLECTION_UPDATES = "status_updates"
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_SAVED_PLACES = "saved_places"
        const val DOC_ID_HOME = "home"
        const val DOC_ID_WORK = "work"
        const val VISIBILITY_PUBLIC = "public"
        const val GUEST_USER_ID = "guest_session"
    }
}
