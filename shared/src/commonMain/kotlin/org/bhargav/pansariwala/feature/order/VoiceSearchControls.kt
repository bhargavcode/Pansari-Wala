package org.bhargav.pansariwala.feature.order

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun VoiceSearchControls(
    isListening: Boolean,
    partialTranscript: String,
    onMicClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    showMicButton: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (showMicButton) {
                ListeningMicButton(
                    isListening = isListening,
                    onClick = onMicClick,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (showMicButton) 12.dp else 0.dp),
            ) {
                Text(
                    text = if (isListening) "Sun raha hoon…" else "Voice se order add karein",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (isListening && partialTranscript.isNotBlank()) {
                    Text(
                        text = partialTranscript,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (isListening) {
                TextButton(onClick = onCancel) {
                    Text("Cancel listening")
                }
            }
        }
    }
}

@Composable
fun ListeningMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "mic")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    )

    val active = MaterialTheme.colorScheme.error
    val idle = MaterialTheme.colorScheme.primary
    val iconColor = if (isListening) Color.White else idle
    val bg = if (isListening) active else MaterialTheme.colorScheme.primaryContainer

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(56.dp),
    ) {
        if (isListening) {
            Canvas(modifier = Modifier.size(56.dp)) {
                drawCircle(
                    color = active.copy(alpha = ringAlpha),
                    radius = size.minDimension / 2f * pulse,
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable(onClick = onClick),
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                // Mic capsule
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(w * 0.32f, h * 0.08f),
                    size = Size(w * 0.36f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                )
                // Arc stand
                drawArc(
                    color = iconColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.28f),
                    size = Size(w * 0.64f, h * 0.48f),
                    style = Stroke(width = w * 0.08f, cap = StrokeCap.Round),
                )
                // Stem
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.5f, h * 0.72f),
                    end = Offset(w * 0.5f, h * 0.88f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.32f, h * 0.88f),
                    end = Offset(w * 0.68f, h * 0.88f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
