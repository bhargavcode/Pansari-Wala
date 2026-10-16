package org.bhargav.pansariwala.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One selling point shown as a chip under the onboarding hero. */
data class OnboardingHighlight(val emoji: String, val text: String)

private val FloatSlots = listOf(
    0.08f to 0.18f,
    0.82f to 0.12f,
    0.14f to 0.72f,
    0.86f to 0.62f,
    0.50f to 0.06f,
    0.70f to 0.86f,
)

/**
 * Gradient hero for phone/OTP onboarding: animated floating icons around a pulsing badge,
 * title + tagline, and highlight chips. Pair with [OnboardingFormCard] for the inputs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingHero(
    title: String,
    tagline: String,
    heroEmoji: String,
    floatingEmojis: List<String>,
    highlights: List<OnboardingHighlight>,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "onboardingHero")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val ringColor = Color.White
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.linearGradient(listOf(startColor, endColor)))
            .drawBehind {
                drawCircle(ringColor.copy(alpha = 0.08f), radius = size.width * 0.55f, center = Offset(size.width, 0f))
                drawCircle(ringColor.copy(alpha = 0.06f), radius = size.width * 0.35f, center = Offset(0f, size.height))
            }
            .statusBarsPadding()
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(200.dp)) {
            floatingEmojis.take(FloatSlots.size).forEachIndexed { index, emoji ->
                val (fx, fy) = FloatSlots[index]
                val dir = if (index % 2 == 0) 1f else -1f
                Text(
                    text = emoji,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .offset(x = maxWidth * fx - 16.dp, y = maxHeight * fy + (drift * 8f * dir).dp)
                        .rotate(drift * 10f * dir)
                        .alpha(0.9f),
                )
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(132.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(heroEmoji, fontSize = 48.sp)
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = tagline,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        if (highlights.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                highlights.forEach { item ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.emoji, fontSize = 14.sp)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            item.text,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Elevated card that overlaps the bottom of [OnboardingHero] and hosts the phone/OTP inputs. */
@Composable
fun OnboardingFormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = (-32).dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
