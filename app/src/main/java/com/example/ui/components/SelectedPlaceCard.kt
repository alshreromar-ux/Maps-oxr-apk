package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Work
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LatLngPoint
import com.example.model.SelectedMapPlace
import com.example.model.calculateDistanceMeters
import com.example.model.formatDistanceArabic
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseOcean
import com.example.ui.theme.PulseTeal

private val FavoriteGold = Color(0xFFF59E0B)
private val HomeTeal = Color(0xFF0F9D8A)
private val WorkBlue = Color(0xFF3B82F6)

@Composable
fun SelectedPlaceCard(
    place: SelectedMapPlace,
    userLocation: LatLngPoint?,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onSetHome: () -> Unit,
    onSetWork: () -> Unit,
    onStartDirections: () -> Unit,
    onCreatePinHere: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val distMeters = userLocation?.let {
        calculateDistanceMeters(it.latitude, it.longitude, place.location.latitude, place.location.longitude)
    }

    val favBg by animateColorAsState(
        targetValue = if (isFavorited) FavoriteGold else FavoriteGold.copy(alpha = 0.14f),
        label = "favButtonBg"
    )
    val favContent by animateColorAsState(
        targetValue = if (isFavorited) Color.White else FavoriteGold,
        label = "favButtonContent"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("selected_map_place_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isFavorited) FavoriteGold.copy(alpha = 0.65f) else PulseOcean.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Info Row + Favorite Star Toggle + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PulseOcean.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (place.isResolvingDetails) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.2.dp,
                                color = PulseOcean
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = null,
                                tint = PulseOcean,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PulseTeal.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = place.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTeal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (distMeters != null) {
                                Text(
                                    text = formatDistanceArabic(context, distMeters),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseOcean,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = place.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (place.subtitle.isNotBlank()) {
                            Text(
                                text = place.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Prominent 1-Tap Favorite Star Action
                    Surface(
                        onClick = onToggleFavorite,
                        shape = RoundedCornerShape(14.dp),
                        color = favBg,
                        border = if (isFavorited) null else BorderStroke(1.dp, FavoriteGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("selected_place_favorite_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = stringResource(R.string.favorite_toggle_cd),
                                tint = favContent,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isFavorited) R.string.remove_from_favorites else R.string.add_to_favorites
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = favContent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
            }

            // Action Strip: Directions, Set as Home, Set as Work, Add Community Pin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartDirections,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseOcean,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("selected_place_directions_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.directions_short),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSetHome,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HomeTeal.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("selected_place_set_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = HomeTeal,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_place_label),
                        color = HomeTeal,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSetWork,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WorkBlue.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("selected_place_set_work_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Work,
                        contentDescription = null,
                        tint = WorkBlue,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.work_place_label),
                        color = WorkBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCreatePinHere,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PulseCoral.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("selected_place_add_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddLocationAlt,
                        contentDescription = null,
                        tint = PulseCoral,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_pin_here_short),
                        color = PulseCoral,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
