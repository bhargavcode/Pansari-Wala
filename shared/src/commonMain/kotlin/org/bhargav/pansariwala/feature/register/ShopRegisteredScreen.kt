package org.bhargav.pansariwala.feature.register

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.reg_congrats_login
import pansariwala.shared.generated.resources.reg_congrats_message
import pansariwala.shared.generated.resources.reg_congrats_title
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Celebrates a new shop, then hands over to login after [AppConstants.ShopRegistration.CONGRATS_DURATION_MS]. */
@Composable
fun ShopRegisteredScreen(
    shopName: String,
    loginUsername: String,
    onFinished: () -> Unit,
) {
    val badgeScale = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val countdown = remember { Animatable(0f) }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onFinished,
    )
    LaunchedEffect(Unit) {
        launch { badgeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        launch { burst.animateTo(1f, tween(BURST_MS, easing = LinearEasing)) }
        countdown.animateTo(
            1f,
            tween(AppConstants.ShopRegistration.CONGRATS_DURATION_MS.toInt(), easing = LinearEasing),
        )
        onFinished()
    }
    val primary = MaterialTheme.colorScheme.primary
    val confettiColors = listOf(primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary, CONFETTI_GOLD)
    val pieces = remember { List(CONFETTI_COUNT) { ConfettiPiece.random() } }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height * CONFETTI_CENTER_Y)
            val reach = size.minDimension * CONFETTI_REACH
            val progress = burst.value
            pieces.forEachIndexed { index, piece ->
                val distance = reach * piece.distance * progress
                val fall = size.height * CONFETTI_FALL * progress * progress
                val position = center + Offset(
                    (cos(piece.angle) * distance).toFloat(),
                    (sin(piece.angle) * distance).toFloat() + fall,
                )
                drawCircle(
                    color = confettiColors[index % confettiColors.size].copy(alpha = 1f - progress * CONFETTI_FADE),
                    radius = piece.radius.dp.toPx(),
                    center = position,
                )
            }
        }
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(120.dp)
                    .scale(badgeScale.value)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(72.dp))
            }
            Text(
                stringResource(Res.string.reg_congrats_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.reg_congrats_message, shopName),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.reg_congrats_login, loginUsername),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                progress = { countdown.value },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            )
        }
    }
}

private class ConfettiPiece(val angle: Double, val distance: Float, val radius: Float) {
    companion object {
        fun random() = ConfettiPiece(
            angle = Random.nextDouble(0.0, 2 * PI),
            distance = Random.nextFloat() * 0.7f + 0.3f,
            radius = Random.nextFloat() * 4f + 3f,
        )
    }
}

private const val CONFETTI_COUNT = 48
private const val BURST_MS = 1_600
private const val CONFETTI_CENTER_Y = 0.35f
private const val CONFETTI_REACH = 0.55f
private const val CONFETTI_FALL = 0.25f
private const val CONFETTI_FADE = 0.6f
private val CONFETTI_GOLD = Color(0xFFFFC107)
