package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocalAirport
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RoundaboutRight
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.network.ManeuverDirection
import com.example.network.NavigationRoute
import com.example.network.TravelMode
import com.example.network.formatRouteDistanceAr
import com.example.network.formatRouteDurationAr
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseNavy
import com.example.ui.theme.PulseOcean
import com.example.ui.theme.PulseTeal

@Composable
fun NavigationBanner(
    route: NavigationRoute?,
    selectedMode: TravelMode,
    currentStepIndex: Int,
    isLoadingRoute: Boolean,
    isVoiceMuted: Boolean,
    isSpeaking: Boolean,
    onSelectTravelMode: (TravelMode) -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onRepeatVoice: () -> Unit,
    onToggleMute: () -> Unit,
    onAskAiAboutRoute: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoadingRoute || route != null,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 640.dp)
                .testTag("navigation_hud_card"),
            shape = RoundedCornerShape(24.dp),
            color = PulseNavy.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.2.dp, PulseTeal.copy(alpha = 0.55f))
        ) {
            if (isLoadingRoute && route == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = PulseTeal
                    )
                    Text(
                        text = stringResource(R.string.calculating_route),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (route != null) {
                val safeIndex = currentStepIndex.coerceIn(0, (route.steps.size - 1).coerceAtLeast(0))
                val step = route.steps.getOrNull(safeIndex)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Maneuver icon + Current Turn Instruction + Stop & Mute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(colors = listOf(PulseTeal, PulseOcean))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = maneuverIconFor(step?.direction ?: ManeuverDirection.STRAIGHT),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = step?.instructionAr ?: route.destinationName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val metaLine = buildString {
                                append(formatRouteDurationAr(route.totalDurationSeconds))
                                append(" • ")
                                append(formatRouteDistanceAr(route.totalDistanceMeters))
                                append(" • ")
                                append("${stringResource(R.string.nav_to_prefix)} ${route.destinationName}")
                            }
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFA5F3FC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("nav_mute_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isVoiceMuted) Color.White.copy(alpha = 0.14f)
                                        else PulseTeal.copy(alpha = 0.25f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVoiceMuted) {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    },
                                    contentDescription = if (isVoiceMuted) {
                                        stringResource(R.string.unmute_voice)
                                    } else {
                                        stringResource(R.string.mute_voice)
                                    },
                                    tint = if (isVoiceMuted) Color(0xFF94A3B8) else Color(0xFF5EEAD4),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Step Progress & Quick Metadata Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.step_counter_fmt,
                                        safeIndex + 1,
                                        route.steps.size.coerceAtLeast(1)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (route.resolvedMode == TravelMode.SEA) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.route_sea_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFBAE6FD),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (route.resolvedMode == TravelMode.AIR) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF4F46E5).copy(alpha = 0.42f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.route_air_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE0E7FF),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (route.isDirectFallback) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PulseCoral.copy(alpha = 0.24f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.route_direct_note),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFCA5A5),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (step != null && step.distanceMeters >= 20.0) {
                                Text(
                                    text = formatRouteDistanceAr(step.distanceMeters),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            onClick = onAskAiAboutRoute,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.55f)),
                            modifier = Modifier.testTag("nav_ask_ai_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFC7D2FE),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.ai_ask_route_chip),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE0E7FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3-Mode Travel Switcher: Road / Sea Ship / Airport & Airplane
                    val activeResolved = if (selectedMode == TravelMode.AUTO) route.resolvedMode else selectedMode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TravelModeChip(
                            icon = Icons.Rounded.DirectionsCar,
                            label = stringResource(R.string.mode_road),
                            selected = activeResolved == TravelMode.ROAD,
                            accent = PulseTeal,
                            onClick = { onSelectTravelMode(TravelMode.ROAD) },
                            modifier = Modifier.weight(1f).testTag("mode_road_chip")
                        )
                        TravelModeChip(
                            icon = Icons.Rounded.DirectionsBoat,
                            label = stringResource(R.string.mode_ship),
                            selected = activeResolved == TravelMode.SEA,
                            accent = Color(0xFF0284C7),
                            onClick = { onSelectTravelMode(TravelMode.SEA) },
                            modifier = Modifier.weight(1f).testTag("mode_sea_chip")
                        )
                        TravelModeChip(
                            icon = Icons.Rounded.Flight,
                            label = stringResource(R.string.mode_plane),
                            selected = activeResolved == TravelMode.AIR,
                            accent = Color(0xFF6366F1),
                            onClick = { onSelectTravelMode(TravelMode.AIR) },
                            modifier = Modifier.weight(1f).testTag("mode_air_chip")
                        )
                    }

                    // Bottom Action Controls: Prev / Repeat Voice / Next / Stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPrevStep,
                            enabled = safeIndex > 0,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("nav_prev_step_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.prev_step),
                                tint = if (safeIndex > 0) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = onRepeatVoice,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeaking) PulseTeal else PulseOcean,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("nav_repeat_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.repeat_voice),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onNextStep,
                            enabled = safeIndex < route.steps.lastIndex,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White,
                                disabledContainerColor = Color.White.copy(alpha = 0.06f),
                                disabledContentColor = Color.White.copy(alpha = 0.35f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("nav_next_step_button")
                        ) {
                            Text(
                                text = stringResource(R.string.next_step),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = onStopNavigation,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseCoral,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("nav_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.stop_navigation),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelModeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent else Color.White.copy(alpha = 0.09f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.16f)
        ),
        modifier = modifier.heightIn(min = 42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun maneuverIconFor(direction: ManeuverDirection): ImageVector = when (direction) {
    ManeuverDirection.DEPART -> Icons.Rounded.Navigation
    ManeuverDirection.STRAIGHT -> Icons.Rounded.Straight
    ManeuverDirection.SLIGHT_RIGHT -> Icons.Rounded.TurnSlightRight
    ManeuverDirection.RIGHT, ManeuverDirection.SHARP_RIGHT -> Icons.Rounded.TurnRight
    ManeuverDirection.SLIGHT_LEFT -> Icons.Rounded.TurnSlightLeft
    ManeuverDirection.LEFT, ManeuverDirection.SHARP_LEFT -> Icons.Rounded.TurnLeft
    ManeuverDirection.U_TURN -> Icons.Rounded.UTurnLeft
    ManeuverDirection.ROUNDABOUT -> Icons.Rounded.RoundaboutRight
    ManeuverDirection.AIRPORT -> Icons.Rounded.LocalAirport
    ManeuverDirection.FLIGHT -> Icons.Rounded.Flight
    ManeuverDirection.PORT -> Icons.Rounded.Anchor
    ManeuverDirection.SHIP -> Icons.Rounded.DirectionsBoat
    ManeuverDirection.ARRIVE -> Icons.Rounded.Flag
}
