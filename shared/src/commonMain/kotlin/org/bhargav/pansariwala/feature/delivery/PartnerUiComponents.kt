package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.domain.model.DeliveryOffer
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.platform.PartnerLiveMap
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_accept
import pansariwala.shared.generated.resources.action_okay
import pansariwala.shared.generated.resources.action_close
import pansariwala.shared.generated.resources.pickup_photos_title
import pansariwala.shared.generated.resources.partner_enter_delivery_otp
import pansariwala.shared.generated.resources.partner_otp_customer_hint
import pansariwala.shared.generated.resources.offer_already_taken
import pansariwala.shared.generated.resources.offer_already_taken_title
import pansariwala.shared.generated.resources.partner_accepted
import pansariwala.shared.generated.resources.partner_accepted_empty_hint
import pansariwala.shared.generated.resources.partner_accepted_empty_title
import pansariwala.shared.generated.resources.partner_accepted_orders_title
import pansariwala.shared.generated.resources.partner_action_decline
import pansariwala.shared.generated.resources.partner_action_go_online
import pansariwala.shared.generated.resources.partner_available_orders_title
import pansariwala.shared.generated.resources.partner_brand_title
import pansariwala.shared.generated.resources.partner_current_location
import pansariwala.shared.generated.resources.partner_jobs_empty_hint
import pansariwala.shared.generated.resources.partner_jobs_empty_offline
import pansariwala.shared.generated.resources.partner_jobs_empty_title
import pansariwala.shared.generated.resources.partner_new_delivery_offer
import pansariwala.shared.generated.resources.partner_offer_distance
import pansariwala.shared.generated.resources.partner_offer_order
import pansariwala.shared.generated.resources.partner_offer_payout
import pansariwala.shared.generated.resources.partner_offer_timer
import pansariwala.shared.generated.resources.partner_resume_job
import pansariwala.shared.generated.resources.partner_navigate
import pansariwala.shared.generated.resources.partner_item_price
import pansariwala.shared.generated.resources.partner_bag_photo
import pansariwala.shared.generated.resources.partner_offline_banner
import pansariwala.shared.generated.resources.partner_online_banner
import pansariwala.shared.generated.resources.partner_pending_verification
import pansariwala.shared.generated.resources.partner_verified
import pansariwala.shared.generated.resources.action_retry
import pansariwala.shared.generated.resources.partner_action_back_home
import pansariwala.shared.generated.resources.partner_job_loading

@Composable
private fun PartnerPrimary() = MaterialTheme.colorScheme.primary


@Composable
fun PartnerBrandHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PartnerPrimary()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "P",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.partner_brand_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PartnerPrimary(),
        )
    }
}

private val PartnerTopBarSlot = 48.dp

/**
 * Prototype top bar: solid primary, white content, balanced leading | title | trailing slots.
 */
@Composable
fun PartnerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = PansariElevation.toolbar,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(PartnerTopBarSlot),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    leading != null -> leading()
                    onBack != null -> {
                        Text(
                            text = "←",
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onBack)
                                .padding(8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        )
                    }
                }
            }
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = PartnerTopBarSlot + 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(PartnerTopBarSlot),
                contentAlignment = Alignment.Center,
            ) {
                trailing?.invoke()
            }
        }
    }
}

/** Home bar: profile + status on the left, matching slot on the right for balance. */
@Composable
fun PartnerHomeTopBar(
    title: String,
    profilePhotoBase64: String?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = PansariElevation.toolbar,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center,
            ) {
                val photo = profilePhotoBase64.orEmpty()
                if (photo.isNotBlank()) {
                    Base64ImageThumbnail(
                        base64 = photo,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text("👤", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Trailing mirror slot keeps the bar balanced with job screens.
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
fun PartnerOnlineBanner(
    online: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (online) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (online) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!online) },
        color = bg,
    ) {
        Text(
            text = stringResource(
                if (online) Res.string.partner_online_banner else Res.string.partner_offline_banner,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
fun PartnerRouteMetaBar(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** @deprecated Use [PartnerTopBar] — kept as alias during migration. */
@Composable
fun PartnerGreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PartnerTopBar(title = title, onBack = onBack)
        if (!subtitle.isNullOrBlank()) {
            PartnerRouteMetaBar(text = subtitle)
        }
    }
}

@Composable
fun PartnerJobLoadingState(contentModifier: Modifier = Modifier) {
    Column(
        modifier = contentModifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.partner_job_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PartnerJobErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: (() -> Unit)? = null,
    contentModifier: Modifier = Modifier,
) {
    Column(
        modifier = contentModifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PartnerPrimaryButton(
            text = stringResource(Res.string.action_retry),
            onClick = onRetry,
        )
        if (onBack != null) {
            Spacer(Modifier.height(8.dp))
            PartnerDangerButton(
                text = stringResource(Res.string.partner_action_back_home),
                onClick = onBack,
            )
        }
    }
}

@Composable
fun PartnerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = PansariElevation.button),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PartnerDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = PansariElevation.button),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PartnerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PartnerOnlineToggle(
    online: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBackground: Boolean = false,
) {
    val onlineColor = if (online) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
    val labelColor = if (onBackground) Color.White
        else if (online) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = online,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = onlineColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD),
            ),
        )
        Text(
            text = stringResource(
                if (online) Res.string.partner_online_banner else Res.string.partner_offline_banner,
            ),
            fontWeight = FontWeight.Bold,
            color = labelColor,
        )
    }
}

@Composable
private fun PartnerCurrentLocationGlyph(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(20.dp)
                .border(2.dp, color, CircleShape),
        )
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
    }
}

@Composable
fun PartnerCurrentLocationButton(
    onClick: () -> Unit,
    fetching: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.partner_current_location)
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = label },
        containerColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = PansariElevation.fab),
    ) {
        if (fetching) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            PartnerCurrentLocationGlyph()
        }
    }
}

@Composable
fun PartnerMapPlaceholder(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    lat: Double = AppConstants.DEFAULT_MAP_LAT,
    lng: Double = AppConstants.DEFAULT_MAP_LNG,
    originLat: Double? = null,
    originLng: Double? = null,
    onCurrentLocationClick: (() -> Unit)? = null,
    fetchingLocation: Boolean = false,
    showCaption: Boolean = true,
) {
    val shape = if (fillHeight) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxSize() else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (fillHeight) 0.dp else PansariElevation.card,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PartnerLiveMap(
                lat = lat,
                lng = lng,
                modifier = Modifier.fillMaxSize(),
                originLat = originLat,
                originLng = originLng,
            )
            if (onCurrentLocationClick != null) {
                PartnerCurrentLocationButton(
                    onClick = onCurrentLocationClick,
                    fetching = fetchingLocation,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = if (showCaption) 72.dp else 16.dp),
                )
            }
            if (showCaption) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                    subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerLocationCard(
    title: String,
    distanceEta: String,
    address: String,
    modifier: Modifier = Modifier,
    onNavigate: (() -> Unit)? = null,
) {
    val navigateLabel = stringResource(Res.string.partner_navigate)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(distanceEta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = navigateLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .semantics { contentDescription = navigateLabel }
                        .then(
                            if (onNavigate != null) Modifier.clickable(onClick = onNavigate) else Modifier,
                        )
                        .padding(8.dp),
                )
            }
            Text(address, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PartnerProductRow(item: OrderItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.imageUrl.isNullOrBlank()) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                Text(item.productName.take(1), fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(item.productName, fontWeight = FontWeight.Medium)
            Text(
                stringResource(Res.string.partner_item_price, item.unitPrice.asMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("×${item.quantity.toInt()}", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PartnerDocumentRow(
    label: String,
    imageBase64: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val attached = imageBase64.isNotBlank()
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (attached) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (attached) {
                Base64ImageThumbnail(
                    base64 = imageBase64,
                    contentDescription = label,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", style = MaterialTheme.typography.titleLarge)
                }
            }
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                if (attached) "✓" else "›",
                color = if (attached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun PartnerPhotoSlot(
    label: String,
    attached: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    imageBase64: String = "",
) {
    val hasImage = imageBase64.isNotBlank()
    val filled = attached || hasImage
    val borderColor = when {
        filled -> MaterialTheme.colorScheme.primary
        dark -> Color.White.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val bg = when {
        filled && dark -> Color.White.copy(alpha = 0.12f)
        filled -> MaterialTheme.colorScheme.primaryContainer
        dark -> Color.White.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            Base64ImageThumbnail(
                base64 = imageBase64,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", style = MaterialTheme.typography.headlineMedium, color = fg)
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = fg)
            }
        }
    }
}

@Composable
fun PartnerVerificationBadge(verified: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (verified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = PansariElevation.chip,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (verified) "✓" else "⏳", color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(if (verified) Res.string.partner_verified else Res.string.partner_pending_verification),
                fontWeight = FontWeight.Medium,
                color = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun Base64ImageThumbnail(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val imageBitmap = remember(base64) {
        runCatching {
            @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
            val bytes = kotlin.io.encoding.Base64.decode(base64)
            bytes.decodeToImageBitmap()
        }.getOrNull()
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("📷")
        }
    }
}

@Composable
fun PickupPhotoStrip(
    photos: List<String>,
    modifier: Modifier = Modifier,
) {
    val visible = photos.filter { it.length > 64 }
    if (visible.isEmpty()) return
    var preview by remember { mutableStateOf<String?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.pickup_photos_title),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            visible.forEachIndexed { index, photo ->
                val label = stringResource(Res.string.partner_bag_photo, (index + 1).toString())
                Base64ImageThumbnail(
                    base64 = photo,
                    contentDescription = label,
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { preview = photo },
                )
            }
        }
    }
    preview?.let { photo ->
        FullscreenBase64Image(
            base64 = photo,
            contentDescription = stringResource(Res.string.pickup_photos_title),
            onDismiss = { preview = null },
        )
    }
}

@Composable
fun FullscreenBase64Image(
    base64: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 4f)
        offset = if (scale <= 1.01f) Offset.Zero else offset + pan
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Base64ImageThumbnail(
                base64 = base64,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(transformState),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Text(stringResource(Res.string.action_close), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeliveryOtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = AppConstants.DELIVERY_OTP_LENGTH,
) {
    val digits = value.filter { it.isDigit() }.take(length)
    val focusRequesters = remember { List(length) { FocusRequester() } }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.partner_enter_delivery_otp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.partner_otp_customer_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            repeat(length) { index ->
                val char = digits.getOrNull(index)?.toString().orEmpty()
                Box(
                    modifier = Modifier
                        .size(56.dp, 64.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicTextField(
                        value = char,
                        onValueChange = { incoming ->
                            val typed = incoming.filter { it.isDigit() }
                            when {
                                typed.length > 1 -> {
                                    val pasted = typed.take(length)
                                    onValueChange(pasted)
                                    focusRequesters[(pasted.length - 1).coerceAtMost(length - 1)].requestFocus()
                                }
                                typed.isEmpty() -> {
                                    onValueChange(digits.take(index))
                                    if (index > 0) focusRequesters[index - 1].requestFocus()
                                }
                                else -> {
                                    val prefix = digits.take(index)
                                    val suffix = digits.drop(index + 1)
                                    onValueChange((prefix + typed.last() + suffix).take(length))
                                    if (index < length - 1) focusRequesters[index + 1].requestFocus()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[index]),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (digits.isEmpty()) focusRequesters.firstOrNull()?.requestFocus()
    }
}

@Composable
fun PartnerProfileCircle(
    base64: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .then(
                if (base64.isBlank()) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (base64.isNotBlank()) {
            Base64ImageThumbnail(
                base64 = base64,
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text("👤", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun PartnerOfferAccent() = MaterialTheme.colorScheme.tertiary

@Composable
fun PartnerJobOfferCard(
    offer: DeliveryOffer,
    secondsLeft: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalKm = offer.totalDistanceKm.takeIf { it > 0 } ?: (offer.shopDistanceKm + offer.dropDistanceKm)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔔", style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(Res.string.partner_new_delivery_offer),
                    fontWeight = FontWeight.Bold,
                    color = PartnerOfferAccent(),
                )
            }
            Text(
                stringResource(Res.string.partner_offer_payout, offer.payoutInr.asMoney()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(Res.string.partner_offer_order, offer.orderId.removePrefix("ord_").takeLast(4)))
            Text(stringResource(Res.string.partner_offer_distance, ((totalKm * 10).toInt() / 10.0).toString()))
            if (secondsLeft > 0) {
                Text(
                    stringResource(Res.string.partner_offer_timer, secondsLeft.toString()),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PartnerPrimaryButton(text = stringResource(Res.string.action_accept), onClick = onAccept, modifier = Modifier.weight(1f))
                PartnerSecondaryButton(
                    text = stringResource(Res.string.partner_action_decline),
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    isDestructive = true,
                )
            }
        }
    }
}

@Composable
fun PartnerAcceptedJobCard(
    order: Order,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalKm = order.totalDistanceKm ?: 0.0
    val payout = order.partnerPayoutInr ?: 0.0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📦", style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(Res.string.partner_accepted),
                    fontWeight = FontWeight.Bold,
                    color = PartnerOfferAccent(),
                )
            }
            Text(
                stringResource(Res.string.partner_offer_payout, payout.asMoney()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(Res.string.partner_offer_order, order.id.removePrefix("ord_").takeLast(4)))
            if (totalKm > 0) {
                Text(stringResource(Res.string.partner_offer_distance, ((totalKm * 10).toInt() / 10.0).toString()))
            }
            PartnerPrimaryButton(
                text = stringResource(Res.string.partner_resume_job),
                onClick = onResume,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PartnerAcceptedOrdersSection(
    jobs: List<Order>,
    onResume: (Order) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (jobs.isEmpty()) {
        PartnerInformativeEmptyState(
            emojiOnline = "📦",
            emojiOffline = "📦",
            online = true,
            title = stringResource(Res.string.partner_accepted_empty_title),
            hintOnline = stringResource(Res.string.partner_accepted_empty_hint),
            hintOffline = stringResource(Res.string.partner_accepted_empty_hint),
            showGoOnline = false,
            onGoOnline = {},
            modifier = modifier,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.partner_accepted_orders_title, jobs.size.toString()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val pagerState = rememberPagerState(pageCount = { jobs.size })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val job = jobs[page]
                PartnerAcceptedJobCard(
                    order = job,
                    onResume = { onResume(job) },
                )
            }
        }
    }
}

@Composable
fun PartnerAvailableOrdersSection(
    offers: List<DeliveryOffer>,
    online: Boolean,
    secondsLeftFor: (DeliveryOffer) -> Int,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onGoOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (offers.isEmpty()) {
        PartnerJobsEmptyState(online = online, onGoOnline = onGoOnline, modifier = modifier)
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.partner_available_orders_title, offers.size.toString()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val pagerState = rememberPagerState(pageCount = { offers.size })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val offer = offers[page]
                PartnerJobOfferCard(
                    offer = offer,
                    secondsLeft = secondsLeftFor(offer),
                    onAccept = { onAccept(offer.id) },
                    onDecline = { onDecline(offer.id) },
                )
            }
        }
    }
}

@Composable
fun PartnerJobsEmptyState(
    online: Boolean,
    onGoOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PartnerInformativeEmptyState(
        emojiOnline = "🛵",
        emojiOffline = "💤",
        online = online,
        title = stringResource(Res.string.partner_jobs_empty_title),
        hintOnline = stringResource(Res.string.partner_jobs_empty_hint),
        hintOffline = stringResource(Res.string.partner_jobs_empty_offline),
        showGoOnline = !online,
        onGoOnline = onGoOnline,
        modifier = modifier,
    )
}

@Composable
private fun PartnerInformativeEmptyState(
    emojiOnline: String,
    emojiOffline: String,
    online: Boolean,
    title: String,
    hintOnline: String,
    hintOffline: String,
    showGoOnline: Boolean,
    onGoOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "jobs-empty-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jobs-empty-scale",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (online) emojiOnline else emojiOffline,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (online) hintOnline else hintOffline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (showGoOnline) {
            PartnerPrimaryButton(
                text = stringResource(Res.string.partner_action_go_online),
                onClick = onGoOnline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerOfferTakenSheet(
    onOkay: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pulse = rememberInfiniteTransition(label = "offer-taken-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offer-taken-scale",
    )
    ModalBottomSheet(
        onDismissRequest = onOkay,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛵💨", style = MaterialTheme.typography.displayMedium)
            }
            Text(
                stringResource(Res.string.offer_already_taken_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.offer_already_taken),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PartnerPrimaryButton(
                text = stringResource(Res.string.action_okay),
                onClick = onOkay,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
