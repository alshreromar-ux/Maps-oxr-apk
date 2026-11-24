package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.LatLngPoint
import com.example.model.PinCategory
import com.example.model.PinStatus
import com.example.ui.theme.CategoryCafeColor
import com.example.ui.theme.CategoryDealColor
import com.example.ui.theme.CategoryMaintenanceColor
import com.example.ui.theme.CategoryPharmacyColor
import com.example.ui.theme.CategoryRestaurantColor
import com.example.ui.theme.CategoryRoadReportColor
import com.example.ui.theme.StatusCrowdedColor
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

private enum class QuickAddPreset(
    val labelRes: Int,
    val icon: ImageVector,
    val color: Color,
    val category: PinCategory,
    val status: PinStatus,
    val titleHint: String,
    val defaultTitlePrefix: String
) {
    RESTAURANT(
        labelRes = R.string.quick_type_restaurant,
        icon = Icons.Rounded.Restaurant,
        color = CategoryRestaurantColor,
        category = PinCategory.RESTAURANT,
        status = PinStatus.OPEN,
        titleHint = "مثال: مطعم البركة — شارع المنصور",
        defaultTitlePrefix = ""
    ),
    PHARMACY(
        labelRes = R.string.quick_type_pharmacy,
        icon = Icons.Rounded.LocalPharmacy,
        color = CategoryPharmacyColor,
        category = PinCategory.PHARMACY,
        status = PinStatus.OPEN,
        titleHint = "مثال: صيدلية الشفاء المناوبة — تقاطع الجامعة",
        defaultTitlePrefix = ""
    ),
    ACCIDENT(
        labelRes = R.string.quick_type_accident,
        icon = Icons.Rounded.CarCrash,
        color = CategoryRoadReportColor,
        category = PinCategory.ROAD_REPORT,
        status = PinStatus.TRAFFIC_ALERT,
        titleHint = "مثال: حادث مروري قرب الجسر السريع",
        defaultTitlePrefix = "حادث مروري — "
    ),
    TRAFFIC(
        labelRes = R.string.quick_type_traffic,
        icon = Icons.Rounded.Traffic,
        color = StatusCrowdedColor,
        category = PinCategory.ROAD_REPORT,
        status = PinStatus.CROWDED,
        titleHint = "مثال: ازدحام سير كثيف على الطريق الدائري",
        defaultTitlePrefix = "ازدحام مروري — "
    ),
    MAINTENANCE(
        labelRes = R.string.quick_type_maintenance,
        icon = Icons.Rounded.Build,
        color = CategoryMaintenanceColor,
        category = PinCategory.MAINTENANCE,
        status = PinStatus.OPEN,
        titleHint = "مثال: محطة وقود وصيانة الأمل",
        defaultTitlePrefix = ""
    ),
    CAFE(
        labelRes = R.string.quick_type_cafe,
        icon = Icons.Rounded.Coffee,
        color = CategoryCafeColor,
        category = PinCategory.CAFE,
        status = PinStatus.OPEN,
        titleHint = "مثال: مقهى النخيل — الكورنيش",
        defaultTitlePrefix = ""
    ),
    OTHER(
        labelRes = R.string.quick_type_other,
        icon = Icons.Rounded.LocalOffer,
        color = CategoryDealColor,
        category = PinCategory.DEAL,
        status = PinStatus.DEAL_ACTIVE,
        titleHint = "مثال: محل أو خدمة أو عرض في منطقتي",
        defaultTitlePrefix = ""
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPinSheet(
    gpsLatLng: LatLngPoint?,
    mapCenterLatLng: LatLngPoint,
    customDropLatLng: LatLngPoint? = null,
    defaultCityName: String,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onCreatePin: (
        title: String,
        description: String,
        category: PinCategory,
        status: PinStatus,
        cityName: String,
        latitude: Double,
        longitude: Double,
        photoBase64: String?
    ) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fallbackCity = stringResource(R.string.pin_city_default)

    var useGpsPoint by remember(customDropLatLng, gpsLatLng) {
        mutableStateOf(customDropLatLng == null && gpsLatLng != null)
    }
    val targetLatLng = when {
        customDropLatLng != null -> customDropLatLng
        useGpsPoint && gpsLatLng != null -> gpsLatLng
        else -> mapCenterLatLng
    }

    var activePreset by remember { mutableStateOf(QuickAddPreset.RESTAURANT) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cityName by remember(defaultCityName) {
        mutableStateOf(defaultCityName.ifBlank { fallbackCity })
    }
    var selectedCategory by remember { mutableStateOf(PinCategory.RESTAURANT) }
    var selectedStatus by remember { mutableStateOf(PinCategory.RESTAURANT.defaultStatus) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var encodedPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                isProcessingPhoto = true
                scope.launch {
                    encodedPhotoBase64 = ImageUtils.encodeUriToBoundedBase64(context, uri)
                    isProcessingPhoto = false
                }
            }
        }
    )

    val isTitleValid = title.trim().length in 2..80
    val isCityValid = cityName.trim().length in 1..60

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.new_pin_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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

            // Coordinate status banner + optional GPS vs Map Center selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (useGpsPoint && customDropLatLng == null && gpsLatLng != null) {
                                "${stringResource(R.string.using_current_coords)} (${String.format("%.4f", targetLatLng.latitude)}, ${String.format("%.4f", targetLatLng.longitude)})"
                            } else {
                                "${stringResource(R.string.using_map_center_coords)} (${String.format("%.4f", targetLatLng.latitude)}, ${String.format("%.4f", targetLatLng.longitude)})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (customDropLatLng == null && gpsLatLng != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = useGpsPoint,
                                onClick = { useGpsPoint = true },
                                label = { Text(stringResource(R.string.use_gps_coords_chip)) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                            FilterChip(
                                selected = !useGpsPoint,
                                onClick = { useGpsPoint = false },
                                label = { Text(stringResource(R.string.use_map_center_chip)) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }

            // Visual Quick-Type Selector (Restaurant, Pharmacy, Accident, Traffic, Maintenance, Cafe, Other)
            Text(
                text = stringResource(R.string.pin_quick_type_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAddPreset.entries.forEach { preset ->
                    val isSelected = activePreset == preset
                    Surface(
                        onClick = {
                            val previousPrefix = activePreset.defaultTitlePrefix
                            activePreset = preset
                            selectedCategory = preset.category
                            selectedStatus = preset.status
                            val trimmed = title.trim()
                            if (trimmed.isEmpty() || (previousPrefix.isNotEmpty() && trimmed == previousPrefix.trim())) {
                                title = preset.defaultTitlePrefix
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) preset.color else preset.color.copy(alpha = 0.10f),
                        border = BorderStroke(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = if (isSelected) Color.Transparent else preset.color.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("add_pin_preset_${preset.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else preset.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(preset.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text(stringResource(R.string.pin_title_label)) },
                placeholder = { Text(activePreset.titleHint) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_title_input")
            )

            OutlinedTextField(
                value = cityName,
                onValueChange = { if (it.length <= 60) cityName = it },
                label = { Text(stringResource(R.string.pin_city_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_city_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 300) description = it },
                label = { Text(stringResource(R.string.pin_desc_label)) },
                placeholder = { Text(stringResource(R.string.pin_desc_hint)) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_description_input")
            )

            // Current Status selector
            Text(
                text = stringResource(R.string.pin_status_label),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PinStatus.entries.forEach { st ->
                    val selected = selectedStatus == st
                    FilterChip(
                        selected = selected,
                        onClick = { selectedStatus = st },
                        label = { Text(stringResource(st.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = st.color,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            // Optional Photo Picker (Zero-permission PickVisualMedia)
            if (selectedPhotoUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = stringResource(R.string.pin_photo_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            TextButton(
                                onClick = {
                                    selectedPhotoUri = null
                                    encodedPhotoBase64 = null
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(stringResource(R.string.pin_photo_remove))
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("pick_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pin_photo_pick))
                }
            }

            Button(
                onClick = {
                    onCreatePin(
                        title,
                        description,
                        selectedCategory,
                        selectedStatus,
                        cityName,
                        targetLatLng.latitude,
                        targetLatLng.longitude,
                        encodedPhotoBase64
                    )
                },
                enabled = isTitleValid && isCityValid && !isMutating && !isProcessingPhoto,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_pin_button")
            ) {
                if (isMutating || isProcessingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.publish_pin),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
