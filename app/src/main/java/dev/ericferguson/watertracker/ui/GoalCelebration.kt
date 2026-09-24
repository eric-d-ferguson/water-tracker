package dev.ericferguson.watertracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val DROP_COUNT = 16
private const val SPLASH_MILLIS = 1100
private const val PEAK_PULSE = 1.12f
// Lets the progress ring finish filling before the celebration starts.
private const val START_DELAY_MILLIS = 300L

/**
 * Drives the goal-reached celebration: a bounce of the progress ring and a splash of droplets.
 *
 * It's triggered as an event by [play] rather than by state, so it doesn't replay when the
 * ring scrolls back into view or the app is reopened with the goal already met.
 */
@Stable
class GoalCelebrationState {
    /** Scale for the ring: 1 at rest. */
    val pulse = Animatable(1f)

    /** 0→1 while the splash plays; 1 when idle (nothing drawn). */
    val splash = Animatable(1f)

    /** Changes on every play, so each splash gets fresh random droplets. */
    var playCount by mutableIntStateOf(0)
        private set

    suspend fun play() = coroutineScope {
        delay(START_DELAY_MILLIS)
        playCount++
        launch {
            pulse.animateTo(PEAK_PULSE, tween(durationMillis = 140, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        launch {
            splash.snapTo(0f)
            splash.animateTo(1f, tween(durationMillis = SPLASH_MILLIS, easing = LinearOutSlowInEasing))
        }
    }
}

@Composable
fun rememberGoalCelebrationState(): GoalCelebrationState = remember { GoalCelebrationState() }

private class Droplet(val angleDegrees: Float, val distance: Float, val scale: Float, val light: Boolean)

/**
 * Droplets bursting outward from just outside a ring of [ringStroke] width drawn at this
 * composable's size. Place it on top of the ring; it draws beyond its bounds, which Compose allows.
 */
@Composable
fun GoalSplash(state: GoalCelebrationState, ringStroke: Dp, modifier: Modifier = Modifier) {
    val droplets = remember(state.playCount) {
        List(DROP_COUNT) { i ->
            Droplet(
                // Evenly spaced with a little jitter so it doesn't look mechanical.
                angleDegrees = i * 360f / DROP_COUNT + Random.nextFloat() * 14f - 7f,
                distance = 0.6f + Random.nextFloat() * 0.4f,
                scale = 0.7f + Random.nextFloat() * 0.6f,
                light = i % 2 == 0,
            )
        }
    }
    val darkColor = MaterialTheme.colorScheme.primary
    val lightColor = Color(0xFF4FC3F7)

    Canvas(modifier) {
        // Reading the value here, in the draw phase, redraws each frame without recomposing.
        val t = state.splash.value
        if (t >= 1f) return@Canvas

        // Start past the ring's outer edge, allowing for the bounce, so no droplet is hidden.
        val startRadius = (size.minDimension / 2 + ringStroke.toPx() / 2) * PEAK_PULSE
        val travel = 48.dp.toPx()
        // Fully visible for the first half, then fade out.
        val alpha = (2f - 2f * t).coerceAtMost(1f)
        droplets.forEach { droplet ->
            val radians = Math.toRadians(droplet.angleDegrees.toDouble())
            val distance = startRadius + travel * droplet.distance * t
            translate(
                left = center.x + (cos(radians) * distance).toFloat(),
                top = center.y + (sin(radians) * distance).toFloat(),
            ) {
                // Rotate so the tip trails back toward the ring, like a flying drop.
                rotate(degrees = droplet.angleDegrees - 90f, pivot = Offset.Zero) {
                    drawTeardrop(
                        radius = 7.dp.toPx() * droplet.scale * (1f - 0.4f * t),
                        color = if (droplet.light) lightColor else darkColor,
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

/** A drop centered on the origin with its tip pointing up. */
private fun DrawScope.drawTeardrop(radius: Float, color: Color, alpha: Float) {
    drawCircle(color = color, radius = radius, center = Offset.Zero, alpha = alpha)
    // With the tip at twice the radius, the lines to (±0.866r, -0.5r) are tangent to the circle.
    val tip = Path().apply {
        moveTo(0f, -2f * radius)
        lineTo(0.866f * radius, -0.5f * radius)
        lineTo(-0.866f * radius, -0.5f * radius)
        close()
    }
    drawPath(tip, color = color, alpha = alpha)
}
