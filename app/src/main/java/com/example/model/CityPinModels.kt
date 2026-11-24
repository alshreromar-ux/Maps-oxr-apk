package com.example.model

import android.content.Context
import android.location.Location
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.ui.theme.CategoryCafeColor
import com.example.ui.theme.CategoryDealColor
import com.example.ui.theme.CategoryMaintenanceColor
import com.example.ui.theme.CategoryPharmacyColor
import com.example.ui.theme.CategoryRestaurantColor
import com.example.ui.theme.CategoryRoadReportColor
import com.example.ui.theme.StatusClosedColor
import com.example.ui.theme.StatusCrowdedColor
import com.example.ui.theme.StatusDealColor
import com.example.ui.theme.StatusOpenColor
import com.example.ui.theme.StatusTrafficColor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.concurrent.TimeUnit

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

enum class PinCategory(
    @StringRes val labelRes: Int,
    val color: Color,
    val icon: ImageVector,
    val defaultStatus: PinStatus
) {
    PHARMACY(
        labelRes = R.string.category_pharmacy,
        color = CategoryPharmacyColor,
        icon = Icons.Rounded.LocalPharmacy,
        defaultStatus = PinStatus.OPEN
    ),
    RESTAURANT(
        labelRes = R.string.category_restaurant,
        color = CategoryRestaurantColor,
        icon = Icons.Rounded.Restaurant,
        defaultStatus = PinStatus.OPEN
    ),
    MAINTENANCE(
        labelRes = R.string.category_maintenance,
        color = CategoryMaintenanceColor,
        icon = Icons.Rounded.Build,
        defaultStatus = PinStatus.OPEN
    ),
    CAFE(
        labelRes = R.string.category_cafe,
        color = CategoryCafeColor,
        icon = Icons.Rounded.Coffee,
        defaultStatus = PinStatus.OPEN
    ),
    DEAL(
        labelRes = R.string.category_deal,
        color = CategoryDealColor,
        icon = Icons.Rounded.LocalOffer,
        defaultStatus = PinStatus.DEAL_ACTIVE
    ),
    ROAD_REPORT(
        labelRes = R.string.category_road_report,
        color = CategoryRoadReportColor,
        icon = Icons.Rounded.Traffic,
        defaultStatus = PinStatus.TRAFFIC_ALERT
    );

    companion object {
        fun fromRaw(raw: String): PinCategory =
            entries.firstOrNull { it.name == raw } ?: RESTAURANT
    }
}

enum class PinStatus(
    @StringRes val labelRes: Int,
    val color: Color,
    val icon: ImageVector
) {
    OPEN(
        labelRes = R.string.status_open,
        color = StatusOpenColor,
        icon = Icons.Rounded.CheckCircle
    ),
    CROWDED(
        labelRes = R.string.status_crowded,
        color = StatusCrowdedColor,
        icon = Icons.Rounded.Groups
    ),
    DEAL_ACTIVE(
        labelRes = R.string.status_deal_active,
        color = StatusDealColor,
        icon = Icons.Rounded.LocalOffer
    ),
    CLOSED(
        labelRes = R.string.status_closed,
        color = StatusClosedColor,
        icon = Icons.Rounded.DoNotDisturbOn
    ),
    TRAFFIC_ALERT(
        labelRes = R.string.status_traffic_alert,
        color = StatusTrafficColor,
        icon = Icons.Rounded.WarningAmber
    );

    companion object {
        fun fromRaw(raw: String): PinStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

sealed interface QuickFilter {
    data object All : QuickFilter
    data object Nearest : QuickFilter
    data class Category(val category: PinCategory) : QuickFilter

    @StringRes
    fun labelRes(): Int = when (this) {
        All -> R.string.filter_all
        Nearest -> R.string.filter_nearest
        is Category -> category.labelRes
    }

    fun icon(): ImageVector = when (this) {
        All -> Icons.Rounded.Public
        Nearest -> Icons.Rounded.MyLocation
        is Category -> category.icon
    }

    fun accentColor(defaultPrimary: Color): Color = when (this) {
        All -> defaultPrimary
        Nearest -> defaultPrimary
        is Category -> category.color
    }
}

@Keep
@IgnoreExtraProperties
data class CityPin(
    @DocumentId val id: String = "",
    val visibility: String = "public",
    val title: String = "",
    val description: String = "",
    val category: String = PinCategory.RESTAURANT.name,
    val status: String = PinStatus.OPEN.name,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cityName: String = "",
    val photoBase64: String? = null,
    val confirmationsCount: Long = 1L,
    val createdBy: String = "",
    val createdByName: String = "",
    val updatedBy: String = "",
    val updatedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): PinCategory = PinCategory.fromRaw(category)
    fun resolvedStatus(): PinStatus = PinStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class PinStatusUpdate(
    @DocumentId val id: String = "",
    val pinId: String = "",
    val visibility: String = "public",
    val status: String = PinStatus.OPEN.name,
    val note: String = "",
    val authorId: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedStatus(): PinStatus = PinStatus.fromRaw(status)
}

data class PinWithDistance(
    val pin: CityPin,
    val distanceMeters: Float?
)

enum class SavedPlaceType {
    HOME,
    WORK,
    FAVORITE;

    companion object {
        fun fromRaw(raw: String): SavedPlaceType =
            entries.firstOrNull { it.name == raw } ?: FAVORITE
    }
}

@Keep
@IgnoreExtraProperties
data class SavedPlace(
    @DocumentId val id: String = "",
    val placeType: String = SavedPlaceType.FAVORITE.name,
    val title: String = "",
    val subtitle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cityName: String = "",
    val sourcePinId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedType(): SavedPlaceType = SavedPlaceType.fromRaw(placeType)
}

data class SelectedMapPlace(
    val title: String,
    val subtitle: String,
    val cityName: String,
    val badge: String,
    val location: LatLngPoint,
    val sourcePinId: String? = null,
    val isResolvingDetails: Boolean = false
)

fun calculateDistanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, results)
    return results[0]
}

fun formatDistanceArabic(context: Context, distanceMeters: Float?): String {
    if (distanceMeters == null) return "—"
    return if (distanceMeters < 1000f) {
        context.getString(R.string.distance_meters_fmt, distanceMeters.toInt().coerceAtLeast(1))
    } else {
        context.getString(R.string.distance_km_fmt, distanceMeters / 1000f)
    }
}

fun formatRelativeTimeArabic(timestamp: Timestamp?): String {
    val timeMillis = timestamp?.toDate()?.time ?: return "الآن"
    val diffMillis = (System.currentTimeMillis() - timeMillis).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        minutes < 1 -> "الآن"
        minutes == 1L -> "منذ دقيقة"
        minutes == 2L -> "منذ دقيقتين"
        minutes in 3..10 -> "منذ $minutes دقائق"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours == 1L -> "منذ ساعة"
        hours == 2L -> "منذ ساعتين"
        hours in 3..10 -> "منذ $hours ساعات"
        hours < 24 -> "منذ $hours ساعة"
        days == 1L -> "منذ يوم"
        days == 2L -> "منذ يومين"
        else -> "منذ $days أيام"
    }
}
