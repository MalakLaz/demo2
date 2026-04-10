package com.mallar.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mallar.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ArrivalScreen(
    storeName: String,
    onNavigateHome: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    val contentScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(600),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        delay(400)
        showConfetti = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        TealPrimary.copy(0.15f),
                        Color(0xFFF5FAFA),
                        Color.White
                    ),
                    radius = 800f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Confetti particles
        if (showConfetti) {
            ConfettiOverlay()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(contentScale)
                .alpha(contentAlpha)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Success ring
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsing ring
                val pulse by rememberInfiniteTransition(label = "ring").animateFloat(
                    initialValue = 1f, targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                    label = "ring_pulse"
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(0.15f))
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(TealLight, TealPrimary, TealDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "🎉",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You have reached your destination!",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = storeName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enjoy your shopping! 🛍",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Star rating row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                repeat(5) { i ->
                    val starAlpha by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0f,
                        animationSpec = tween(300, delayMillis = 600 + i * 100),
                        label = "star_$i"
                    )
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(32.dp).alpha(starAlpha)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigate home button
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(29.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TealPrimary),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Another Store", fontSize = 15.sp)
            }
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val delay: Int,
    val rotation: Float
)

@Composable
fun ConfettiOverlay() {
    val confettiColors = listOf(
        TealPrimary, Color(0xFFE91E8C), Color(0xFFFFC107),
        Color(0xFF00BCD4), Color(0xFFE91E63), Color(0xFF4CAF50),
        Color(0xFFFF9800), Color(0xFF9C27B0)
    )

    val particles = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                color = confettiColors.random(),
                size = Random.nextFloat() * 10f + 6f,
                speed = Random.nextFloat() * 0.5f + 0.3f,
                delay = Random.nextInt(1500),
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEachIndexed { i, particle ->
            val animY by infiniteTransition.animateFloat(
                initialValue = -0.1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (3000 / particle.speed).toInt(),
                        delayMillis = particle.delay,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "conf_$i"
            )
            val rot by infiniteTransition.animateFloat(
                initialValue = particle.rotation,
                targetValue = particle.rotation + 360f,
                animationSpec = infiniteRepeatable(
                    tween(2000 + i * 100, easing = LinearEasing),
                    RepeatMode.Restart
                ),
                label = "rot_$i"
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val xPx = particle.x * maxWidth.value
                val yPx = animY * maxHeight.value

                Box(
                    modifier = Modifier
                        .offset(x = xPx.dp, y = yPx.dp)
                        .size(particle.size.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(particle.color.copy(alpha = 0.8f))
                )
            }
        }
    }
}
