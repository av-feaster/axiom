package com.axiom.android.sdk.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal val thinkingPhrases = listOf(
    "Thinking…",
    "Finding the best way to answer…",
    "Generating the best possible response…",
    "Weaving context together…",
    "Almost ready to reply…",
    "Polishing the answer…",
)

/**
 * Indeterminate horizontal sweep (Material-style infinite linear progress).
 */
@Composable
internal fun IndeterminateLinearBar(
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "indeterminateBar")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "barShift",
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(trackColor),
    ) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val segment = w * 0.38f
        val travel = w + segment
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.38f)
                .graphicsLayer {
                    translationX = -segment + shift * travel
                }
                .clip(RoundedCornerShape(2.dp))
                .background(barColor),
        )
    }
}

/**
 * Cycling “thinking” copy + pulse + indeterminate bar while waiting for the first streamed token.
 */
@Composable
internal fun ThinkingGenerationPanel(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!active) return

    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        phraseIndex = 0
        while (isActive) {
            delay(2_600)
            phraseIndex = (phraseIndex + 1) % thinkingPhrases.size
        }
    }

    val pulse = rememberInfiniteTransition(label = "thinkingPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Crossfade(
                targetState = phraseIndex,
                animationSpec = tween(420),
                label = "thinkingPhrase",
            ) { index ->
                Text(
                    text = thinkingPhrases[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = pulseAlpha),
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            IndeterminateLinearBar()
        }
    }
}
