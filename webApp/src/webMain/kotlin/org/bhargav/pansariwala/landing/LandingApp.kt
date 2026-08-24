package org.bhargav.pansariwala.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.navigateToUserApp
import org.bhargav.pansariwala.theme.PansariTheme
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.app_name
import pansariwala.shared.generated.resources.brand_tagline_market
import pansariwala.shared.generated.resources.landing_about_body
import pansariwala.shared.generated.resources.landing_about_title
import pansariwala.shared.generated.resources.landing_cta_app_store
import pansariwala.shared.generated.resources.landing_cta_play
import pansariwala.shared.generated.resources.landing_delivery_about
import pansariwala.shared.generated.resources.landing_delivery_how
import pansariwala.shared.generated.resources.landing_delivery_title
import pansariwala.shared.generated.resources.landing_how_it_works
import pansariwala.shared.generated.resources.landing_login
import pansariwala.shared.generated.resources.landing_screenshots
import pansariwala.shared.generated.resources.landing_signup
import pansariwala.shared.generated.resources.landing_user_about
import pansariwala.shared.generated.resources.landing_user_how
import pansariwala.shared.generated.resources.landing_user_title
import pansariwala.shared.generated.resources.pansariwala_logo
import pansariwala.shared.generated.resources.shot_partner_duty
import pansariwala.shared.generated.resources.shot_partner_nav
import pansariwala.shared.generated.resources.shot_partner_offer
import pansariwala.shared.generated.resources.shot_partner_otp
import pansariwala.shared.generated.resources.shot_user_cart
import pansariwala.shared.generated.resources.shot_user_catalog
import pansariwala.shared.generated.resources.shot_user_home
import pansariwala.shared.generated.resources.shot_user_orders
import pansariwala.shared.generated.resources.shot_user_pay
import pansariwala.shared.generated.resources.shot_user_profile
import pansariwala.shared.generated.resources.shot_user_search
import pansariwala.shared.generated.resources.shot_user_track

@Composable
fun LandingApp() {
    PansariTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.pansariwala_logo),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.brand_tagline_market),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            AuthButtons()
            SectionCard(
                title = stringResource(Res.string.landing_about_title),
                body = stringResource(Res.string.landing_about_body),
            )
            AppProductSection(
                title = stringResource(Res.string.landing_user_title),
                about = stringResource(Res.string.landing_user_about),
                how = stringResource(Res.string.landing_user_how),
                shots = userShots(),
                playUrl = AppConstants.PLAY_STORE_USER_URL,
                appStoreUrl = AppConstants.APP_STORE_USER_URL,
            )
            AppProductSection(
                title = stringResource(Res.string.landing_delivery_title),
                about = stringResource(Res.string.landing_delivery_about),
                how = stringResource(Res.string.landing_delivery_how),
                shots = partnerShots(),
                playUrl = AppConstants.PLAY_STORE_DELIVERY_URL,
                appStoreUrl = AppConstants.APP_STORE_DELIVERY_URL,
            )
            Spacer(Modifier.height(24.dp))
            AuthButtons()
        }
    }
}

@Composable
private fun AuthButtons() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = ::navigateToUserApp) {
            Text(stringResource(Res.string.landing_login))
        }
        OutlinedButton(onClick = ::navigateToUserApp) {
            Text(stringResource(Res.string.landing_signup))
        }
    }
}

@Composable
private fun SectionCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AppProductSection(
    title: String,
    about: String,
    how: String,
    shots: List<String>,
    playUrl: String,
    appStoreUrl: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(about, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(Res.string.landing_how_it_works), style = MaterialTheme.typography.titleMedium)
            FlowSteps(how)
            Text(stringResource(Res.string.landing_screenshots), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                shots.forEach { label ->
                    PhoneMock(label = label)
                }
            }
            StoreButtons(playUrl = playUrl, appStoreUrl = appStoreUrl)
        }
    }
}

@Composable
private fun FlowSteps(how: String) {
    val steps = how.split("→").map { it.trim() }.filter { it.isNotEmpty() }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "${index + 1}. $step",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (index < steps.lastIndex) {
                Text("→", color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun PhoneMock(label: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(260.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.surfaceContainerHighest)
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surface)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.primaryContainer),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(scheme.surfaceVariant),
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.primary),
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StoreButtons(playUrl: String, appStoreUrl: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { openLandingUrl(playUrl) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.landing_cta_play))
        }
        OutlinedButton(onClick = { openLandingUrl(appStoreUrl) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.landing_cta_app_store))
        }
    }
}

@Composable
private fun userShots(): List<String> = listOf(
    stringResource(Res.string.shot_user_home),
    stringResource(Res.string.shot_user_search),
    stringResource(Res.string.shot_user_catalog),
    stringResource(Res.string.shot_user_cart),
    stringResource(Res.string.shot_user_pay),
    stringResource(Res.string.shot_user_track),
    stringResource(Res.string.shot_user_orders),
    stringResource(Res.string.shot_user_profile),
)

@Composable
private fun partnerShots(): List<String> = listOf(
    stringResource(Res.string.shot_partner_duty),
    stringResource(Res.string.shot_partner_offer),
    stringResource(Res.string.shot_partner_nav),
    stringResource(Res.string.shot_partner_otp),
)

fun openLandingUrl(url: String) {
    kotlinx.browser.window.open(url, "_blank")
}
