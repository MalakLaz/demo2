package com.mallar.app.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.google.accompanist.permissions.*
import com.mallar.app.data.model.*
import com.mallar.app.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARNavigationScreen(
    store: Store?,
    navigationSteps: List<ARNavigationStep>,
    currentStepIndex: Int,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onBack: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val currentStep = navigationSteps.getOrNull(currentStepIndex)
    val isFloorChange = currentStep?.isFloorChange == true

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera background
        if (cameraPermission.status.isGranted) {
            CameraPreview(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A2E45), Color(0xFF0D1E28))
                        )
                    )
            )
        }

        // AR Arrow overlay on "floor"
        currentStep?.let { step ->
            ARArrowOverlay(
                direction = step.direction,
                isFloorChange = step.isFloorChange,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top bar: store info card
        store?.let { s ->
            StoreInfoTopCard(
                store = s,
                currentStep = currentStep,
                onClose = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // Navigation instruction: bottom card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            currentStep?.let { step ->
                // Instruction card (floor change style)
                if (isFloorChange) {
                    FloorChangeCard(step = step, onClick = onNextStep)
                } else {
                    DirectionInstructionCard(
                        step = step,
                        stepIndex = currentStepIndex,
                        totalSteps = navigationSteps.size,
                        onNext = onNextStep,
                        onPrevious = onPreviousStep
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show Road button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show Road",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ARArrowOverlay(
    direction: NavDirection,
    isFloorChange: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")

    // Arrow pulse/move animation
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -24f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "arrow_move"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900, delayMillis = 200), RepeatMode.Reverse),
        label = "a2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900, delayMillis = 400), RepeatMode.Reverse),
        label = "a3"
    )

    val rotation = when (direction) {
        NavDirection.LEFT -> -45f
        NavDirection.RIGHT -> 45f
        NavDirection.ESCALATOR_UP, NavDirection.ELEVATOR -> 0f
        else -> 0f
    }

    if (direction == NavDirection.ARRIVAL) return

    Column(
        modifier = modifier.offset(y = (arrowOffset / 2).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-16).dp)
    ) {
        // Three cascading arrows
        ARArrow(alpha = alpha3, scale = 0.5f, rotation = rotation)
        ARArrow(alpha = alpha2, scale = 0.75f, rotation = rotation)
        ARArrow(alpha = alpha1, scale = 1f, rotation = rotation)
    }
}

@Composable
fun ARArrow(alpha: Float, scale: Float, rotation: Float) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )

    Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = null,
        tint = ARArrowGreen,
        modifier = Modifier
            .size((80 * scale).dp)
            .scale(glow * scale)
            .alpha(alpha)
    )
}

@Composable
fun StoreInfoTopCard(
    store: Store,
    currentStep: ARNavigationStep?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store logo
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    store.name.take(2),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TealPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            tint = TealPrimary, modifier = Modifier.size(14.dp))
                        currentStep?.let { step ->
                            if (step.distance > 0) {
                                Text("${step.distance}m", fontSize = 12.sp,
                                    color = TealPrimary, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Arrived!", fontSize = 12.sp,
                                    color = TealPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null,
                            tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Text("~2min", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close",
                    tint = TextSecondary)
            }
        }
    }
}

@Composable
fun DirectionInstructionCard(
    step: ARNavigationStep,
    stepIndex: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Step ${stepIndex + 1} of $totalSteps",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                // Step dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(totalSteps) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == stepIndex) 20.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i == stepIndex) TealPrimary else Color(0xFFE0E0E0))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Direction icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TealSurface),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (step.direction) {
                        NavDirection.LEFT -> Icons.Default.TurnLeft
                        NavDirection.RIGHT -> Icons.Default.TurnRight
                        NavDirection.STRAIGHT -> Icons.Default.ArrowUpward
                        NavDirection.ESCALATOR_UP -> Icons.Default.Elevator
                        NavDirection.ESCALATOR_DOWN -> Icons.Default.Elevator
                        NavDirection.ELEVATOR -> Icons.Default.Elevator
                        NavDirection.ARRIVAL -> Icons.Default.Flag
                    }
                    Icon(icon, contentDescription = null, tint = TealPrimary,
                        modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        step.instruction,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (step.distance > 0) {
                        Text(
                            "${step.distance}m ahead",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Next step button
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Next Step", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FloorChangeCard(
    step: ARNavigationStep,
    onClick: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth().scale(pulse),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Elevator, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step.instruction, color = Color.White,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Tap to proceed", color = Color.White.copy(0.75f), fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = Color.White.copy(0.8f), modifier = Modifier.size(24.dp))
        }
    }
}
