package com.example.data

import android.content.Context
import androidx.annotation.Keep
import com.example.model.SavedPlace
import com.example.model.SavedPlaceType
import com.google.firebase.Timestamp
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Keep
private data class SavedPlaceJsonRecord(
    val id: String,
    val placeType: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val sourcePinId: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

class SavedPlacesLocalStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SavedPlaceJsonRecord::class.java)
    private val adapter = moshi.adapter<List<SavedPlaceJsonRecord>>(listType)

    fun loadAll(): List<SavedPlace> {
        val raw = prefs.getString(KEY_SAVED_PLACES, null) ?: return emptyList()
        return runCatching {
            adapter.fromJson(raw).orEmpty().map { rec ->
                val ts = Timestamp(rec.updatedAtMillis / 1000L, ((rec.updatedAtMillis % 1000L) * 1_000_000L).toInt())
                SavedPlace(
                    id = rec.id,
                    placeType = SavedPlaceType.fromRaw(rec.placeType).name,
                    title = rec.title,
                    subtitle = rec.subtitle,
                    latitude = rec.latitude,
                    longitude = rec.longitude,
                    cityName = rec.cityName,
                    sourcePinId = rec.sourcePinId,
                    createdAt = ts,
                    updatedAt = ts
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveAll(places: List<SavedPlace>) {
        val records = places.map { place ->
            SavedPlaceJsonRecord(
                id = place.id,
                placeType = place.resolvedType().name,
                title = place.title,
                subtitle = place.subtitle,
                latitude = place.latitude,
                longitude = place.longitude,
                cityName = place.cityName,
                sourcePinId = place.sourcePinId,
                updatedAtMillis = place.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
        prefs.edit().putString(KEY_SAVED_PLACES, adapter.toJson(records)).apply()
    }

    private companion object {
        const val PREFS_NAME = "city_pulse_saved_places"
        const val KEY_SAVED_PLACES = "guest_saved_places_v1"
    }
}
