package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.domain.model.PartnerEarnings
import org.bhargav.pansariwala.domain.model.PartnerProfile
import org.bhargav.pansariwala.feature.reports.ReportRange
import org.bhargav.pansariwala.feature.reports.short
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.media.isHttpImageUrl
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.partner_earnings
import pansariwala.shared.generated.resources.partner_earnings_acceptance
import pansariwala.shared.generated.resources.partner_earnings_deliveries
import pansariwala.shared.generated.resources.partner_earnings_filter_cd
import pansariwala.shared.generated.resources.partner_earnings_lifetime
import pansariwala.shared.generated.resources.partner_onboarding
import pansariwala.shared.generated.resources.partner_period_custom
import pansariwala.shared.generated.resources.partner_period_six_months
import pansariwala.shared.generated.resources.partner_period_three_months
import pansariwala.shared.generated.resources.partner_verified
import pansariwala.shared.generated.resources.report_period_month
import pansariwala.shared.generated.resources.report_period_today
import pansariwala.shared.generated.resources.report_period_week
import pansariwala.shared.generated.resources.report_period_year
import pansariwala.shared.generated.resources.report_period_yesterday

/** Home header: profile row plus earnings for the selected period, with a period filter in the corner. */
@Composable
fun PartnerEarningsProfileCard(
    profile: PartnerProfile?,
    earnings: PartnerEarnings?,
    period: PartnerEarningsPeriod,
    range: ReportRange?,
    loading: Boolean,
    onSelectPeriod: (PartnerEarningsPeriod) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val onCard = colors.onPrimary
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(end = 40.dp).clickable(onClick = onProfileClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar(profile)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onCard,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        profile?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (it.verified) {
                                    Icon(Icons.Default.Verified, null, tint = onCard, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    listOf(
                                        stringResource(if (it.verified) Res.string.partner_verified else Res.string.partner_onboarding),
                                        it.vehicleReg,
                                    ).filter(String::isNotBlank).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onCard.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = onCard.copy(alpha = 0.25f))
                Text(
                    "${stringResource(Res.string.partner_earnings)} · ${periodLabel(period, range)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = onCard.copy(alpha = 0.85f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (earnings?.rangeEarnings ?: 0.0).asMoney(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = onCard,
                    )
                    if (loading) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(Modifier.size(18.dp), color = onCard, strokeWidth = 2.dp)
                    }
                }
                earnings?.let {
                    Spacer(Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatText(stringResource(Res.string.partner_earnings_deliveries, it.rangeDeliveredCount))
                        StatText(stringResource(Res.string.partner_earnings_acceptance, it.acceptanceRatePercent))
                        StatText(stringResource(Res.string.partner_earnings_lifetime, it.totalEarnings.asMoney()))
                    }
                }
            }
            PeriodFilterButton(
                selected = period,
                onSelect = onSelectPeriod,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
        }
    }
}

@Composable
private fun ProfileAvatar(profile: PartnerProfile?) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        val photo = profile?.profilePhoto.orEmpty()
        if (isHttpImageUrl(photo)) {
            NetworkImage(url = photo, contentDescription = profile?.name, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Icon(Icons.Default.Person, contentDescription = profile?.name, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun StatText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
    )
}

@Composable
private fun PeriodFilterButton(
    selected: PartnerEarningsPeriod,
    onSelect: (PartnerEarningsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = stringResource(Res.string.partner_earnings_filter_cd),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PartnerEarningsPeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(periodName(period)) },
                    onClick = {
                        expanded = false
                        onSelect(period)
                    },
                    trailingIcon = if (period == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun periodLabel(period: PartnerEarningsPeriod, range: ReportRange?): String {
    if (period != PartnerEarningsPeriod.CUSTOM || range == null) return periodName(period)
    val from = range.start.short()
    val to = range.endInclusive.short()
    return if (from == to) from else "$from – $to"
}

@Composable
private fun periodName(period: PartnerEarningsPeriod): String = stringResource(
    when (period) {
        PartnerEarningsPeriod.TODAY -> Res.string.report_period_today
        PartnerEarningsPeriod.YESTERDAY -> Res.string.report_period_yesterday
        PartnerEarningsPeriod.WEEK -> Res.string.report_period_week
        PartnerEarningsPeriod.MONTH -> Res.string.report_period_month
        PartnerEarningsPeriod.THREE_MONTHS -> Res.string.partner_period_three_months
        PartnerEarningsPeriod.SIX_MONTHS -> Res.string.partner_period_six_months
        PartnerEarningsPeriod.YEAR -> Res.string.report_period_year
        PartnerEarningsPeriod.CUSTOM -> Res.string.partner_period_custom
    },
)
