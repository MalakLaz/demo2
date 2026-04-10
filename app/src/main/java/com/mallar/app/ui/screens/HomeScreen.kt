package com.mallar.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.mallar.app.data.model.Store
import com.mallar.app.ui.theme.*

@Composable
fun HomeScreen(
    stores: List<Store>,
    onOpenCamera: () -> Unit,
    onSearchDestination: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")

    // Animated floating store bubbles
    val bubble1Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bubble2Y by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bubble3Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(800), label = "content"
    )

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFA))
    ) {
        // Top teal header wave
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TealPrimary, TealLight)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Text(
                text = "Find the place you\nare looking for",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Floating store bubbles section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Decorative empty circles
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .offset(x = (-120).dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .then(
                            Modifier.clip(CircleShape)
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color(0x30E91E8C))
                                )
                            )
                    )
                }

                // H&M bubble
                StoreBubble(
                    storeName = "H&M",
                    textColor = Color(0xFFE50010),
                    bgColor = Color.White,
                    size = 100,
                    modifier = Modifier.offset(x = (-70).dp, y = bubble1Y.dp - 30.dp)
                )

                // ZARA bubble
                StoreBubble(
                    storeName = "ZARA",
                    textColor = Color.Black,
                    bgColor = Color(0xFFF0F0F0),
                    size = 90,
                    modifier = Modifier.offset(x = 80.dp, y = bubble2Y.dp + 10.dp)
                )

                // PIXI bubble
                StoreBubble(
                    storeName = "PIXI",
                    textColor = Color(0xFF1B5E20),
                    bgColor = Color.White,
                    size = 80,
                    borderColor = Color(0xFF1B5E20),
                    modifier = Modifier.offset(x = (-20).dp, y = bubble3Y.dp + 70.dp)
                )

                // Decorative small circles
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 110.dp, y = (-70).dp)
                        .clip(CircleShape)
                        .background(Color(0x25FFC107))
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = (-100).dp, y = 80.dp)
                        .clip(CircleShape)
                        .background(Color(0x30E91E8C))
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .offset(x = 30.dp, y = (-90).dp)
                        .clip(CircleShape)
                        .background(Color(0x358B1A4A))
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = (-50).dp, y = (-80).dp)
                        .clip(CircleShape)
                        .background(Color(0x302196F3))
                )
            }

            // Bottom section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sign in text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Already have an account? ",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sign In",
                        color = TealPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camera / AR Scan button (primary)
                Button(
                    onClick = onOpenCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pulse)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan Surroundings with AR",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search store button (secondary)
                OutlinedButton(
                    onClick = onSearchDestination,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TealPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TealPrimary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search for a Store",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StoreBubble(
    storeName: String,
    textColor: Color,
    bgColor: Color,
    size: Int,
    modifier: Modifier = Modifier,
    borderColor: Color? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { pressed = !pressed }
            .then(
                if (borderColor != null) Modifier.clip(CircleShape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (borderColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = storeName,
                    color = textColor,
                    fontSize = (size / 5).sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = storeName,
                color = textColor,
                fontSize = (size / 5).sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
